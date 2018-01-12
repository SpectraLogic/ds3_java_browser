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

package com.spectralogic.dsbrowser.util

/**
 * andThen executes some lambda after executing the source lambda, but preserves
 * the return type of the first lambda
 * @param T input of the calller
 * @param R return type of caller
 * @property block the lambda to be included in the composed lambda
 * @return A new lambda with both behaviors, but returning the same value as the origional lambda
 * Note the crossinline keyword on block
 * The block will not be inlined into the function but the body of the new lambda
 */
inline fun <T, R> ((T) -> R).andThen(crossinline block: () -> Unit): (T) -> R {
    return { t: T ->
        val r: R = this.invoke(t)
        block.invoke()
        r
    }
}

/**
 * exists conditionally executes a lambda if the calling object is not null
 * @param T the type of the object that we will be checking
 * @property block the lambda that will be executed if the object exists
 * @return The results oƒ invoking block on T or null
 * The exist method will be inlined into the call site, including block
 */
inline fun <T> T?.exists(block: (T) -> Any?): Any? {
    return if (this == null) {
        null
    } else {
        block.invoke(this)
    }
}

private inline fun <T> T?.populated(block: T.() -> Boolean): Boolean {
    return if (this == null) {
        false
    } else {
        block.invoke(this)
    }
}

fun String?.populated(): Boolean = this.populated { !isNotEmpty() }
fun Collection<*>?.populated(): Boolean = this.populated { isNotEmpty() }
fun Iterator<*>?.populated(): Boolean = this.populated { hasNext() }
