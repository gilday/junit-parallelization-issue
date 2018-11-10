package com.github.gilday;

/** synchronized counter which keeps track of the highest value recorded */
final class HighWaterMarkCounter {

  private int count;
  private int max;

  synchronized void increment() {
    count++;
    if (count > max) {
      max = count;
    }
  }

  synchronized void decrement() {
    count--;
  }

  /** @return maximum observed value of the counter */
  synchronized int max() {
    return max;
  }
}
