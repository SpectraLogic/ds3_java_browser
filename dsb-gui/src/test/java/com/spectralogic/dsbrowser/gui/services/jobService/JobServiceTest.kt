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

import com.spectralogic.ds3client.Ds3Client
import io.reactivex.Completable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.util.UUID

const val INITIAL_MESSAGE: String = ""
const val RAN_MESSAGE: String = "ran"
const val PERCENT_RAN: Long = 1L
const val VISIBLE: Boolean = true

class JobServiceTest {

    private var jobService: IncrementalJobService? = null

    private class IncrementalJobService() : JobService() {
        override fun getDs3Client(): Ds3Client {
            return Mockito.mock(Ds3Client::class.java)
        }

        override fun jobUUID(): UUID = UUID.randomUUID()

        override fun finishedCompletable(): Completable {
            return Completable.fromAction {
                // Simplified example, just skips the whole thing if cancelled is true
                    message.set(RAN_MESSAGE)
                    totalJob.set(PERCENT_RAN)
                    title.set(RAN_MESSAGE)
                    visible.set(false)
            }
        }

        override fun cancel() {
        }
    }

    @Before
    fun setUp() {
        jobService = IncrementalJobService()
    }

    @Test
    fun messageTest() {
        var message = "N/A"
        jobService!!.messageObservable().subscribe { t: String -> message = t }
        assertThat(message).isEqualTo(INITIAL_MESSAGE)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(message).isEqualTo(RAN_MESSAGE)
    }

    @Test
    fun titleTest() {
        var title = "N/A"
        jobService!!.titleObservable().subscribe { t: String -> title = t }
        assertThat(title).isEqualTo(INITIAL_MESSAGE)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(title).isEqualTo(RAN_MESSAGE)
    }

    @Test
    fun visibleTest() {
        var visible = false
        jobService!!.visabilityObservable().subscribe { t: Boolean -> visible = t }
        assertThat(visible).isEqualTo(VISIBLE)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(visible).isEqualTo(false)
    }

    @Test
    fun totalJobTest() {
        var total = 100.00
        jobService!!.jobSizeObservable().subscribe { t: Number -> total = t.toDouble() }
        assertThat(total).isEqualTo(0.0)
        jobService!!.finishedCompletable().blockingGet()
        assertThat(total).isEqualTo(1.0)
    }
}