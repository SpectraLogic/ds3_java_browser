/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetPhysicalPlacementForObjectsSpectraS3Response;
import com.spectralogic.ds3client.commands.spectrads3.GetTapePartitionSpectraS3Request;
import com.spectralogic.ds3client.models.Ds3Target;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.models.Pool;
import com.spectralogic.ds3client.models.Tape;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PhysicalPlacementModel;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.PoolEntry;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.ReplicationEntry;
import com.spectralogic.dsbrowser.gui.components.physicalplacement.TapeEntry;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormatKt;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import kotlin.collections.EmptyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PhysicalPlacementTask extends Ds3Task<PhysicalPlacementModel> {
    private final static Logger LOG = LoggerFactory.getLogger(PhysicalPlacementTask.class);

    private final Ds3Common ds3Common;
    private final Workers workers;
    private final DateTimeUtils dateTimeUtils;
    private final Ds3TreeTableValue value;
    private final LoggingService loggingService;
    private final GetDirectoryObjects.Companion.GetDirectoryObjectsFactory getDirectoryObjectsFactory;

    @Inject
    public PhysicalPlacementTask(
            @Assisted final Ds3TreeTableValue value,
            final Ds3Common ds3Common,
            final DateTimeUtils dateTimeUtils,
            final Workers workers,
            final LoggingService loggingService,
            final GetDirectoryObjects.Companion.GetDirectoryObjectsFactory getDirectoryObjectsFactory) {
        this.ds3Common = ds3Common;
        this.value = value;
        this.workers = workers;
        this.dateTimeUtils = dateTimeUtils;
        this.loggingService = loggingService;
        this.getDirectoryObjectsFactory = getDirectoryObjectsFactory;
    }

    @Override
    protected PhysicalPlacementModel call() throws Exception {
        final Ds3Client client = ds3Common.getCurrentSession().getClient();
        final ImmutableList<Ds3Object> list;
        switch (value.getType()) {
            case File:
                list = ImmutableList.of(new Ds3Object(value.getFullName(), value.getSize()));
                break;
            case Directory:
                final GetDirectoryObjects getDirectoryObjects = getDirectoryObjectsFactory.create(value.getBucketName(), value.getFullName());
                workers.execute(getDirectoryObjects);
                final ListBucketResult listBucketResult = getDirectoryObjects.getValue();
                list = null != listBucketResult ? listBucketResult
                        .getObjects()
                        .stream()
                        .map(item -> new Ds3Object(item.getKey(), item.getSize()))
                        .collect(GuavaCollectors.immutableList()) : ImmutableList.of();
                break;
            default:
                list = ImmutableList.of();
        }
        final GetPhysicalPlacementForObjectsSpectraS3Response response = client
                .getPhysicalPlacementForObjectsSpectraS3(
                        new GetPhysicalPlacementForObjectsSpectraS3Request(value.getBucketName(), list));
        if (response.getPhysicalPlacementResult().getPools() == null) {
            response.getPhysicalPlacementResult().setPools(EmptyList.INSTANCE);
        }
        if (response.getPhysicalPlacementResult().getTapes() == null) {
            response.getPhysicalPlacementResult().setTapes(EmptyList.INSTANCE);
        }
        if (response.getPhysicalPlacementResult().getDs3Targets() == null) {
            response.getPhysicalPlacementResult().setDs3Targets(EmptyList.INSTANCE);
        }
        return new PhysicalPlacementModel(
                buildPoolEntries(response),
                buildReplicationEntries(response),
                buildTapeEntries(response)
        );
    }

    private ImmutableList<TapeEntry> buildTapeEntries(final GetPhysicalPlacementForObjectsSpectraS3Response response) {
        return response.getPhysicalPlacementResult()
                .getTapes()
                .stream()
                .map(this::getTapeEntry)
                .collect(GuavaCollectors.immutableList());
    }

    private TapeEntry getTapeEntry(final Tape tape) {
        return new TapeEntry(
                tape.getBarCode(),
                tape.getSerialNumber(),
                tape.getType(),
                tape.getState().name(),
                tape.getWriteProtected(),
                FileSizeFormatKt.toByteRepresentation(tape.getAvailableRawCapacity()),
                FileSizeFormatKt.toByteRepresentation(tape.getTotalRawCapacity() - tape.getAvailableRawCapacity()),
                getPartitionName(tape.getPartitionId()),
                dateTimeUtils.format(tape.getLastModified()),
                tape.getEjectLabel() == null ? "" : tape.getEjectLabel(),
                tape.getEjectLocation() == null ? "" : tape.getEjectLocation());
    }

    private String getPartitionName(final UUID id) {
        try {
            return ds3Common.getCurrentSession().getClient()
                    .getTapePartitionSpectraS3(new GetTapePartitionSpectraS3Request(id.toString()))
                    .getTapePartitionResult()
                    .getName();
        } catch (final IOException e) {
            LOG.error("Could not get Tape Partition", e);
            loggingService.logInternationalMessage("unableToGetPartitionName", LogType.ERROR);
            return id.toString();
        }
    }

    private ImmutableList<ReplicationEntry> buildReplicationEntries(final GetPhysicalPlacementForObjectsSpectraS3Response response) {
        return response
                .getPhysicalPlacementResult()
                .getDs3Targets()
                .stream()
                .map(this::getReplicationEntry)
                .collect(GuavaCollectors.immutableList());
    }

    private ReplicationEntry getReplicationEntry(final Ds3Target ds3Target) {
        return new ReplicationEntry(
                ds3Target.getDataPathEndPoint(),
                ds3Target.getDataPathPort(),
                ds3Target.getAdminAuthId(),
                ds3Target.getId().toString(),
                ds3Target.getName(),
                ds3Target.getAccessControlReplication().name(),
                ds3Target.getState().name(),
                ds3Target.getPermitGoingOutOfSync(),
                ds3Target.getQuiesced().name());
    }

    private ImmutableList<PoolEntry> buildPoolEntries(final GetPhysicalPlacementForObjectsSpectraS3Response response) {
        return response
                .getPhysicalPlacementResult()
                .getPools()
                .stream()
                .map(this::getPoolEntry)
                .collect(GuavaCollectors.immutableList());
    }

    private PoolEntry getPoolEntry(final Pool pool) {
        return new PoolEntry(
                pool.getName(),
                pool.getHealth().name(),
                pool.getType().toString(),
                pool.getPartitionId().toString());
    }

    public interface PhysicalPlacementTaskFactory {
        public PhysicalPlacementTask create(final Ds3TreeTableValue value);
    }
}

