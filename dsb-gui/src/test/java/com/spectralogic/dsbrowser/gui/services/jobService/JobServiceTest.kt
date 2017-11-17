/*
 * ****************************************************************************
 *    Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.jobService

import io.reactivex.Completable
import io.reactivex.functions.Consumer
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.*

const val INITIAL_MESSAGE: String = ""
const val RAN_MESSAGE: String = "ran"
const val PERCENT_INITIAL: Long = 0L
const val PERCENT_RAN: Long = 1L
const val VISIBLE: Boolean = true
const val VISIBLE_RAN: Boolean = false


class JobServiceTest {
    private var jobService: IncrementalJobService? = null

    protected class IncrementalJobService() : JobService() {
        override fun finishedCompletable(): Completable {
            return Completable.fromAction {
                message.set(RAN_MESSAGE)
                totalJob.set(PERCENT_RAN)
                title.set(RAN_MESSAGE)
                visible.set(false)
            }
        }
    }

    @Before
    fun setUp() {
        jobService = IncrementalJobService()
    }

    @Test
    fun messageTest() {
        var message = "N/A"
        jobService!!.messageObservable().subscribe(Consumer { t: String -> message = t })
        assertThat(message).isEqualTo(INITIAL_MESSAGE)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(message).isEqualTo(RAN_MESSAGE)
    }

    @Test
    fun progressTest() {
        var progress = 100.00
        jobService!!.progressObservable().subscribe(Consumer { t: Number -> progress = t.toDouble() })
        assertThat(progress).isEqualTo(PERCENT_INITIAL)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(progress).isEqualTo(PERCENT_RAN)
    }

    @Test
    fun titleTest() {
        var title = "N/A"
        jobService!!.titleObservable().subscribe(Consumer { t: String -> title = t })
        assertThat(title).isEqualTo(INITIAL_MESSAGE)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(title).isEqualTo(RAN_MESSAGE)
    }

    @Test
    fun visibleTest() {
        var visible = false;
        jobService!!.visabilityObservable().subscribe(Consumer { t: Boolean -> visible = t })
        assertThat(visible).isEqualTo(VISIBLE)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(visible).isEqualTo(false)
    }

    @Test
    fun totalJobTest() {
        var total = 100.00
        jobService!!.jobSizeObservable().subscribe(Consumer { t: Number -> total = t.toDouble() })
        assertThat(total).isEqualTo(PERCENT_INITIAL)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(total).isEqualTo(PERCENT_RAN)
    }

}