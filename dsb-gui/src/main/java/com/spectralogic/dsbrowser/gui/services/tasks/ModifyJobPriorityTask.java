package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.components.modifyjobpriority.ModifyJobPriorityModel;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;

import java.util.Optional;

public class ModifyJobPriorityTask extends Ds3Task {

    private final ModifyJobPriorityModel value;
    private final Priority newPriority;

    public ModifyJobPriorityTask(final ModifyJobPriorityModel value, final Priority newPriority) {
        this.value = value;
        this.newPriority = newPriority;
    }

    @Override
    protected Optional<Object> call() throws Exception {
        value.getSession().getClient().modifyJobSpectraS3(
                new ModifyJobSpectraS3Request(value.getJobID()).withPriority(newPriority));
        return Optional.of(StringConstants.SUCCESS);
    }
}
