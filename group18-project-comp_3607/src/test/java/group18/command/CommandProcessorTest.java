package group18.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CommandProcessor runs commands one at a time, in order, off the caller's thread")
class CommandProcessorTest {

    private final CommandProcessor processor = CommandProcessor.singleThreaded();

    @AfterEach
    void tearDown() {
        processor.shutdown(Duration.ofSeconds(5));
    }

    @Test
    void commandsRunInTheOrderTheyWereSubmitted() {
        List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = IntStream.range(0, 200)
                .mapToObj(i -> processor.submit(() -> order.add(i))).toList();
        futures.forEach(CompletableFuture::join);
        assertEquals(IntStream.range(0, 200).boxed().toList(), order);
    }

    @Test
    void commandsRunOnABackgroundDaemonThread() {
        Thread[] runner = new Thread[1];
        processor.submit(() -> runner[0] = Thread.currentThread()).join();
        assertFalse(runner[0] == Thread.currentThread());
        assertTrue(runner[0].isDaemon());
        assertEquals("telemetry-commands", runner[0].getName());
    }

    @Test
    void aFailingCommandIsReportedAndLaterCommandsStillRun() {
        Logger.getLogger(CommandProcessor.class.getName()).setLevel(Level.OFF);
        CompletableFuture<Void> failing = processor.submit(() -> {
            throw new IOException("disk full");
        });
        CompletableFuture<Void> next = processor.submit(() -> { });
        CompletionException thrown = assertThrows(CompletionException.class, failing::join);
        assertInstanceOf(IOException.class, thrown.getCause());
        next.join();
        assertTrue(next.isDone() && !next.isCompletedExceptionally());
    }

    @Test
    void anErrorCompletesTheFutureExceptionally() {
        CompletableFuture<Void> failing = processor.submit(() -> {
            throw new AssertionError("broken");
        });
        assertInstanceOf(AssertionError.class, assertThrows(CompletionException.class, failing::join).getCause());
        processor.submit(() -> { }).join();
    }

    @Test
    void shutdownWaitsForQueuedCommandsThenRejectsNewOnes() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        List<String> done = Collections.synchronizedList(new ArrayList<>());
        processor.submit(() -> release.await(5, TimeUnit.SECONDS));
        processor.submit(() -> done.add("queued"));
        release.countDown();
        assertTrue(processor.shutdown(Duration.ofSeconds(5)));
        assertEquals(List.of("queued"), done);
        CompletableFuture<Void> late = processor.submit(() -> done.add("late"));
        assertInstanceOf(RejectedExecutionException.class,
                assertThrows(CompletionException.class, late::join).getCause());
    }

    @Test
    void shutdownReportsATimeout() {
        CountDownLatch release = new CountDownLatch(1);
        processor.submit(() -> release.await(5, TimeUnit.SECONDS));
        assertFalse(processor.shutdown(Duration.ofMillis(50)));
        release.countDown();
    }

    @Test
    void nullCommandsAreRejected() {
        assertThrows(NullPointerException.class, () -> processor.submit(null));
    }
}
