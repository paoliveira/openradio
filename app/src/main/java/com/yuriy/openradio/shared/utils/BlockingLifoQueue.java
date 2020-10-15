/*
 * Copyright 2020 The "Open Radio" Project. Author: Chernyshov Yuriy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuriy.openradio.shared.utils;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public final class BlockingLifoQueue<T> implements BlockingQueue<T> {

    /**
     * Add and remove only from the end of the queue.
     */
    private final BlockingDeque<T> mDeque;

    BlockingLifoQueue() {
        super();
        mDeque = new LinkedBlockingDeque<>();
    }

    public boolean add(final T e) {
        mDeque.addLast(e);
        return true;
    }

    public boolean contains(final Object o) {
        return mDeque.contains(o);
    }

    public int drainTo(final Collection<? super T> c) {
        return mDeque.drainTo(c);
    }

    public int drainTo(final Collection<? super T> c, final int maxElements) {
        return mDeque.drainTo(c, maxElements);
    }

    public boolean offer(final T e) {
        return mDeque.offerLast(e);
    }

    public boolean offer(final T e,
                         final long timeout,
                         final TimeUnit unit) throws InterruptedException {
        return mDeque.offerLast(e, timeout, unit);
    }

    public T poll(final long timeout, final TimeUnit unit) throws InterruptedException {
        return mDeque.pollLast(timeout, unit);
    }

    public void put(final T e) throws InterruptedException {
        mDeque.putLast(e);
    }

    public int remainingCapacity() {
        return mDeque.size();
    }

    public boolean remove(final Object o) {
        return mDeque.remove(o);
    }

    public T take() throws InterruptedException {
        return mDeque.takeLast();
    }

    public T element() {
        if (mDeque.isEmpty()) {
            throw new NoSuchElementException("empty stack");
        }

        return mDeque.pollLast();
    }

    public T peek() {
        return mDeque.peekLast();
    }

    public T poll() {
        return mDeque.pollLast();
    }

    public T remove() {
        if (mDeque.isEmpty()) {
            throw new NoSuchElementException("empty stack");
        }
        return mDeque.pollLast();
    }

    public boolean addAll(@NonNull final Collection<? extends T> c) {
        mDeque.addAll(c);
        return true;
    }

    public void clear() {
        mDeque.clear();
    }

    public boolean containsAll(@NonNull final Collection<?> c) {
        return mDeque.containsAll(c);
    }

    public boolean isEmpty() {
        return mDeque.isEmpty();
    }

    public Iterator<T> iterator() {
        return mDeque.descendingIterator();
    }

    public boolean removeAll(@NonNull final Collection<?> c) {
        return mDeque.removeAll(c);
    }

    public boolean retainAll(@NonNull final Collection<?> c) {
        return mDeque.retainAll(c);
    }

    public int size() {
        return mDeque.size();
    }

    public Object[] toArray() {
        return mDeque.toArray();
    }

    public <P> P[] toArray(@NonNull final P[] a) {
        return mDeque.toArray(a);
    }
}
