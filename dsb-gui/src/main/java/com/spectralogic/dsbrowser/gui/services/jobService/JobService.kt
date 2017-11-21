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

import com.github.thomasnield.rxkotlinfx.toObservable
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers
import io.reactivex.Completable
import io.reactivex.Observable
import javafx.beans.property.*
import org.reactivestreams.Publisher
import java.util.*

abstract class JobService : JobFacade {
    protected val title: StringProperty = SimpleStringProperty("")
    private val titleObservable: Observable<String> = title.toObservable()

    protected val message: StringProperty = SimpleStringProperty("")
    private val messageObservable: Observable<String> = message.toObservable()

    protected val totalJob: LongProperty = SimpleLongProperty(0L)
    private val totalObservable: Observable<Number> = totalJob.toObservable()

    protected val visible: BooleanProperty = SimpleBooleanProperty(true)
    private val visibilityObservable: Observable<Boolean> = visible.toObservable()

    protected val sent: LongProperty = SimpleLongProperty(0L)
    private val sentObservable: Observable<Number> = sent.toObservable()

    override fun titleObservable(): Observable<String> = titleObservable
    override fun messageObservable(): Observable<String> = messageObservable
    override fun visabilityObservable(): Observable<Boolean> = visibilityObservable
    override fun jobSizeObservable(): Observable<Number> = totalObservable
    override fun sentObservable(): Observable<Number> = sentObservable
    override fun totalJob(): LongProperty = totalJob

    abstract override fun finishedCompletable(): Completable
}