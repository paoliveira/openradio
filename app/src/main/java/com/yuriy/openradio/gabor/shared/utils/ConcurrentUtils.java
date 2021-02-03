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

package com.yuriy.openradio.gabor.shared.utils;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConcurrentUtils {

    private ConcurrentUtils() { }

    /**
     * An {@link Executor} that can be used to execute image related tasks in parallel.
     */
    static final ThreadPoolExecutor IMAGE_WORKER_EXECUTOR;

    /**
     * Executor of the API requests.
     */
    public static final ExecutorService API_CALL_EXECUTOR = Executors.newCachedThreadPool();

    private static final int CORE_POOL_SIZE = 5;
    private static final int MAXIMUM_POOL_SIZE = 125;
    private static final int KEEP_ALIVE_SECONDS = 3;
    private static final int MAXIMUM_QUEUE_SIZE = 120;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        @Override
        public Thread newThread(@NonNull final Runnable runnable) {
            return new Thread(runnable, "ORAsyncTask #" + mCount.getAndIncrement());
        }
    };

    static {
        IMAGE_WORKER_EXECUTOR = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new BlockingLifoQueue<>(), sThreadFactory
        );
    }

    static boolean isImageWorkerExecutorNotReady() {
        return IMAGE_WORKER_EXECUTOR.getQueue().size() >= MAXIMUM_QUEUE_SIZE
                || IMAGE_WORKER_EXECUTOR.getActiveCount() >= MAXIMUM_POOL_SIZE;
    }
}
