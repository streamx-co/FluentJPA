package co.streamx.fluent.JPA;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

interface Streams {

    static <A, B, C> Stream<C> zip(List<A> lista,
                                          List<B> listb,
                                          BiFunction<? super A, ? super B, ? extends C> zipper) {
        int longestLength = Math.max(lista.size(), listb.size());
        return IntStream.range(0, longestLength).mapToObj(i -> {
            return zipper.apply(i < lista.size() ? lista.get(i) : null, i < listb.size() ? listb.get(i) : null);
        });
    }

    static <FROM, TO> List<TO> map(List<FROM> source,
                                          Function<? super FROM, ? extends TO> mapper) {
        List<TO> result = new ArrayList<>(source.size());
        source.forEach(f -> result.add(mapper.apply(f)));
        return result;
    }

    static <E> List<E> join(List<E> a,
                            List<E> b) {
        return new ListList<>(a, b);
    }

    @SuppressWarnings("serial")
    @RequiredArgsConstructor
    static final class ListList<E> extends AbstractList<E> implements RandomAccess, java.io.Serializable {

        private final List<E> a;
        private final List<E> b;

        @Override
        public int size() {
            return a.size() + b.size();
        }

        @Override
        public E get(int index) {
            int sizeA = a.size();
            return index < sizeA ? a.get(index) : b.get(index - sizeA);
        }
    }
}
