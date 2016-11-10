package com.spectralogic.dsbrowser.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.UNORDERED;

/**
 * Stream {@link Collector collectors} for Guava types.
 */
public final class GuavaCollectors {
    private GuavaCollectors() {
        throw new AssertionError("No instances.");
    }

    /**
     * Collect a stream of elements into an {@link ImmutableList}.
     */
    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> immutableList() {
        return Collector.of(ImmutableList.Builder::new, ImmutableList.Builder::add,
                (l, r) -> l.addAll(r.build()), ImmutableList.Builder<T>::build);
    }

    /**
     * Collect a stream of elements into an {@link ImmutableSet}.
     */
    public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> immutableSet() {
        return Collector.of(ImmutableSet.Builder::new, ImmutableSet.Builder::add,
                (l, r) -> l.addAll(r.build()), ImmutableSet.Builder<T>::build, UNORDERED);
    }


}