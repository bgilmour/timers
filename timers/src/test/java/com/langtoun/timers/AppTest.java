package com.langtoun.timers;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for NanoTimer App.
 */
public class AppTest extends TestCase {
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public AppTest(String testName) {
    super(testName);
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite() {
    return new TestSuite(AppTest.class);
  }

  /**
   * Scenario 1
   */
  public void testScenario1() {
    final NanoTimer timer = NanoTimers.createTimer("scenario 1: start - stop");
    runTimerScenario(timer, t -> {
      runAfterDelay(t, 0, NanoTimer::start);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::stop);
      assertEquals(NanoTimerState.STOPPED, t.getTimerState());
    });
  }

  /**
   * Scenario 2
   */
  public void testScenario2() {
    final NanoTimer timer = NanoTimers.createTimer("scenario 2: start - split(split1) - stop");
    runTimerScenario(timer, t -> {
      runAfterDelay(t, 0, NanoTimer::start);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, t1 -> t1.split("split1"));
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::stop);
      assertEquals(NanoTimerState.STOPPED, t.getTimerState());
    });
  }

  /**
   * Scenario 3
   */
  public void testScenario3() {
    final NanoTimer timer = NanoTimers.createTimer("scenario 3: start - pause - resume - stop");
    runTimerScenario(timer, t -> {
      runAfterDelay(t, 0, NanoTimer::start);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::pause);
      assertEquals(NanoTimerState.PAUSED, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::resume);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::stop);
      assertEquals(NanoTimerState.STOPPED, t.getTimerState());
    });
  }

  /**
   * Scenario 4
   */
  public void testScenario4() {
    final NanoTimer timer = NanoTimers.createTimer("scenario 4: start - pause - stop");
    runTimerScenario(timer, t -> {
      runAfterDelay(t, 0, NanoTimer::start);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::pause);
      assertEquals(NanoTimerState.PAUSED, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::stop);
      assertEquals(NanoTimerState.STOPPED, t.getTimerState());
    });
  }

  /**
   * Scenario 5
   */
  public void testScenario5() {
    final NanoTimer timer = NanoTimers.createTimer("scenario 5: start - split(split1) - pause - resume - stop");
    runTimerScenario(timer, t -> {
      runAfterDelay(t, 0, NanoTimer::start);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, t1 -> t1.split("split1"));
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::pause);
      assertEquals(NanoTimerState.PAUSED, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::resume);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::stop);
      assertEquals(NanoTimerState.STOPPED, t.getTimerState());
    });
  }

  /**
   * Scenario 6
   */
  public void testScenario6() {
    final NanoTimer timer = NanoTimers.createTimer("scenario 6: start - split(split1) - pause - stop");
    runTimerScenario(timer, t -> {
      runAfterDelay(t, 0, NanoTimer::start);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, t1 -> t1.split("split1"));
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::pause);
      assertEquals(NanoTimerState.PAUSED, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::stop);
      assertEquals(NanoTimerState.STOPPED, t.getTimerState());
    });
  }

  /**
   * Scenario 7
   */
  public void testScenario7() {
    final NanoTimer timer = NanoTimers.createTimer("scenario 7: start - split(split1) - pause - resume - pause - resume - split(split2) - pause - resume - stop");
    runTimerScenario(timer, t -> {
      runAfterDelay(t, 0, NanoTimer::start);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, t1 -> t1.split("split1"));
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::pause);
      assertEquals(NanoTimerState.PAUSED, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::resume);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::pause);
      assertEquals(NanoTimerState.PAUSED, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::resume);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, t1 -> t1.split("split2"));
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::pause);
      assertEquals(NanoTimerState.PAUSED, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::resume);
      assertEquals(NanoTimerState.RUNNING, t.getTimerState());
      runAfterDelay(t, 50, NanoTimer::stop);
      assertEquals(NanoTimerState.STOPPED, t.getTimerState());
    });
  }

  private static void runAfterDelay(final NanoTimer timer, final long millis, final Consumer<NanoTimer> action) {
    try {
      TimeUnit.MILLISECONDS.sleep(millis);
    } catch (InterruptedException e) {}
    action.accept(timer);
  }

  private static void runTimerScenario(final NanoTimer timer, final Consumer<NanoTimer> scenario) {
    timer.reset();
    scenario.accept(timer);

    System.out.printf("timer => %s\n\n", timer);

    System.out.printf("elapsed   : %dus\n", timer.elapsedTime(TimeUnit.MICROSECONDS));
    int i = 0;
    for (long splitTime : timer.splitTimes(TimeUnit.MICROSECONDS)) {
      System.out.printf("split[%d]  : %dus\n", i++, splitTime);
    }
    i = 0;
    for (long splitPeriod : timer.splitPeriods(TimeUnit.MICROSECONDS)) {
      System.out.printf("period[%d] : %dus\n", i++, splitPeriod);
    }
    System.out.println("\n");
  }

}
