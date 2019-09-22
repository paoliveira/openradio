package com.yuriy.openradio.utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConcurrentUtils {

    private ConcurrentUtils() { }

    /**
     * An {@link Executor} that can be used to execute tasks in parallel.
     */
    public static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    /**
     * Executor of the API requests.
     */
    public static final ExecutorService API_CALL_EXECUTOR = Executors.newSingleThreadExecutor();

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 20;
    private static final int KEEP_ALIVE_SECONDS = 3;
    private static final int MAXIMUM_QUEUE_SIZE = 120;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(final Runnable runnable) {
            return new Thread(runnable, "ORAsyncTask #" + mCount.getAndIncrement());
        }
    };

    static {
        THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<>(), sThreadFactory
        );
    }

    public static boolean isThreadPoolFull() {
        return THREAD_POOL_EXECUTOR.getQueue().size() >= MAXIMUM_QUEUE_SIZE;
    }
}
