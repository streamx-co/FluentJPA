package co.streamx.fluent.JPA.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import lombok.RequiredArgsConstructor;

@SuppressWarnings("serial")
@RequiredArgsConstructor
public class ScopedHashSet<E> extends HashSet<E> {
    private final Set<E> upper;

    @Override
    public boolean contains(Object o) {
        return super.contains(o) || upper.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return super.containsAll(c) || upper.containsAll(c);
    }

    @Override
    public int size() {
        return super.size() + upper.size();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && upper.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }
}
