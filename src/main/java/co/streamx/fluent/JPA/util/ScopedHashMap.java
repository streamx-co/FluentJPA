package co.streamx.fluent.JPA.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;

@SuppressWarnings("serial")
@RequiredArgsConstructor
public class ScopedHashMap<K, V> extends HashMap<K, V> {

    private final Map<K, V> upper;
    private List<K> removedKeys = Collections.emptyList();

    @Override
    public boolean containsKey(Object o) {
        return super.containsKey(o) || upper.containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return super.containsValue(o) || upper.containsValue(o);
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
    public V get(Object key) {
        return getOrDefault(key, null);
    }

    @Override
    public V getOrDefault(Object key,
                          V defaultValue) {
        V v = super.get(key);
        return v != null ? v : removedKeys.contains(key) ? defaultValue : upper.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        V removed = super.remove(key);
        if (removed != null)
            return removed;
        removed = super.get(key);
        if (removed != null) {
        if (removedKeys == Collections.emptyList())
            removedKeys = new ArrayList<>();
            removedKeys.add((K) key);
        }

        return removed;
    }

    protected Iterator<K> keysIterator() {

        Iterator<K> scopeIt = super.keySet().iterator();

        return new Iterator<K>() {

            private Iterator<K> it = scopeIt;

            @Override
            public boolean hasNext() {
                boolean hasNext = it.hasNext();
                if (!hasNext) {
                    if (it == scopeIt && upper instanceof ScopedHashMap) {
                        it = ((ScopedHashMap<K, V>) upper).keysIterator();
                        return it.hasNext();
                    }
                }
                return hasNext;
            }

            @Override
            public K next() {
                return it.next();
            }
        };
    }

    @Override
    public boolean remove(Object key,
                          Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        return Objects.equals(this, o);
    }
}
