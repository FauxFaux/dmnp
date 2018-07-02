package com.goeswhere.dmnp.util;

import com.google.common.collect.Lists;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;

/**
 * When we get hold of a lock, do all the pending work, then release it.
 */
public class BlockDoer extends Thread implements Closeable {
    private final Lock wl;
    final BlockingQueue<Runnable> queue;

    private static class Shutdown extends RuntimeException {
        // nothing at all
    }

    private final static Runnable shutdown = new Runnable() {
        @Override
        public void run() {
            throw new Shutdown();
        }
    };

    public BlockDoer(Lock wl, BlockingQueue<Runnable> queue) {
        this.wl = wl;
        this.queue = queue;
    }

    public BlockDoer(Lock wl) {
        this(wl, new LinkedBlockingQueue<>());
    }

    @Override
    public void run() {
        try {
            while (true) {
                final Runnable first = queue.take();

                wl.lock();
                try {
                    if (null != first)
                        first.run();

                    final List<Runnable> l = Lists.newArrayListWithExpectedSize(queue.size());
                    queue.drainTo(l);

                    for (Runnable wr : l)
                        wr.run();
                } finally {
                    wl.unlock();
                }
            }
        } catch (InterruptedException ignored) {
            // assume cancelled
        } catch (Shutdown ignored) {
            // by command
        }
    }

    public static BlockDoer start(Lock lock) {
        final BlockDoer bd = new BlockDoer(lock);
        bd.start();
        return bd;
    }

    /**
     * As {@link BlockingQueue#offer(Object)}.
     */
    BlockDoer offer(Runnable r) {
        if (!isAlive())
            throw new RejectedExecutionException("Thread's dead");

        queue.offer(r);
        return this;
    }

    /**
     * Passed queue will have been emptied by return.
     */
    @Override
    public void close() {
        shutdown();
        try {
            join();
        } catch (InterruptedException e) {
            interrupt();
        }
    }

    void shutdown() {
        offer(shutdown);
    }

    List<Runnable> getPending() {
        final List<Runnable> ret = Lists.newArrayListWithExpectedSize(queue.size());
        queue.drainTo(ret);
        return ret;
    }
}