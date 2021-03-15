package com.langtoun.timers;

import static com.langtoun.timers.Timer.newTimer;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Hello world!
 *
 */
public class App {
  public static void main(String[] args) {
    System.out.println("Timers\n------");

    final Timer timer = newTimer();

    runTimerScenario(timer, "scenario 1: start - stop", t -> {
      runAfterDelay(t, 0, Timer::start);
      runAfterDelay(t, 50, Timer::stop);
    });

    runTimerScenario(timer, "scenario 2: start - split - stop", t -> {
      runAfterDelay(t, 0, Timer::start);
      runAfterDelay(t, 50, Timer::split);
      runAfterDelay(t, 50, Timer::stop);
    });

    runTimerScenario(timer, "scenario 3: start - pause - resume - stop", t -> {
      runAfterDelay(t, 0, Timer::start);
      runAfterDelay(t, 50, Timer::pause);
      runAfterDelay(t, 50, Timer::resume);
      runAfterDelay(t, 50, Timer::stop);
    });

    runTimerScenario(timer, "scenario 4: start - pause - stop", t -> {
      runAfterDelay(t, 0, Timer::start);
      runAfterDelay(t, 50, Timer::pause);
      runAfterDelay(t, 50, Timer::stop);
    });

    runTimerScenario(timer, "scenario 5: start - split - pause - resume - stop", t -> {
      runAfterDelay(t, 0, Timer::start);
      runAfterDelay(t, 50, Timer::split);
      runAfterDelay(t, 50, Timer::pause);
      runAfterDelay(t, 50, Timer::resume);
      runAfterDelay(t, 50, Timer::stop);
    });

    runTimerScenario(timer, "scenario 6: start - split - pause - stop", t -> {
      runAfterDelay(t, 0, Timer::start);
      runAfterDelay(t, 50, Timer::split);
      runAfterDelay(t, 50, Timer::pause);
      runAfterDelay(t, 50, Timer::stop);
    });

    runTimerScenario(timer, "scenario 7: start - split - pause - resume - pause - resume - split - pause - resume - stop", t -> {
      runAfterDelay(t, 0, Timer::start);
      runAfterDelay(t, 50, Timer::split);
      runAfterDelay(t, 50, Timer::pause);
      runAfterDelay(t, 50, Timer::resume);
      runAfterDelay(t, 50, Timer::pause);
      runAfterDelay(t, 50, Timer::resume);
      runAfterDelay(t, 50, Timer::split);
      runAfterDelay(t, 50, Timer::pause);
      runAfterDelay(t, 50, Timer::resume);
      runAfterDelay(t, 50, Timer::stop);
    });
  }

  private static void runAfterDelay(final Timer timer, final long millis, final Consumer<Timer> action) {
    if (millis > 0) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {}
    }
    action.accept(timer);
  }

  private static void runTimerScenario(final Timer timer, final String title, final Consumer<Timer> scenario) {
    System.out.println(title);
    System.out.println();

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
    System.out.println();
  }

}
