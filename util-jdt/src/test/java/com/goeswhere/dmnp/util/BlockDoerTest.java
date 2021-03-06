package com.goeswhere.dmnp.util;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockDoerTest {
    private static final int REPETITIONS = 500;

    private final class SaveString implements Runnable {
        private final Set<String> ret;
        private final String msg;

        private SaveString(Set<String> ret, String msg) {
            this.ret = ret;
            this.msg = msg;
        }

        @Override
        public void run() {
            ret.add(msg);
            try {
                Thread.sleep(r.nextInt(2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private final Random r = new Random();

    @Test
    void test() throws InterruptedException {
        for (int i = 0; i < REPETITIONS; ++i) {
            final Set<String> ret = Collections.newSetFromMap(new ConcurrentHashMap<>());
            Lock l = new ReentrantReadWriteLock().writeLock();
            try (BlockDoer d = BlockDoer.start(l)) {
                d.offer(new SaveString(ret, "here"));
                Thread.sleep(r.nextInt(2));
                d.offer(new SaveString(ret, "there"));
                Thread.sleep(r.nextInt(2));
                d.offer(new SaveString(ret, "everywhere"));
            }

            assertEquals(ImmutableSet.of("here", "there", "everywhere"), ret);
        }
    }

    @Test
    void testExecutor() throws InterruptedException {
        for (int i = 0; i < REPETITIONS; ++i) {
            final Set<String> ret = Collections.newSetFromMap(new ConcurrentHashMap<>());
            Lock l = new ReentrantReadWriteLock().writeLock();
            final ExecutorService d = BlockDoerExecutorService.create(l);
            d.execute(new SaveString(ret, "here"));
            Thread.sleep(r.nextInt(2));
            d.execute(new SaveString(ret, "there"));
            Thread.sleep(r.nextInt(2));
            d.execute(new SaveString(ret, "everywhere"));
            d.shutdown();
            d.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            assertEquals(ImmutableSet.of("here", "there", "everywhere"), ret);
        }
    }
}
