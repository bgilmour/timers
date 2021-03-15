package com.langtoun.timers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Timer {

  private List<List<Long>> splits;
  private List<String> timerActions;

  private TimerState state = TimerState.UNINITIALISED;

  private Timer() {
    // create using static method
  }

  public static Timer newTimer() {
    return new Timer();
  }

  public void start() {
    final long startTime = System.nanoTime();
    if (state == TimerState.UNINITIALISED) {
      splits = new ArrayList<>();
      timerActions = new ArrayList<>();
      final List<Long> actions = new ArrayList<>();
      actions.add(startTime);
      splits.add(actions);
      state = TimerState.RUNNING;
      timerActions.add("start");
    } else {
      throw new IllegalStateException("timer can only be started if uninitialised");
    }
  }

  public void split() {
    final long splitTime = System.nanoTime();
    if (state == TimerState.RUNNING) {
      final List<Long> actions = new ArrayList<>();
      actions.add(splitTime);
      splits.add(actions);
      timerActions.add("split");
    } else {
      throw new IllegalStateException("timer can only be split if running");
    }
  }

  public void pause() {
    final long pauseTime = System.nanoTime();
    if (state == TimerState.RUNNING) {
      final List<Long> actions = splits.get(splits.size() - 1);
      actions.add(pauseTime);
      state = TimerState.PAUSED;
      timerActions.add("pause");
    } else if (state != TimerState.PAUSED) {
      throw new IllegalStateException("timer can only be paused if it is running or paused");
    }
  }

  public void resume() {
    final long resumeTime = System.nanoTime();
    if (state == TimerState.PAUSED) {
      final List<Long> actions = splits.get(splits.size() - 1);
      actions.add(resumeTime);
      state = TimerState.RUNNING;
      timerActions.add("resume");
    } else if (state != TimerState.RUNNING) {
      throw new IllegalStateException("timer can only be resumed if it is running or paused");
    }
  }

  public void stop() {
    final long stopTime = System.nanoTime();
    if (state == TimerState.PAUSED) {
      final List<Long> previousActions = splits.get(splits.size() - 1);
      previousActions.add(stopTime);
      timerActions.add("resume");
    }
    if (state == TimerState.PAUSED || state == TimerState.RUNNING) {
      final List<Long> actions = new ArrayList<>();
      actions.add(stopTime);
      splits.add(actions);
      state = TimerState.STOPPED;
      timerActions.add("stop");
    } else {
      throw new IllegalStateException("timer has not been started");
    }
  }

  public void reset() {
    state = TimerState.UNINITIALISED;
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
    if (state == TimerState.STOPPED) {
      return splits.get(splits.size() - 1).get(0) - splits.get(0).get(0) - calcAllPauses();
    } else {
      throw new IllegalStateException("timer is not stopped");
    }
  }

  public long elapsedTime(final TimeUnit timeUnit) {
    if (state == TimerState.STOPPED) {
      return timeUnit.convert(elapsedTime(), TimeUnit.NANOSECONDS);
    } else {
      throw new IllegalStateException("timer is not stopped");
    }
  }

  public long[] splitTimes() {
    if (state == TimerState.STOPPED) {
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
    if (state == TimerState.STOPPED) {
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
    if (state == TimerState.STOPPED) {
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
    if (state == TimerState.STOPPED) {
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

  private enum TimerState {
    UNINITIALISED,
    RUNNING,
    PAUSED,
    STOPPED
  }

  @Override
  public String toString() {
    if (state != TimerState.STOPPED) {
      return "timer " + state.name().toLowerCase();
    } else {
      final Iterator<String> iter = timerActions.iterator();
      return "{\n" + "  elapsed: " + elapsedTime() + ",\n" + "  splits: " + splits.stream().map(actions -> {
        final StringBuilder s = new StringBuilder();
        final String actionsStr = actions.stream().map(t -> iter.next() + "(" + t + ")").collect(Collectors.joining(",", "[", "]"));
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
