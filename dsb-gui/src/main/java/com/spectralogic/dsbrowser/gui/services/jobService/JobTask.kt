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

import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.concurrent.Task
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class JobTask(private val jorb : JobFacade) : Ds3JobTask() {
    override fun executeJob() {
        jorb.titleObservable()
                .doOnNext { s : String -> updateTitle(s) }
                .doOnError { t: Throwable -> throw t }
                .subscribe()

        jorb.messageObservable()
                .doOnNext { s : String -> updateMessage(s) }
                .doOnError { t : Throwable -> throw t }
                .subscribe()

        jorb.progressObservable()
                .doOnNext { n: Number -> updateProgress(n.toDouble(), 100.0)}
                .doOnError { t : Throwable -> throw t}
                .subscribe()

        jorb.visabilityObservable()
                .doOnNext { b : Boolean -> isVisible.set(b) }
                .subscribe()

        val disposed : Disposable = jorb.finishedCompletable().subscribe()
        while(!disposed.isDisposed) {
            Thread.sleep(1000)
        }
    }

    override fun getJobId(): UUID = UUID.fromString("1")

    public val isVisible : BooleanProperty = SimpleBooleanProperty(true)
}