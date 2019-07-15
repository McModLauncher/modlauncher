/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 *
 *     Copyright (C) 2017-2019 cpw
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cpw.mods.modlauncher.api;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Typed key-value map, similar to the AttributeKey netty stuff. Uses lambda interfaces for get/set.
 */
public final class TypesafeMap {
    private static final ConcurrentHashMap<Class<?>, TypesafeMap> maps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Key<Object>, Object> map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Key<Object>> keys = new ConcurrentHashMap<>();

    public TypesafeMap() {
    }

    public TypesafeMap(Class<?> owner) {
        KeyBuilder.keyBuilders.getOrDefault(owner, Collections.emptyList()).forEach(kb -> kb.buildKey(this));
        maps.put(owner, this);
    }

    public <V> Optional<V> get(Key<V> key) {
        return Optional.ofNullable(key.clz.cast(map.get(key)));
    }

    public <V> V computeIfAbsent(Key<V> key, Function<? super Key<V>, ? extends V> valueFunction) {
        return computeIfAbsent(this.map, key, valueFunction);
    }

    @SuppressWarnings("unchecked")
    private <C1, C2, V> V computeIfAbsent(ConcurrentHashMap<C1, C2> map, Key<V> key, Function<? super Key<V>, ? extends V> valueFunction) {
        return (V) map.computeIfAbsent((C1) key, (Function<? super C1, ? extends C2>) valueFunction);
    }

    private ConcurrentHashMap<String, Key<Object>> getKeyIdentifiers() {
        return keys;
    }

    /**
     * Unique blackboard key
     */
    public static final class Key<T> implements Comparable<Key<T>> {
        private static final AtomicLong idGenerator = new AtomicLong();
        private final String name;
        private final long uniqueId;
        private final Class<T> clz;

        private Key(String name, Class<T> clz) {
            this.clz = clz;
            this.name = name;
            this.uniqueId = idGenerator.getAndIncrement();
        }

        @SuppressWarnings("unchecked")
        public static <V> Key<V> getOrCreate(TypesafeMap owner, String name, Class<? super V> clazz) {
            Key<V> result = (Key<V>) owner.getKeyIdentifiers().computeIfAbsent(name, (n) -> new Key<>(n, (Class<Object>) clazz));
            if (result.clz != clazz) {
                throw new IllegalArgumentException("Invalid type");
            }
            return result;
        }

        public static <V> Supplier<Key<V>> getOrCreate(Supplier<TypesafeMap> owner, String name, Class<V> clazz) {
            return () -> getOrCreate(owner.get(), name, clazz);
        }

        public final String name() {
            return name;
        }

        @Override
        public int hashCode() {
            return (int) (this.uniqueId ^ (this.uniqueId >>> 32));
        }

        @Override
        public boolean equals(Object obj) {
            try {
                return this.uniqueId == ((Key) obj).uniqueId;
            } catch (ClassCastException cc) {
                return false;
            }
        }

        @Override
        public int compareTo(Key o) {
            if (this == o) {
                return 0;
            }

            if (this.uniqueId < o.uniqueId) {
                return -1;
            }

            if (this.uniqueId > o.uniqueId) {
                return 1;
            }

            throw new RuntimeException("Huh?");
        }
    }

    public static final class KeyBuilder<T> implements Supplier<Key<T>> {
        private static final Map<Class<?>, List<KeyBuilder<?>>> keyBuilders = new HashMap<>();
        private final Class<?> owner;
        private final String name;
        private final Class<? super T> clazz;
        private Key<T> key;

        public KeyBuilder(String name, Class<? super T> clazz, Class<?> owner) {
            this.name = name;
            this.clazz = clazz;
            this.owner = owner;
            keyBuilders.computeIfAbsent(owner, k -> new ArrayList<>()).add(this);
        }

        final void buildKey(TypesafeMap map) {
            key = Key.getOrCreate(map, name, clazz);
        }

        @Override
        public Key<T> get() {
            if (key == null && maps.containsKey(owner)) {
                buildKey(maps.get(owner));
            }
            if (key == null) {
                throw new NullPointerException("Missing map");
            }
            return key;
        }
    }
}
