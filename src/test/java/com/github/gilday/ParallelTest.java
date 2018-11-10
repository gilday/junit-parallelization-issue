package com.github.gilday;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;

/**
 * Test which replicates an issue I am having while using {@link CompletableFuture} in a JUnit 5
 * parallel test. This test is repeated several times concurrently, and before each test a {@link
 * HighWaterMarkCounter} counter is incremented and after each test the {@link HighWaterMarkCounter}
 * counter is decremented. And the end of all tests, we expect the {@link
 * HighWaterMarkCounter#max()} to equal the number of threads with which JUnit is executing these
 * tests; however, when a thread sleeps in a {@link CompletableFuture} twice as many tests are run
 * concurrently as one would expect
 */
final class ParallelTest {

  /** for tracking the maximum number of tests running concurrently */
  private static final HighWaterMarkCounter counter = new HighWaterMarkCounter();

  @Nested
  final class RepeatedInnerTest {

    @BeforeEach
    void before() {
      counter.increment();
    }

    @RepeatedTest(10)
    void repeat() {
      // if property "parallel-test.nop" is set, return immediately
      final boolean nop = Boolean.parseBoolean(System.getProperty("parallel-test.nop"));
      if (nop) {
        return;
      }

      // sleeping in a CompletableFuture running with its own ExecutorService will trick JUnit into
      // running another test concurrently as a side-effect 🤯

      // sleep in a CompletableFuture executed on another thread and block immediately until that
      // operation has completed
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      CompletableFuture.runAsync(
              () -> {
                try {
                  Thread.sleep(250);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              },
              executor)
          .join();
      executor.shutdown();
    }

    @AfterEach
    void after() {
      counter.decrement();
    }
  }

  /**
   * Fail if the maximum number of concurrent nested tests is greater than the configured
   * "junit.parallelism" setting
   */
  @AfterAll
  static void afterAll() {
    final int parallelism = Integer.parseInt(System.getProperty("junit.parallelism"));
    assertTrue(
        parallelism >= counter.max(),
        "Expected at most "
            + parallelism
            + " concurrent tests, but observed "
            + counter.max()
            + " concurrent tests");
  }
}
