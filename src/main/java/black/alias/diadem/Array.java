/*******************************************************************************
 * Copyright 2011
 * Mario Zechner <badlogicgames@gmail.com>
 * Nathan Sweet <nathan.sweet@gmail.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package black.alias.diadem;

import java.util.ArrayList;
import java.util.Iterator;

/** A resizable, ordered or unordered array of objects. If unordered, this class avoids a memory copy when removing elements (the
 * last element is moved to the removed element's position).
 * @author Nathan Sweet */
public class Array<T> implements Iterable<T> {
    /** Provides direct access to the underlying array. If the Array's generic type is not Object, this field may only be
     * accessed if the {@link Array#Array(boolean, int, Class)} constructor was used. */
    public T[] items;

    public int size;
    public boolean ordered;

    private ArrayIterable<T> iterable;
    private Predicate.PredicateIterable<T> predicateIterable;

    /** Creates an ordered array with a capacity of 16. */
    public Array () {
        this(true, 16);
    }

    /** Creates an ordered array with the specified capacity. */
    public Array (int capacity) {
        this(true, capacity);
    }

    /** @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     *           memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown. */
    @SuppressWarnings("unchecked")
    public Array (boolean ordered, int capacity) {
        this.ordered = ordered;
        items = (T[])new Object[capacity];
    }

    /** Creates a new array containing the elements in the specific array. The new array will be ordered if the specific array is
     * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
     * grown. */
    public Array (Array<? extends T> array) {
        this(array.ordered, array.size, array.items.getClass().getComponentType());
        size = array.size;
        System.arraycopy(array.items, 0, items, 0, size);
    }

    /** @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     *           memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     * @param arrayType Used for reification of the array type at runtime. */
    @SuppressWarnings("unchecked")
    public Array (boolean ordered, int capacity, Class arrayType) {
        this.ordered = ordered;
        items = (T[])java.lang.reflect.Array.newInstance(arrayType, capacity);
    }

    public void add (T value) {
        T[] items = this.items;
        if (size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size++] = value;
    }

    public void add (T value1, T value2) {
        T[] items = this.items;
        if (size + 1 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size] = value1;
        items[size + 1] = value2;
        size += 2;
    }

    public void add (T value1, T value2, T value3) {
        T[] items = this.items;
        if (size + 2 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size] = value1;
        items[size + 1] = value2;
        items[size + 2] = value3;
        size += 3;
    }

    public void add (T value1, T value2, T value3, T value4) {
        T[] items = this.items;
        if (size + 3 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size] = value1;
        items[size + 1] = value2;
        items[size + 2] = value3;
        items[size + 3] = value4;
        size += 4;
    }

    public void addAll (Array<? extends T> array) {
        addAll(array.items, 0, array.size);
    }

    public void addAll (Array<? extends T> array, int start, int count) {
        if (start + count > array.size)
            throw new IllegalArgumentException("start + count must be <= size: " + start + " + " + count + " <= " + array.size);
        addAll(array.items, start, count);
    }

    public void addAll (T... array) {
        addAll(array, 0, array.length);
    }

    public void addAll (T[] array, int start, int count) {
        T[] items = this.items;
        int sizeNeeded = size + count;
        if (sizeNeeded > items.length) items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));
        System.arraycopy(array, start, items, size, count);
        size = sizeNeeded;
    }

    public T get (int index) {
        if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        return items[index];
    }

    public void set (int index, T value) {
        if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] = value;
    }

    public void insert (int index, T value) {
        if (index > size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
        T[] items = this.items;
        if (size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        if (ordered)
            System.arraycopy(items, index, items, index + 1, size - index);
        else
            items[size] = items[index];
        size++;
        items[index] = value;
    }

    public boolean contains (T value, boolean identity) {
        T[] items = this.items;
        int i = size - 1;
        if (identity || value == null) {
            while (i >= 0)
                if (items[i--] == value) return true;
        } else {
            while (i >= 0)
                if (value.equals(items[i--])) return true;
        }
        return false;
    }

    public int indexOf (T value, boolean identity) {
        T[] items = this.items;
        if (identity || value == null) {
            for (int i = 0, n = size; i < n; i++)
                if (items[i] == value) return i;
        } else {
            for (int i = 0, n = size; i < n; i++)
                if (value.equals(items[i])) return i;
        }
        return -1;
    }

    public boolean removeValue (T value, boolean identity) {
        T[] items = this.items;
        if (identity || value == null) {
            for (int i = 0, n = size; i < n; i++) {
                if (items[i] == value) {
                    removeIndex(i);
                    return true;
                }
            }
        } else {
            for (int i = 0, n = size; i < n; i++) {
                if (value.equals(items[i])) {
                    removeIndex(i);
                    return true;
                }
            }
        }
        return false;
    }

    public T removeIndex (int index) {
        if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        T[] items = this.items;
        T value = items[index];
        size--;
        if (ordered)
            System.arraycopy(items, index + 1, items, index, size - index);
        else
            items[index] = items[size];
        items[size] = null;
        return value;
    }

    public T pop () {
        if (size == 0) throw new IllegalStateException("Array is empty.");
        --size;
        T item = items[size];
        items[size] = null;
        return item;
    }

    public T peek () {
        if (size == 0) throw new IllegalStateException("Array is empty.");
        return items[size - 1];
    }

    public T first () {
        if (size == 0) throw new IllegalStateException("Array is empty.");
        return items[0];
    }

    public boolean notEmpty () {
        return size > 0;
    }

    public boolean isEmpty () {
        return size == 0;
    }

    public void clear () {
        T[] items = this.items;
        for (int i = 0, n = size; i < n; i++)
            items[i] = null;
        size = 0;
    }

    /** Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     * @return {@link #items} */
    @SuppressWarnings("unchecked")
    public T[] shrink () {
        if (items.length != size) resize(size);
        return items;
    }

    /** Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     * @return {@link #items} */
    @SuppressWarnings("unchecked")
    public T[] ensureCapacity (int additionalCapacity) {
        if (additionalCapacity < 0) throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
        int sizeNeeded = size + additionalCapacity;
        if (sizeNeeded > items.length) resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));
        return items;
    }

    /** Sets the array size, leaving any values beyond the current size undefined.
     * @return {@link #items} */
    @SuppressWarnings("unchecked")
    public T[] setSize (int newSize) {
        truncate(newSize);
        if (newSize > items.length) resize(Math.max(8, newSize));
        size = newSize;
        return items;
    }

    @SuppressWarnings("unchecked")
    protected T[] resize (int newSize) {
        T[] items = this.items;
        T[] newItems = (T[])java.lang.reflect.Array.newInstance(items.getClass().getComponentType(), newSize);
        System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    public void sort () {
        java.util.Arrays.sort(items, 0, size);
    }

    public void reverse () {
        T[] items = this.items;
        for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
            int ii = lastIndex - i;
            T temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void shuffle () {
        T[] items = this.items;
        for (int i = size - 1; i >= 0; i--) {
            int ii = (int)(Math.random() * (i + 1));
            T temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    /** Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
     * taken. */
    public void truncate (int newSize) {
        if (size <= newSize) return;
        for (int i = newSize; i < size; i++)
            items[i] = null;
        size = newSize;
    }

    /** Returns a random item from the array, or null if the array is empty. */
    public T random () {
        if (size == 0) return null;
        return items[(int)(Math.random() * size)];
    }

    /** Returns the items as an array. Note the array is typed, so the {@link #Array(Class)} constructor must have been used.
     * Otherwise use {@link #toArray(Class)} to specify the array type. */
    public T[] toArray () {
        return toArray(items.getClass().getComponentType());
    }

    @SuppressWarnings("unchecked")
    public <V> V[] toArray (Class type) {
        V[] result = (V[])java.lang.reflect.Array.newInstance(type, size);
        System.arraycopy(items, 0, result, 0, size);
        return result;
    }

    public int hashCode () {
        if (!ordered) return super.hashCode();
        Object[] items = this.items;
        int h = 1;
        for (int i = 0, n = size; i < n; i++) {
            h *= 31;
            Object item = items[i];
            if (item != null) h += item.hashCode();
        }
        return h;
    }

    public boolean equals (Object object) {
        if (object == this) return true;
        if (!ordered) return false;
        if (!(object instanceof Array)) return false;
        Array array = (Array)object;
        if (!array.ordered) return false;
        int n = size;
        if (n != array.size) return false;
        Object[] items1 = this.items, items2 = array.items;
        for (int i = 0; i < n; i++) {
            Object o1 = items1[i], o2 = items2[i];
            if (!(o1 == null ? o2 == null : o1.equals(o2))) return false;
        }
        return true;
    }

    public String toString () {
        if (size == 0) return "[]";
        T[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(items[0]);
        for (int i = 1; i < size; i++) {
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    public String toString (String separator) {
        if (size == 0) return "";
        T[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append(items[0]);
        for (int i = 1; i < size; i++) {
            buffer.append(separator);
            buffer.append(items[i]);
        }
        return buffer.toString();
    }

    /** @see #Array(Class) */
    static public <T> Array<T> of (Class<T> arrayType) {
        return new Array<T>(arrayType);
    }

    /** @see #Array(boolean, int, Class) */
    static public <T> Array<T> of (boolean ordered, int capacity, Class<T> arrayType) {
        return new Array<T>(ordered, capacity, arrayType);
    }

    /** @see #Array(Object[]) */
    static public <T> Array<T> with (T... array) {
        return new Array(array);
    }

    public Iterator<T> iterator () {
        if (iterable == null) iterable = new ArrayIterable(this);
        return iterable.iterator();
    }

    static public class ArrayIterable<T> implements Iterable<T> {
        private ArrayIterator iterator1, iterator2;
        private Array<T> array;

        public ArrayIterable (Array<T> array) {
            this.array = array;
        }

        public Iterator<T> iterator () {
            if (iterator1 == null) {
                iterator1 = new ArrayIterator(array);
                iterator2 = new ArrayIterator(array);
            }
            if (!iterator1.valid) {
                iterator1.index = 0;
                iterator1.valid = true;
                iterator2.valid = false;
                return iterator1;
            }
            iterator2.index = 0;
            iterator2.valid = true;
            iterator1.valid = false;
            return iterator2;
        }
    }

    static public class ArrayIterator<T> implements Iterator<T> {
        private final Array<T> array;
        int index;
        boolean valid = true;

        public ArrayIterator (Array<T> array) {
            this.array = array;
        }

        public boolean hasNext () {
            if (!valid) throw new RuntimeException("#iterator() cannot be used nested.");
            return index < array.size;
        }

        public T next () {
            if (index >= array.size) throw new RuntimeException("No more elements.");
            if (!valid) throw new RuntimeException("#iterator() cannot be used nested.");
            return array.items[index++];
        }

        public void remove () {
            index--;
            array.removeIndex(index);
        }
    }

    /** Creates a new array with {@link #items} of the specified type.
     * @param arrayType Use int.class, etc for primitive types. */
    @SuppressWarnings("unchecked")
    public Array (Class arrayType) {
        this(true, 16, arrayType);
    }

    /** @param array May be null. */
    @SuppressWarnings("unchecked")
    public Array (T[] array) {
        this(true, array, 0, array.length);
    }

    /** @param array May be null. */
    @SuppressWarnings("unchecked")
    public Array (boolean ordered, T[] array, int startIndex, int count) {
        this(ordered, count, array.getClass().getComponentType());
        size = count;
        System.arraycopy(array, startIndex, items, 0, count);
    }
}

class Predicate<T> {
    static public class PredicateIterable<T> implements Iterable<T> {
        private final Array<T> array;
        private final Predicate<T> predicate;
        
        public PredicateIterable(Array<T> array, Predicate<T> predicate) {
            this.array = array;
            this.predicate = predicate;
        }
        
        public Iterator<T> iterator () {
            return new PredicateIterator<T>(array, predicate);
        }
    }
    
    static public class PredicateIterator<T> implements Iterator<T> {
        private final Array<T> array;
        private final Predicate<T> predicate;
        private int index = 0;
        private T next;
        private boolean hasNext;
        
        public PredicateIterator(Array<T> array, Predicate<T> predicate) {
            this.array = array;
            this.predicate = predicate;
            findNext();
        }
        
        private void findNext() {
            hasNext = false;
            while (index < array.size) {
                T item = array.get(index++);
                if (predicate.evaluate(item)) {
                    next = item;
                    hasNext = true;
                    break;
                }
            }
        }
        
        public boolean hasNext() {
            return hasNext;
        }
        
        public T next() {
            if (!hasNext) throw new RuntimeException("No more elements.");
            T result = next;
            findNext();
            return result;
        }
        
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported");
        }
    }
    
    public boolean evaluate(T object) {
        return false; // Override in subclasses
    }
}
