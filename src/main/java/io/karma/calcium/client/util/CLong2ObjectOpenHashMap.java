package io.karma.calcium.client.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CLong2ObjectOpenHashMap<V> implements Long2ObjectMap<V> {
    private final int numBuckets;
    private final ReadWriteLock[] locks;
    private final Long2ObjectOpenHashMap<V>[] buckets;
    private V defaultValue;

    public CLong2ObjectOpenHashMap() {
        this(Runtime.getRuntime().availableProcessors());
    }

    @SuppressWarnings("unchecked")
    public CLong2ObjectOpenHashMap(final int numBuckets) {
        this.numBuckets = numBuckets;

        locks = new ReadWriteLock[numBuckets];
        Arrays.fill(locks, new ReentrantReadWriteLock());

        buckets = new Long2ObjectOpenHashMap[numBuckets];
        Arrays.fill(buckets, new Long2ObjectOpenHashMap<V>());
    }

    @Override
    public int size() {
        int size = 0;

        for (int i = 0; i < numBuckets; i++) {
            final Lock lock = locks[i].readLock();

            try {
                if (lock.tryLock()) {
                    size += buckets[i].size();
                }
            }
            finally {
                lock.unlock();
            }
        }

        return size;
    }

    @Override
    // TODO: find a nicer way to do this..
    public synchronized void defaultReturnValue(V value) {
        defaultValue = value;
    }

    @Override
    public V defaultReturnValue() {
        return defaultValue;
    }

    @Override
    public @NotNull ObjectSet<Entry<V>> long2ObjectEntrySet() {
        final ObjectOpenHashSet<Entry<V>> entries = new ObjectOpenHashSet<>();

        for (int i = 0; i < numBuckets; i++) {
            final Lock lock = locks[i].readLock();

            try {
                if (lock.tryLock()) {
                    entries.addAll(buckets[i].long2ObjectEntrySet());
                }
            }
            finally {
                lock.unlock();
            }
        }

        return entries;
    }

    @Override
    public @NotNull LongSet keySet() {
        final LongOpenHashSet keys = new LongOpenHashSet();

        for (int i = 0; i < numBuckets; i++) {
            final Lock lock = locks[i].readLock();

            try {
                if (lock.tryLock()) {
                    keys.addAll(buckets[i].keySet());
                }
            }
            finally {
                lock.unlock();
            }
        }

        return keys;
    }

    @Override
    public @NotNull ObjectCollection<V> values() {
        final ObjectArrayList<V> values = new ObjectArrayList<>();

        for (int i = 0; i < numBuckets; i++) {
            final Lock lock = locks[i].readLock();

            try {
                if (lock.tryLock()) {
                    values.addAll(buckets[i].values());
                }
            }
            finally {
                lock.unlock();
            }
        }

        return values;
    }

    @Override
    public boolean containsKey(final long key) {
        for (int i = 0; i < numBuckets; i++) {
            final Lock lock = locks[i].readLock();

            try {
                if (lock.tryLock()) {
                    if (buckets[i].containsKey(key)) {
                        return true;
                    }
                }
            }
            finally {
                lock.unlock();
            }
        }

        return false;
    }

    @Override
    public @Nullable V get(final long key) {
        final int bucket = getBucket(key);
        final Lock lock = locks[bucket].readLock();

        try {
            if (lock.tryLock()) {
                return buckets[bucket].get(key);
            }
        }
        finally {
            lock.unlock();
        }

        return null;
    }

    @Override
    public @Nullable V put(final long key, final @Nullable V value) {
        final int bucket = getBucket(key);
        final Lock lock = locks[bucket].writeLock();

        try {
            if (lock.tryLock()) {
                return buckets[bucket].put(key, value);
            }
        }
        finally {
            lock.unlock();
        }

        return null;
    }

    @Override
    public @Nullable V remove(final long key) {
        final int bucket = getBucket(key);
        final Lock lock = locks[bucket].writeLock();

        try {
            if (lock.tryLock()) {
                return buckets[bucket].remove(key);
            }
        }
        finally {
            lock.unlock();
        }

        return null;
    }

    @Override
    public void clear() {
        for(int i = 0; i < numBuckets; i++) {
            final Lock lock = locks[i].writeLock();

            try {
                if(lock.tryLock()) {
                    buckets[i].clear();
                }
            }
            finally {
                lock.unlock();
            }
        }
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < numBuckets; i++) {
            final Lock lock = locks[i].readLock();

            try {
                if (lock.tryLock()) {
                    if (!buckets[i].isEmpty()) {
                        return false;
                    }
                }
            }
            finally {
                lock.unlock();
            }
        }

        return true;
    }

    @Override
    public boolean containsValue(final @Nullable Object value) {
        if (value == null) {
            return false;
        }

        for (int i = 0; i < numBuckets; i++) {
            final Lock lock = locks[i].readLock();

            try {
                if (lock.tryLock()) {
                    if (buckets[i].containsValue(value)) {
                        return true;
                    }
                }
            }
            finally {
                lock.unlock();
            }
        }

        return false;
    }

    @Override
    public void putAll(final @NotNull Map<? extends Long, ? extends V> map) {
        // TODO: this is horrible, but we don't use it sooo.. >.>
        for (final Map.Entry<? extends Long, ? extends V> entry : map.entrySet()) {
            put((long) entry.getKey(), entry.getValue());
        }
    }

    private int getBucket(final long key) {
        final int hash = Long.hashCode(key);
        return Math.abs(hash == Integer.MIN_VALUE ? 0 : hash) % numBuckets;
    }
}
