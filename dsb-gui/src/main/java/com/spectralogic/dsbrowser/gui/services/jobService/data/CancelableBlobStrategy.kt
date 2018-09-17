package com.spectralogic.dsbrowser.gui.services.jobService.data

import com.spectralogic.ds3client.helpers.JobPart
import com.spectralogic.ds3client.helpers.strategy.blobstrategy.BlobStrategy
import com.spectralogic.ds3client.helpers.strategy.blobstrategy.PutSequentialBlobStrategy
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableBooleanValue
import java.security.Provider
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

class CancelableBlobStrategy(val strategy: BlobStrategy, val cancelled: Supplier<Boolean>) : BlobStrategy by strategy {

    override public fun getWork(): Iterable<JobPart> = CancelableIterable(strategy.work, cancelled)

    private class CancelableIterable<T> (val innerIterable: Iterable<T>, val cancelled: Supplier<Boolean>): Iterable<T> by innerIterable {
        override fun iterator(): Iterator<T> = CancelableIterator(innerIterable.iterator(), cancelled)
    }

    private class CancelableIterator<T> (val innterIterator: Iterator<T>, val cancelled: Supplier<Boolean>): Iterator<T> by innterIterator {
        override fun hasNext(): Boolean {
            return if(cancelled.get()) {
                false
            } else {
                innterIterator.hasNext()
            }
        }
    }
}