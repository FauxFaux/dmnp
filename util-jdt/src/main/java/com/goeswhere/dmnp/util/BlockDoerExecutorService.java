package com.goeswhere.dmnp.util;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * Single-threaded executor.
 * <p>
 * Lock will be held during execution, but will not unlock and re-lock between immediate re-uses.
 */
class BlockDoerExecutorService extends AbstractExecutorService {

    private final BlockDoer doer;
    private final AtomicBoolean shutdown = new AtomicBoolean();

    private BlockDoerExecutorService(Lock l) {
        doer = new BlockDoer(l);
    }

    static BlockDoerExecutorService create(Lock l) {
        final BlockDoerExecutorService exec = new BlockDoerExecutorService(l);
        exec.doer.start();
        return exec;
    }

    @Override
    public synchronized void shutdown() {
        if (!shutdown.getAndSet(true)) {
            doer.shutdown();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        doer.interrupt();
        return doer.getPending();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    @Override
    public boolean isTerminated() {
        return !doer.isAlive();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        if (!isShutdown())
            throw new IllegalStateException();

        doer.join(unit.toMillis(timeout));
        return !isTerminated();
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown())
            throw new RejectedExecutionException();
        doer.offer(command);
    }
}
