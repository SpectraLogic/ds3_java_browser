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

package com.spectralogic.dsbrowser.gui.services.jobService.util

import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import com.spectralogic.ds3client.helpers.FileObjectGetter
import com.spectralogic.ds3client.helpers.ObjectChannelBuilderLogger
import com.spectralogic.dsbrowser.api.services.logging.LogType
import com.spectralogic.dsbrowser.api.services.logging.LoggingService
import com.spectralogic.dsbrowser.gui.util.StringConstants
import java.nio.channels.SeekableByteChannel
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class DelimChannelBuilder(private val ocb: Ds3ClientHelpers.ObjectChannelBuilder, private val localPath: Path) : Ds3ClientHelpers.ObjectChannelBuilder by ocb {
    private val localDelim = localPath.fileSystem.separator
    override fun buildChannel(p0: String): SeekableByteChannel {
        return ocb.buildChannel(if (localPath.fileSystem.separator != "/") {
            p0.replace("/", localDelim)
        } else {
            p0
        })
    }
}