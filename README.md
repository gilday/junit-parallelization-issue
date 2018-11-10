# junit-parallelization-issue

While working with parallel test execution in JUnit 5.3, I came across a mystery
that I have not yet solved wherein I can trick JUnit into running more
concurrent tests than it is configured to run as a side-effect of sleeping a
thread inside of a `CompletableFuture` 🤯

I do not yet know if this is just my failure to understand Java concurrency, a
problem with `CompletableFuture`, or a problem with JUnit 5.3. This project is a
demonstration of the issue for me to explore.

## Building

Use maven to run the tests. When the tests are configured to do nothing, the
test suite passes

    mvn test -Dparallel-test.nop=true

But when the tests use their default behavior which is to sleep in a
`CompletableFuture`, the test suite fails.

   mvn test

# The Issue

In this example, 10 tests are run concurrently. JUnit 5 is configured to run 4
concurrent tests. The tests' `beforeEach` and `afterEach` logic increments and
decrements a counter respectively. This counter keeps track of its maximum
value, so after all tests in the suite have completed, we expect the maximum
value of the counter to be equal to the number of tests run concurrently;
however, when the tests sleep asynchronously in a `CompletableFuture` and block
until the sleep operation is complete, JUnit is tricked into running twice as
many tests concurrently. This behavior may be observed using the aforementioned
counter: its maximum value is 8 when the tests sleep in a `CompletableFuture`.

By default, `CompletableFuture.runAsync` uses the common `ForkJoinPool` which I
believe is also what JUnit 5 uses. I thought maybe sharing the common
`ForkJoinPool` was the source of the issue, so I create a new `ExecutorService`
for each test to eliminate any hypothetical problems from sharing a thread pool.
