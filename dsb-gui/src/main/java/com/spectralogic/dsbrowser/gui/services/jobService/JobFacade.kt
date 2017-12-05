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
import io.reactivex.Observable
import io.reactivex.Observer
import javafx.beans.property.LongProperty
import javafx.beans.property.SimpleBooleanProperty
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong

interface JobFacade {
    public fun titleObservable(): Observable<String>
    public fun jobSizeObservable(): Observable<Number>
    public fun messageObservable(): Observable<String>
    public fun visabilityObservable(): Observable<Boolean>
    public fun finishedCompletable(): Completable
    public fun sentObservable(): Observable<Number>
    public fun totalJobSizeAsProperty(): LongProperty
    public fun visibleProperty(): SimpleBooleanProperty
    public fun jobUUID(): UUID
}