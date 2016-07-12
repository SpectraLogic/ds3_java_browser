package com.spectralogic.dsbrowser.gui.components.localfiletreetable;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.GetObjectRequest;
import com.spectralogic.ds3client.commands.spectrads3.GetBulkJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetBulkJobSpectraS3Response;
import com.spectralogic.ds3client.models.BulkObject;
import com.spectralogic.ds3client.models.MasterObjectList;
import com.spectralogic.ds3client.models.Objects;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class Ds3GetJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);

    private final List<Ds3TreeTableValue> list;
    private final FileTreeModel fileTreeModel;
    private final Ds3Client ds3Client;
    private String bucket;
    private ArrayList<Ds3TreeTableValue> nodes;
    final ImmutableSet.Builder<String> partOfDirBuilder;


    public Ds3GetJob(List<Ds3TreeTableValue> list, FileTreeModel fileTreeModel, Ds3Client client) {
        this.list = list;
        this.fileTreeModel = fileTreeModel;
        this.ds3Client = client;
        this.bucket = null;
        partOfDirBuilder = ImmutableSet.builder();
        nodes = new ArrayList<>();
    }

    @Override
    public void executeJob() throws Exception {
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            String date = sdf.format(new Date());
            updateTitle("GET "+ds3Client.getConnectionDetails().getEndpoint()+" "+date);
            updateMessage("Transferring..");

            Path localDirPath = fileTreeModel.getPath();
            if (fileTreeModel.getType().equals(FileTreeModel.Type.File)) {
                localDirPath = localDirPath.getParent();
            }

            final ImmutableList<Ds3TreeTableValue> directories = list.stream().filter(value -> (value.getType().equals(Ds3TreeTableValue.Type.Directory))).collect(GuavaCollectors.immutableList());
            final ImmutableList<Ds3TreeTableValue> files = list.stream().filter(value -> value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());

            files.stream().forEach(value -> nodes.add(value));
            directories.stream().forEach(value -> {
                addAllDescendents(value, nodes);
                LOG.info("" + partOfDirBuilder.build().size());
            });

            final ImmutableList<Ds3Object> objects = nodes.stream().map(pair -> {
                try {
                    if (bucket == null) {
                        bucket = pair.getBucketName();
                    }
                    if (partOfDirBuilder.build().size() == 0)
//                        if(pair.getDirectoryName()!=null)
//                            return new Ds3Object(pair.getFullName(), Long.parseLong(pair.getSize().replaceAll("\\D+", "")));
//                        else
                        return new Ds3Object(pair.getFullName(), Long.parseLong(pair.getSize().replaceAll("\\D+", "")));
                    else {
                        Path dirPath = fileTreeModel.getPath();
                        if (fileTreeModel.getType().equals(FileTreeModel.Type.File)) {
                            dirPath = dirPath.getParent();
                        }
                        File f = new File(dirPath.toString() + "\\" + pair.getFullName());
                        f.getParentFile().mkdirs();
                        return new Ds3Object(pair.getFullName(), Long.parseLong(pair.getSize().replaceAll("\\D+", "")));
                    }
                } catch (final Exception e) {
                    LOG.error("Failed to get file size for: " + pair.getName(), e);
                    return null;
                }
            }).filter(item -> item != null).collect(GuavaCollectors.immutableList());

            LOG.info("" + objects.size());
//
            // Prime DS3 with the BulkGet command so that it can start to get objects off of tape.
            final GetBulkJobSpectraS3Response bulkResponse = ds3Client
                    .getBulkJobSpectraS3(new GetBulkJobSpectraS3Request(bucket, objects));
//
            // The bulk response returns a list of lists which is designed to optimize data transmission from DS3.
            final MasterObjectList list = bulkResponse.getResult();
            for (final Objects objects1 : list.getObjects()) {
                for (final BulkObject obj : objects1.getObjects()) {
                    File file = new File(obj.getName());
                    String path = file.getPath();
                    if (partOfDirBuilder.build().size() == 0) {
                        path = file.getName();
                    }
                    final FileChannel channel = FileChannel.open(
                            localDirPath.resolve(localDirPath + "//" + path),
                            StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE
                    );
                    // To handle the case where a file has been chunked we need to seek to the correct offset
                    // before we make the GetObject call so that when the request writes to the channel it is
                    // writing at the correct offset in the file.
                    channel.position(obj.getOffset());

                    // Perform the operation to get the object from DS3.
                    ds3Client.getObject(new GetObjectRequest(
                            bucket,
                            obj.getName(),
                            channel,
                            list.getJobId(),
                            obj.getOffset()
                    ));
                }
            }



        } catch (final Exception e) {
            //Could be permission issue. Need to handle
            System.out.println("Exception" + e.toString());
            LOG.info("Execption" + e.toString());
        }
    }

    private void addAllDescendents(Ds3TreeTableValue value, ArrayList nodes) {

        try {
            partOfDirBuilder.add(value.getFullName());
            final GetBucketRequest request = new GetBucketRequest(value.getBucketName()).withDelimiter("/");
            // Don't include the prefix if the item we are looking up from is the base bucket
            if (value.getType() != Ds3TreeTableValue.Type.Bucket) {
                request.withPrefix(value.getFullName());
            }
            final GetBucketResponse bucketResponse = ds3Client.getBucket(request);
            final ImmutableList<Ds3TreeTableValue> files = bucketResponse.getListBucketResult()
                    .getObjects().stream()
                    .map(f -> new Ds3TreeTableValue(value.getBucketName(), f.getKey(), Ds3TreeTableValue.Type.File, FileSizeFormat.getFileSizeType(f.getSize()), "")).collect(GuavaCollectors.immutableList());

            files.stream().forEach(i -> {
                nodes.add(i);
            });

            final ImmutableList<Ds3TreeTableValue> directoryValues = bucketResponse.getListBucketResult()
                    .getCommonPrefixes().stream().map(CommonPrefixes::getPrefix)
                    .map(c -> new Ds3TreeTableValue(value.getBucketName(), c, Ds3TreeTableValue.Type.Directory, FileSizeFormat.getFileSizeType(0), ""))
                    .collect(GuavaCollectors.immutableList());

            directoryValues.stream().forEach(i -> {
                addAllDescendents(i, nodes);
            });


        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final SignatureException e) {
            e.printStackTrace();
        }


    }
}
