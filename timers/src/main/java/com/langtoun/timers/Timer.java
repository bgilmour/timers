package com.langtoun.timers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Nanosecond timer class supporting reset, start, split, pause, resume, and
 * stop actions.
 */
public class Timer {

  private List<List<Long>> splits;
  private List<TimerAction> timerActions;

  private TimerState timerState = TimerState.UNINITIALISED;

  private Timer() {
    // create using static method
  }

  public static Timer newTimer() {
    return new Timer();
  }

  public void start() {
    final long startTime = System.nanoTime();
    if (timerState == TimerState.UNINITIALISED) {
      splits = new ArrayList<>();
      timerActions = new ArrayList<>();
      final List<Long> actions = new ArrayList<>();
      actions.add(startTime);
      splits.add(actions);
      timerState = TimerState.RUNNING;
      timerActions.add(TimerAction.START);
    } else {
      throw new IllegalStateException("timer can only be started if uninitialised");
    }
  }

  public void split() {
    final long splitTime = System.nanoTime();
    if (timerState == TimerState.RUNNING) {
      final List<Long> actions = new ArrayList<>();
      actions.add(splitTime);
      splits.add(actions);
      timerActions.add(TimerAction.SPLIT);
    } else {
      throw new IllegalStateException("timer can only be split if running");
    }
  }

  public void pause() {
    final long pauseTime = System.nanoTime();
    if (timerState == TimerState.RUNNING) {
      final List<Long> actions = splits.get(splits.size() - 1);
      actions.add(pauseTime);
      timerState = TimerState.PAUSED;
      timerActions.add(TimerAction.PAUSE);
    } else if (timerState != TimerState.PAUSED) {
      throw new IllegalStateException("timer can only be paused if it is running or paused");
    }
  }

  public void resume() {
    final long resumeTime = System.nanoTime();
    if (timerState == TimerState.PAUSED) {
      final List<Long> actions = splits.get(splits.size() - 1);
      actions.add(resumeTime);
      timerState = TimerState.RUNNING;
      timerActions.add(TimerAction.RESUME);
    } else if (timerState != TimerState.RUNNING) {
      throw new IllegalStateException("timer can only be resumed if it is running or paused");
    }
  }

  public void stop() {
    final long stopTime = System.nanoTime();
    if (timerState == TimerState.PAUSED) {
      final List<Long> previousActions = splits.get(splits.size() - 1);
      previousActions.add(stopTime);
      timerActions.add(TimerAction.RESUME);
    }
    if (timerState == TimerState.PAUSED || timerState == TimerState.RUNNING) {
      final List<Long> actions = new ArrayList<>();
      actions.add(stopTime);
      splits.add(actions);
      timerState = TimerState.STOPPED;
      timerActions.add(TimerAction.STOP);
    } else {
      throw new IllegalStateException("timer has not been started");
    }
  }

  public void reset() {
    timerState = TimerState.UNINITIALISED;
  }

  public long calcAllPauses() {
    long totalPauses = 0;
    for (int i = 0; i < splits.size() - 1; i++) {
      totalPauses += calcPauses(splits.get(i));
    }
    return totalPauses;
  }

  public long calcPauses(final List<Long> actions) {
    long pauseTime = 0;
    for (int i = 1; i < actions.size(); i += 2) {
      pauseTime += (actions.get(i + 1) - actions.get(i));
    }
    return pauseTime;
  }

  public long elapsedTime() {
    if (timerState == TimerState.STOPPED) {
      return splits.get(splits.size() - 1).get(0) - splits.get(0).get(0) - calcAllPauses();
    } else {
      throw new IllegalStateException("timer is not stopped");
    }
  }

  public long elapsedTime(final TimeUnit timeUnit) {
    if (timerState == TimerState.STOPPED) {
      return timeUnit.convert(elapsedTime(), TimeUnit.NANOSECONDS);
    } else {
      throw new IllegalStateException("timer is not stopped");
    }
  }

  public long[] splitTimes() {
    if (timerState == TimerState.STOPPED) {
      final long[] times = new long[splits.size() - 1];
      final long startTime = splits.get(0).get(0);
      long totalPauses = 0;
      for (int i = 1; i < splits.size(); i++) {
        totalPauses += calcPauses(splits.get(i - 1));
        times[i - 1] = splits.get(i).get(0) - startTime - totalPauses;
      }
      return times;
    } else {
      throw new IllegalStateException("split times are only available after timer is stopped");
    }
  }

  public long[] splitTimes(final TimeUnit timeUnit) {
    if (timerState == TimerState.STOPPED) {
      final long[] convertedTimes = new long[splits.size() - 1];
      int i = 0;
      for (long time : splitTimes()) {
        convertedTimes[i++] = timeUnit.convert(time, TimeUnit.NANOSECONDS);
      }
      return convertedTimes;
    } else {
      throw new IllegalStateException("split times are only available after timer is stopped");
    }
  }

  public long[] splitPeriods() {
    if (timerState == TimerState.STOPPED) {
      final long[] periods = new long[splits.size() - 1];
      for (int i = 1; i < splits.size(); i++) {
        periods[i - 1] = splits.get(i).get(0) - splits.get(i - 1).get(0) - calcPauses(splits.get(i - 1));
      }
      return periods;
    } else {
      throw new IllegalStateException("split periods are only available after timer is stopped");
    }
  }

  public long[] splitPeriods(final TimeUnit timeUnit) {
    if (timerState == TimerState.STOPPED) {
      final long[] convertedPeriods = new long[splits.size() - 1];
      int i = 0;
      for (long period : splitPeriods()) {
        convertedPeriods[i++] = timeUnit.convert(period, TimeUnit.NANOSECONDS);
      }
      return convertedPeriods;
    } else {
      throw new IllegalStateException("split periods are only available after timer is stopped");
    }
  }

  @Override
  public String toString() {
    if (timerState != TimerState.STOPPED) {
      return "timer " + timerState.name().toLowerCase();
    } else {
      final Iterator<TimerAction> iter = timerActions.iterator();
      return "{\n" + "  elapsed: " + elapsedTime() + ",\n" + "  splits: " + splits.stream().map(actions -> {
        final StringBuilder s = new StringBuilder();
        final String actionsStr = actions.stream().map(t -> iter.next().action() + "(" + t + ")").collect(Collectors.joining(",", "[", "]"));
        final long pauses = calcPauses(actions);
        s.append("    {").append("\n");
        s.append("      actions: ").append(actionsStr).append(",\n");
        s.append("      paused: ").append(pauses).append("\n");
        s.append("    }");
        return s.toString();
      }).collect(Collectors.joining(",\n", "[\n", "\n  ]")) + "\n}";
    }
  }

}
