package com.github.jacekolszak.promises;

import static org.junit.Assert.*;
import static threadjiggler.core.JiggleStrategy.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import threadjiggler.test.Jiggle;
import threadjiggler.test.JigglingRunner;

@RunWith(JigglingRunner.class)
@Jiggle(value = "com.github.jacekolszak.promises.*", strategy = THREAD_SLEEP)
public class PromiseParallelSpec {

    private Object resolvedValue;

    @Test
    public void parallelPromiseResolutionNotPossible() throws InterruptedException {
        // given
        int threadsCount = (int) range().count();
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        AtomicInteger thenExecutionsCount = new AtomicInteger(0);
        CountDownLatch allThreadsReady = new CountDownLatch(threadsCount);
        CountDownLatch allThreadsExecuted = new CountDownLatch(threadsCount);

        // when
        new Promise<>(p ->
                range().
                        forEach(i -> executorService.submit(() -> {
                            try {
                                allThreadsReady.countDown();
                                allThreadsReady.await();
                                p.resolve(i);
                                allThreadsExecuted.countDown();
                            } catch (InterruptedException e) {
                                fail(e.getMessage());
                            }
                        }))).
                thenVoid(i -> {
                    thenExecutionsCount.incrementAndGet();
                    this.resolvedValue = i;
                });
        allThreadsExecuted.await();
        executorService.shutdown();

        // then
        assertEquals(1, thenExecutionsCount.get());
        assertTrue(range().anyMatch(i -> (Long) resolvedValue == i));
    }

    private LongStream range() {
        return LongStream.range(((long) Integer.MAX_VALUE) + 1, (long) Integer.MAX_VALUE + 5000);
    }

    @Test
    public void parallelPromiseRejectionNotPossible() throws InterruptedException {
        // given
        int threadsCount = (int) range().count();
        ExecutorService executorService = Executors.newFixedThreadPool(threadsCount);
        AtomicInteger thenExecutionsCount = new AtomicInteger(0);
        CountDownLatch allThreadsReady = new CountDownLatch(threadsCount);
        CountDownLatch allThreadsExecuted = new CountDownLatch(threadsCount);

        // when
        new Promise<>(p ->
                range().
                        forEach(i -> executorService.submit(() -> {
                            try {
                                allThreadsReady.countDown();
                                allThreadsReady.await();
                                p.reject(new Exception("" + i));
                                allThreadsExecuted.countDown();
                            } catch (InterruptedException e) {
                                fail(e.getMessage());
                            }
                        }))).
                catchVoid(i -> {
                    thenExecutionsCount.incrementAndGet();
                    this.resolvedValue = i;
                });
        allThreadsExecuted.await();
        executorService.shutdown();

        // then
        assertEquals(1, thenExecutionsCount.get());
        assertTrue(range().anyMatch(i -> Long.valueOf(((Throwable) resolvedValue).getMessage()) == i));
    }

}
