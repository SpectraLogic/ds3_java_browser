package com.spectralogic.dsbrowser.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
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

    /**
     * Collect a stream of elements into an {@link ImmutableMap}.
     */
    public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> immutableMap(
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Supplier<ImmutableMap.Builder<K, V>> supplier = ImmutableMap.Builder::new;

        BiConsumer<ImmutableMap.Builder<K, V>, T> accumulator = (b, t) -> b.put(keyMapper.apply(t), valueMapper.apply(t));

        BinaryOperator<ImmutableMap.Builder<K, V>> combiner = (l, r) -> l.putAll(r.build());

        Function<ImmutableMap.Builder<K, V>, ImmutableMap<K, V>> finisher = ImmutableMap.Builder::build;

    return Collector.of(supplier, accumulator, combiner, finisher);
    }
}