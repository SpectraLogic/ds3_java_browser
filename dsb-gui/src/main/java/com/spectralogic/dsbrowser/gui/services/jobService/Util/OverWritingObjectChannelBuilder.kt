/*
 * ***************************************************************************
 *   Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
 *   Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *   this file except in compliance with the License. A copy of the License is located at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file.
 *   This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *   CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *   specific language governing permissions and limitations under the License.
 * ***************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.jobService.Util

import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectGetter
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.util.StringConstants
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class OverWritingObjectChannelBuilder(private val ocb: FileObjectGetter,
                                      private val root: Path,
                                      private val loggingService: LoggingService,
                                      private val resourceBundle: ResourceBundle) : Ds3ClientHelpers.ObjectChannelBuilder by ocb {
    override fun buildChannel(p0: String?): SeekableByteChannel {
        if (Files.exists(root.resolve(p0))) {
            loggingService.logMessage( resourceBundle.getString("fileOverridden")
                            + StringConstants.SPACE + p0 + StringConstants.SPACE
                            + resourceBundle.getString("to")
                            + StringConstants.SPACE + root, LogType.SUCCESS)
        }
        return ocb.buildChannel(p0)
    }

}