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

import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

class ErrorByteChannel() : SeekableByteChannel {

    override fun isOpen(): Boolean {
        return true
    }

    override fun position(): Long = 0L

    override fun position(newPosition: Long): SeekableByteChannel = this

    override fun write(src: ByteBuffer?): Int {
        throw IllegalArgumentException("This should never be called write")
    }

    override fun size(): Long = 0

    override fun close() {
    }

    override fun truncate(size: Long): SeekableByteChannel = this

    override fun read(dst: ByteBuffer?): Int {
        throw IllegalArgumentException("This should never be called read")
    }
}