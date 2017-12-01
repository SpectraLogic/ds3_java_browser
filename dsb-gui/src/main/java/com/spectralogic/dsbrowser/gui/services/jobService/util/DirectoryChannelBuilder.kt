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
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path

class DirectoryChannelBuilder(private val objectChannelBuilder: Ds3ClientHelpers.ObjectChannelBuilder, private val localPath : Path) :  Ds3ClientHelpers.ObjectChannelBuilder by objectChannelBuilder {
    override fun buildChannel(name: String): SeekableByteChannel {
        return if(name.endsWith(localPath.fileSystem.separator)) {
            Files.createDirectories(localPath.resolve(name))
            ErrorByteChannel()
        } else {
            objectChannelBuilder.buildChannel(name)
        }
    }
}