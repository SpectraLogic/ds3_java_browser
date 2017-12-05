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
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import java.util.*

class JobTask(private val wrappedJob: JobFacade) : Ds3JobTask() {
    @Throws(Throwable::class)
    override fun executeJob() {
        var throwable : Throwable? = null
        wrappedJob.titleObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { title: String -> updateTitle(title) }
                .subscribe()

        wrappedJob.messageObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { message: String -> updateMessage(message) }
                .subscribe()

        wrappedJob.jobSizeObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { size: Number -> updateProgress(0L, size.toLong()) }
                .subscribe()

        wrappedJob.sentObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { size: Number -> updateProgress(size.toLong(), wrappedJob.totalJobSizeAsProperty().get()) }
                .subscribe()

        wrappedJob.visabilityObservable()
                .observeOn(JavaFxScheduler.platform())
                .doOnNext { visible: Boolean -> isVisible.set(visible) }
                .subscribe()

        wrappedJob.finishedCompletable()
                .subscribe(Action {  }, Consumer { throwable = it })
        if(throwable != null) {
            throw throwable!!
        }

    }

    override fun getJobId(): UUID = wrappedJob.jobUUID()

    public val isVisible: BooleanProperty = SimpleBooleanProperty(true)
}