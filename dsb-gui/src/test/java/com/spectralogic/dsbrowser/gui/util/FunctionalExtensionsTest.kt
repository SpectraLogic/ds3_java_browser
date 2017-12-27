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

package com.spectralogic.dsbrowser.gui.util

import com.spectralogic.dsbrowser.util.andThen
import com.spectralogic.dsbrowser.util.exists
import org.assertj.core.api.Assertions.*
import org.junit.Test

class FunctionalExtensionsTest {

    @Test
    fun existDoesNotFalseExecute() {
        val h: String? = null
        var b = false
        h.exists {
            b = true
            it
        }
        assertThat(b).isFalse()
    }

    @Test
    fun existDoesOnExist() {
        val h: String? = "Hello World"
        var b = false
        h.exists {
            b = true
            null
        }
        assertThat(b).isTrue()
    }

    @Test
    fun existsAssignment() {
        val h: String? = "Hello World"
        val b = h.exists { it }
        assertThat(b).isEqualTo("Hello World")
    }

    @Test
    fun existsInlineAssignment() {
        val b = "H".exists { it }
        assertThat(b).isEqualTo("H")
    }

    @Test
    fun existsNull() {
        val b = null.exists { "Hello World" }
        assertThat(b).isNull()
    }

    @Test
    fun notExistingAssignment() {
        val h: String? = null
        val b = h.exists { it }
        assertThat(b).isNull()
    }

    @Test
    fun andThenTest() {
        val a = { b: Int -> b + 2 }
        var f = false
        val c = a.andThen { f = true }.invoke(1)
        assertThat(f).isTrue()
        assertThat(c).isEqualTo(3)
    }

}