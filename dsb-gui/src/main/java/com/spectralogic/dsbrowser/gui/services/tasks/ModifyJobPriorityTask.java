/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
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
