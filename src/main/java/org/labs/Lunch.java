package org.labs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.logging.Level.INFO;
import static java.util.stream.Stream.of;
import static org.labs.ProgrammerState.*;

public class Lunch implements Runnable {
  private final int programmersCount;
  final List<Thread> programmers;
  private final Semaphore waiters;
  private final AtomicInteger portionsCount;
  private final CountDownLatch latch;
  private final Map<Integer, ProgrammerState> states;
  private final Semaphore[] bothSpoonsAvailable;
  private final ReentrantLock lock = new ReentrantLock();
  final Map<Integer, Integer> portionsEatenByProgrammer;
  final AtomicInteger portionsEatenInTotal;

  public Lunch(int programmersCount, int portions) {
    this.programmersCount = programmersCount;
    programmers = new ArrayList<>();
    portionsEatenByProgrammer = new ConcurrentHashMap<>();
    states = new ConcurrentHashMap<>();
    bothSpoonsAvailable = new Semaphore[programmersCount];
    portionsEatenInTotal = new AtomicInteger(0);
    for (int i = 0; i < programmersCount; i++) {
      bothSpoonsAvailable[i] = new Semaphore(0);
      portionsEatenByProgrammer.put(i, 0);
      states.put(i, DISCUSS_LECTURERS);
    }
    latch = new CountDownLatch(programmersCount);
    waiters = new Semaphore(2);
    portionsCount = new AtomicInteger(portions - programmersCount);
  }

  @Override
  public void run() {
    approveLunchRules();
    var start = System.currentTimeMillis();
    for (var p : programmers) p.start();
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(String.format("Thread %s was interrupted", Thread.currentThread().getName()), e);
    }
    logger.log(INFO, String.format("Execution time: %s ms", System.currentTimeMillis() - start));
    logger.log(INFO, String.format("Portions count after lunch is over: %s", portionsCount.get()));
    logger.log(INFO, "Distribution " + portionsEatenByProgrammer);
  }

  private void approveLunchRules() {
    var programmersCount = states.size();
    for (int i = 0; i < programmersCount; i++) {
      int programmerId = i;
      programmers.add(new Thread(() -> {
        try {
          lunchBehavior(programmerId);
          latch.countDown();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(String.format("Thread %s was interrupted", Thread.currentThread().getName()), e);
        }
      }));
    }
  }

  private void lunchBehavior(int programmerId) throws InterruptedException {
    while (true) {
      acquireSpoons(programmerId);
      // eating (can be replaced with Thread.sleep(TIME_TO_EAT))
      releaseSpoons(programmerId);
      // discuss lecturers (can be replaced with Thread.sleep(TIME_TO_DISCUSS))
      waiters.acquire();
      var isPortionAvailable = orderNewPortion();
      waiters.release();
      if (!isPortionAvailable) {
        break;
      }
    }
  }

  private void acquireSpoons(int programmerId) throws InterruptedException {
    lock.lock();
    try {
      states.put(programmerId, HUNGRY);
      requestSpoonsAccess(programmerId); // not blocked
    } finally {
      lock.unlock();
    }
    bothSpoonsAvailable[programmerId].acquire(); // blocked if spoons are unavailable
    portionsEatenByProgrammer.computeIfPresent(programmerId, (key, oldBill) -> oldBill + 1);
    portionsEatenInTotal.incrementAndGet();
  }

  private void requestSpoonsAccess(int requesterId) {
    if (
      states.get(requesterId) == HUNGRY &&
      states.get(left(requesterId)) != EATING &&
      states.get(right(requesterId)) != EATING
    ) {
      states.put(requesterId, EATING);
      bothSpoonsAvailable[requesterId].release();
    }
  }

  private void releaseSpoons(int programmerId) {
    lock.lock();
    try {
      states.put(programmerId, DISCUSS_LECTURERS);
      requestSpoonsAccess(left(programmerId));
      requestSpoonsAccess(right(programmerId));
    } finally {
      lock.unlock();
    }
  }

  private boolean orderNewPortion() {
    while (true) {
      // CAS loop to ensure the condition of one thread in a critical section (portions decrement)
      var oldPortionsCount = portionsCount.get();
      if (oldPortionsCount == 0) {
        return false;
      }
      if (!portionsCount.compareAndSet(oldPortionsCount, oldPortionsCount - 1)) {
        continue;
      }
      return true;
    }
  }

  private int left(int i) {
    return (i - 1 + programmersCount) % programmersCount;
  }

  private int right(int i) {
    return (i + 1) % programmersCount;
  }

  static final Logger logger = Logger.getLogger("Lunch");
  private static final String PROGRAMMER_EATING_MESSAGE = "Programmer %s is eating";
  private static final String SPOONS_RELEASED_MESSAGE = "Programmer %s released spoons";

}
