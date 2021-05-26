package com.langtoun.timers;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Lightweight nanosecond timer class supporting reset, start, split, pause,
 * resume, and stop actions with results presented in an optional specified
 * {@link TimeUnit}.
 */
public final class NanoTimer {

  private static final String TIMER_START_EXCEPTION = "timer can only be started if uninitialised";
  private static final String TIMER_SPLIT_EXCEPTION = "timer can only be split if running";
  private static final String TIMER_PAUSE_EXCEPTION = "timer can only be paused if it is running or paused";
  private static final String TIMER_RESUME_EXCEPTION = "timer can only be resumed if it is running or paused";
  private static final String TIMER_STOP_EXCEPTION = "timer is not stopped";
  private static final String TIMER_ELAPSED_TIME_EXCEPTION = "elapsed time is only available after the timer is stopped";
  private static final String TIMER_SPLIT_TIMES_EXCEPTION = "split times are only available after the timer is stopped";
  private static final String TIMER_SPLIT_PERIODS_EXCEPTION = "split periods are only available after the timer is stopped";
  private static final String TIMER_SPLIT_OUT_OF_RANGE = "split period index %d out of range: 0 < index <= %d";
  private static final String TIMER_SPLIT_WITH_NAME = "%s[%d %s]";

  private final String name;

  private List<TimerSplit> splits;

  private Long elapsedTime;
  private long[] splitTimes;
  private long[] splitPeriods;

  private NanoTimerState timerState = NanoTimerState.UNINITIALISED;

  private NanoTimer() {
    this.name = "";
  }

  private NanoTimer(final String name) {
    this.name = name;
  }

  /**
   * Static factory method that creates a new unnamed timer.
   *
   * @return the unnamed timer object
   */
  public static NanoTimer newTimer() {
    return new NanoTimer();
  }

  /**
   * Static factory method that creates a new named timer.
   *
   * @param name the name of the timer
   * @return the named timer object
   */
  public static NanoTimer newTimer(final String name) {
    return new NanoTimer(name);
  }

  public String getName() { return name; }

  public NanoTimerState getTimerState() { return timerState; }

  /**
   * Start this timer if it is in the {@link NanoTimerState.UNINITIALISED} state,
   * otherwise throw an exception.
   *
   * @param splitName a name to be assigned to the split period
   * @return a reference to this timer
   */
  public NanoTimer start(final String splitName) {
    final long startTime = System.nanoTime();
    if (timerState == NanoTimerState.UNINITIALISED) {
      splits = new ArrayList<>();
      final TimerSplit split = new TimerSplit(splitName);
      split.actions.add(new SplitAction(NanoTimerAction.START, startTime));
      splits.add(split);
      timerState = NanoTimerState.RUNNING;
    } else {
      throw new IllegalStateException(TIMER_START_EXCEPTION);
    }
    return this;
  }

  /**
   * Start this timer if it is in the {@link NanoTimerState.UNINITIALISED} state,
   * otherwise throw an exception.
   *
   * @return a reference to this timer
   */
  public NanoTimer start() {
    return start(null);
  }

  /**
   * Create a new split for this timer if it is in the
   * {@link NanoTimerState.RUNNING} or {@link NanoTimerState.PAUSED} state,
   * otherwise throw an exception.
   *
   * @param splitName a name to be assigned to the split period
   */
  public void split(final String splitName) {
    final long splitTime = System.nanoTime();
    if (timerState == NanoTimerState.PAUSED) {
      final TimerSplit previousSplit = splits.get(splits.size() - 1);
      previousSplit.actions.add(new SplitAction(NanoTimerAction.RESUME, splitTime));
      timerState = NanoTimerState.RUNNING;
    }
    if (timerState == NanoTimerState.RUNNING) {
      final TimerSplit split = new TimerSplit(splitName);
      split.actions.add(new SplitAction(NanoTimerAction.SPLIT, splitTime));
      splits.add(split);
    } else {
      throw new IllegalStateException(TIMER_SPLIT_EXCEPTION);
    }
  }

  /**
   * Create a new split for this timer if it is in the
   * {@link NanoTimerState.RUNNING} or {@link NanoTimerState.PAUSED} state,
   * otherwise throw an exception.
   */
  public void split() {
    split(null);
  }

  /**
   * Pause this timer. Time elapsed between {@link NanoTimerAction.PAUSE} and
   * {@link NanoTimerAction.RESUME} actions does not contribute to the overall
   * elapsed time.
   */
  public void pause() {
    final long pauseTime = System.nanoTime();
    if (timerState == NanoTimerState.RUNNING) {
      final TimerSplit split = splits.get(splits.size() - 1);
      split.actions.add(new SplitAction(NanoTimerAction.PAUSE, pauseTime));
      timerState = NanoTimerState.PAUSED;
    } else if (timerState != NanoTimerState.PAUSED) {
      throw new IllegalStateException(TIMER_PAUSE_EXCEPTION);
    }
  }

  /**
   * Resume this timer. Time elapsed between {@link NanoTimerAction.PAUSE} and
   * {@link NanoTimerAction.RESUME} actions does not contribute to the overall
   * elapsed time.
   */
  public void resume() {
    final long resumeTime = System.nanoTime();
    if (timerState == NanoTimerState.PAUSED) {
      final TimerSplit split = splits.get(splits.size() - 1);
      split.actions.add(new SplitAction(NanoTimerAction.RESUME, resumeTime));
      timerState = NanoTimerState.RUNNING;
    } else if (timerState != NanoTimerState.RUNNING) {
      throw new IllegalStateException(TIMER_RESUME_EXCEPTION);
    }
  }

  /**
   * Stop this timer if it is in the {@link NanoTimerState.RUNNING} or
   * {@link NanoTimerState.PAUSED} state, otherwise throw an exception.
   *
   * @param splitName a name to be assigned to the split period
   * @return a reference to this timer
   */
  public NanoTimer stop(final String splitName) {
    final long stopTime = System.nanoTime();
    if (timerState == NanoTimerState.PAUSED) {
      final TimerSplit split = splits.get(splits.size() - 1);
      split.actions.add(new SplitAction(NanoTimerAction.RESUME, stopTime));
      timerState = NanoTimerState.RUNNING;
    }
    if (timerState == NanoTimerState.RUNNING) {
      final TimerSplit split = new TimerSplit(splitName);
      split.actions.add(new SplitAction(NanoTimerAction.STOP, stopTime));
      splits.add(split);
      timerState = NanoTimerState.STOPPED;
    } else {
      throw new IllegalStateException(TIMER_STOP_EXCEPTION);
    }
    return this;
  }

  /**
   * Stop this timer if it is in the {@link NanoTimerState.RUNNING} or
   * {@link NanoTimerState.PAUSED} state, otherwise throw an exception.
   *
   * @return a reference to this timer
   */
  public NanoTimer stop() {
    return stop(null);
  }

  /**
   * Reset this timer to the {@link NanoTimerState.UNINITIALISED} state.
   */
  public void reset() {
    timerState = NanoTimerState.UNINITIALISED;
    elapsedTime = null;
    splitTimes = null;
    splitPeriods = null;
  }

  /**
   * Calculate the elapsed time for which the timer has been running. This
   * excludes any periods during which the timer was paused. If the timer is not
   * in the {@link NanoTimerState.STOPPED} state an exception is thrown.
   *
   * @return the elapsed time expressed in {@link TimeUnit.NANOSECONDS}
   */
  public long elapsedTime() {
    if (timerState == NanoTimerState.STOPPED) {
      if (elapsedTime == null) {
        elapsedTime = splits.get(splits.size() - 1).actions.get(0).timestamp - splits.get(0).actions.get(0).timestamp - calcAllPauses();
      }
      return elapsedTime;
    } else {
      throw new IllegalStateException(TIMER_ELAPSED_TIME_EXCEPTION);
    }
  }

  /**
   * Calculate the elapsed time in the specified {@link TimeUnit} for which the
   * timer has been running. This excludes any periods during which the timer was
   * paused. If the timer is not in the {@link NanoTimerState.STOPPED} state an
   * exception is thrown.
   *
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return the elapsed time expressed in the specified {@link TimeUnit}
   */
  public long elapsedTime(final TimeUnit timeUnit) {
    if (timerState == NanoTimerState.STOPPED) {
      return timeUnit.convert(elapsedTime(), TimeUnit.NANOSECONDS);
    } else {
      throw new IllegalStateException(TIMER_ELAPSED_TIME_EXCEPTION);
    }
  }

  /**
   * Return the split times recorded for this timer. The split times represent the
   * elapsed time from the {@link NanoTimerAction.START} action to each optional
   * {@link NanoTimerAction.SPLIT} action, ending with the
   * {@link NanoTimerAction.STOP} action. Each split time is presented with the
   * calculated pauses removed.
   *
   * @return an array of split times expressed in {@link TimeUnit.NANOSECONDS}
   */
  public long[] splitTimes() {
    if (timerState == NanoTimerState.STOPPED) {
      if (splitTimes == null) {
        splitTimes = new long[splits.size() - 1];
        final long startTime = splits.get(0).actions.get(0).timestamp;
        long totalPauses = 0;
        for (int i = 1; i < splits.size(); i++) {
          totalPauses += calcPauses(splits.get(i - 1));
          splitTimes[i - 1] = splits.get(i).actions.get(0).timestamp - startTime - totalPauses;
        }
      }
      return splitTimes;
    } else {
      throw new IllegalStateException(TIMER_SPLIT_TIMES_EXCEPTION);
    }
  }

  /**
   * Return the split times recorded for this timer. The split times represent the
   * elapsed time from the {@link NanoTimerAction.START} action to each optional
   * {@link NanoTimerAction.SPLIT} action, ending with the
   * {@link NanoTimerAction.STOP} action. Each split time is presented with the
   * calculated pauses removed.
   *
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return an array of split times expressed in the specified {@link TimeUnit}
   */
  public long[] splitTimes(final TimeUnit timeUnit) {
    if (timerState == NanoTimerState.STOPPED) {
      final long[] convertedTimes = new long[splits.size() - 1];
      int i = 0;
      for (long time : splitTimes()) {
        convertedTimes[i++] = timeUnit.convert(time, TimeUnit.NANOSECONDS);
      }
      return convertedTimes;
    } else {
      throw new IllegalStateException(TIMER_SPLIT_TIMES_EXCEPTION);
    }
  }

  /**
   * Return a split time specified by index that was recorded for this timer. The
   * split times represent the elapsed time from the {@link NanoTimerAction.START}
   * action to each optional {@link NanoTimerAction.SPLIT} action, ending with the
   * {@link NanoTimerAction.STOP} action. The split time is presented with the
   * calculated pauses removed.
   *
   * @param index    the index of the requested split time
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return the indexed split time expressed in the specified {@link TimeUnit}
   */
  public long splitTime(final int index, final TimeUnit timeUnit) {
    if (timerState == NanoTimerState.STOPPED) {
      if (index < 0 || index >= splits.size() - 1) {
        throw new IllegalArgumentException(String.format(TIMER_SPLIT_OUT_OF_RANGE, index, splits.size() - 1));
      }
      return timeUnit.convert(splitTimes()[index], TimeUnit.NANOSECONDS);
    } else {
      throw new IllegalStateException(TIMER_SPLIT_TIMES_EXCEPTION);
    }
  }

  /**
   * Return a split time specified by index that was recorded for this timer
   * formatted to include the name (if any) of the split and the time unit. The
   * split times represent the elapsed time from the {@link NanoTimerAction.START}
   * action to each optional {@link NanoTimerAction.SPLIT} action, ending with the
   * {@link NanoTimerAction.STOP} action. The split time is presented with the
   * calculated pauses removed.
   *
   * @param index    the index of the requested split time
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return the indexed split time expressed in the specified {@link TimeUnit}
   */
  public String splitTimeWithName(final int index, final TimeUnit timeUnit) {
    if (timerState == NanoTimerState.STOPPED) {
      if (index < 0 || index >= splits.size() - 1) {
        throw new IllegalArgumentException(String.format("split time index %d out of range: 0 < index <= %d", index, splits.size() - 1));
      }
      return String.format(TIMER_SPLIT_WITH_NAME, splits.get(index).getName(), timeUnit.convert(splitTimes()[index], TimeUnit.NANOSECONDS), timeUnit.name().toLowerCase(Locale.ENGLISH));
    } else {
      throw new IllegalStateException(TIMER_SPLIT_TIMES_EXCEPTION);
    }
  }

  /**
   * Return the split periods recorded for this timer. The split periods are the
   * elapsed times between the {@link NanoTimerAction} events starting with the
   * {@link NanoTimerAction.START} action, followed by optional
   * {@link NanoTimerAction.SPLIT} actions, and ending with the
   * {@link NanoTimerAction.STOP} action. Each split period is presented with the
   * calculated pauses removed.
   *
   * @return an array of split periods expressed in {@link TimeUnit.NANOSECONDS}.
   */
  public long[] splitPeriods() {
    if (timerState == NanoTimerState.STOPPED) {
      if (splitPeriods == null) {
        splitPeriods = new long[splits.size() - 1];
        for (int i = 1; i < splits.size(); i++) {
          splitPeriods[i - 1] = splits.get(i).actions.get(0).timestamp - splits.get(i - 1).actions.get(0).timestamp - calcPauses(splits.get(i - 1));
        }
      }
      return splitPeriods;
    } else {
      throw new IllegalStateException(TIMER_SPLIT_PERIODS_EXCEPTION);
    }
  }

  /**
   * Return the split periods recorded for this timer. The split periods are the
   * elapsed times between the {@link NanoTimerAction} events starting with the
   * {@link NanoTimerAction.START} action, followed by optional
   * {@link NanoTimerAction.SPLIT} actions, and ending with the
   * {@link NanoTimerAction.STOP} action. Each split period is presented with the
   * calculated pauses removed.
   *
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return an array of split periods expressed in the specified
   *         {@link TimeUnit}.
   */
  public long[] splitPeriods(final TimeUnit timeUnit) {
    if (timerState == NanoTimerState.STOPPED) {
      final long[] convertedPeriods = new long[splits.size() - 1];
      int i = 0;
      for (long period : splitPeriods()) {
        convertedPeriods[i++] = timeUnit.convert(period, TimeUnit.NANOSECONDS);
      }
      return convertedPeriods;
    } else {
      throw new IllegalStateException(TIMER_SPLIT_PERIODS_EXCEPTION);
    }
  }

  /**
   * Return the split period specified by index that was recorded for this timer.
   * The split periods are the elapsed times between the {@link NanoTimerAction}
   * events starting with the {@link NanoTimerAction.START} action, followed by
   * optional {@link NanoTimerAction.SPLIT} actions, and ending with the
   * {@link NanoTimerAction.STOP} action. The split period is presented with the
   * calculated pauses removed.
   *
   * @param index    the index of the requested split period
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return the indexed split period expressed in the specified {@link TimeUnit}.
   */
  public long splitPeriod(final int index, final TimeUnit timeUnit) {
    if (timerState == NanoTimerState.STOPPED) {
      if (index < 0 || index >= splits.size() - 1) {
        throw new IllegalArgumentException(String.format(TIMER_SPLIT_OUT_OF_RANGE, index, splits.size() - 1));
      }
      return timeUnit.convert(splitPeriods()[index], TimeUnit.NANOSECONDS);
    } else {
      throw new IllegalStateException(TIMER_SPLIT_PERIODS_EXCEPTION);
    }
  }

  /**
   * Return the split period specified by index that was recorded for this timer
   * formatted to include the name (if any) of the split and the time unit. The
   * split periods are the elapsed times between the {@link NanoTimerAction}
   * events starting with the {@link NanoTimerAction.START} action, followed by
   * optional {@link NanoTimerAction.SPLIT} actions, and ending with the
   * {@link NanoTimerAction.STOP} action. The split period is presented with the
   * calculated pauses removed.
   *
   * @param index    the index of the requested split period
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return the indexed split period expressed in the specified {@link TimeUnit}.
   */
  public String splitPeriodWithName(final int index, final TimeUnit timeUnit) {
    if (timerState == NanoTimerState.STOPPED) {
      if (index < 0 || index >= splits.size() - 1) {
        throw new IllegalArgumentException(String.format("split period index %d out of range: 0 < index <= %d", index, splits.size() - 1));
      }
      return String.format(TIMER_SPLIT_WITH_NAME, splits.get(index).getName(), timeUnit.convert(splitPeriods()[index], TimeUnit.NANOSECONDS), timeUnit.name().toLowerCase(Locale.ENGLISH));
    } else {
      throw new IllegalStateException(TIMER_SPLIT_PERIODS_EXCEPTION);
    }
  }

  @Override
  public String toString() {
    return toString(TimeUnit.NANOSECONDS);
  }

  /**
   * Create a string representation of the NanoTimer.
   *
   * @param timeUnit the {@link TimeUnit} to use for the elapsed time output
   * @return a string description of the timer activity or the current state if it
   *         has not been stopped
   */
  public String toString(final TimeUnit timeUnit) {
    if (timerState != NanoTimerState.STOPPED) {
      return "timer " + timerState.state();
    } else {
      return "{\n" + "  name: \"" + name + "\",\n  elapsed: " + elapsedTime(timeUnit) + " " + timeUnit.name().toLowerCase(Locale.ENGLISH) + ",\n" + "  splits: " + splits.stream().map(split -> {
        final StringBuilder s = new StringBuilder();
        final String actionsStr = split.actions.stream().map(sa -> sa.timerAction.action() + "(" + sa.timestamp + ")").collect(Collectors.joining(",", "[", "]"));
        final long pauses = calcPauses(split);
        s.append("    {").append("\n");
        s.append("      name: ").append(split.getName()).append(",\n");
        s.append("      actions: ").append(actionsStr).append(",\n");
        s.append("      paused: ").append(pauses).append("\n");
        s.append("    }");
        return s.toString();
      }).collect(Collectors.joining(",\n", "[\n", "\n  ]")) + "\n}";
    }
  }

  private long calcAllPauses() {
    long totalPauses = 0;
    for (int i = 0; i < splits.size() - 1; i++) {
      totalPauses += calcPauses(splits.get(i));
    }
    return totalPauses;
  }

  private long calcPauses(final TimerSplit split) {
    long pauseTime = 0;
    for (int i = 1; i < split.actions.size(); i += 2) {
      pauseTime += (split.actions.get(i + 1).timestamp - split.actions.get(i).timestamp);
    }
    return pauseTime;
  }

  public static class TimerSplit {

    private final String name;
    private final List<SplitAction> actions;

    public TimerSplit() {
      this(null);
    }

    public TimerSplit(final String name) {
      this.name = name;
      this.actions = new ArrayList<>();
    }

    /**
     * Return the assigned name of the split period or the name of the action that
     * created the split if no name was specified.
     *
     * @return the name of this split period
     */
    public String getName() {
      if (name != null) {
        return name;
      }
      return actions.get(0).timerAction.action();
    }
  }

  public static class SplitAction {

    private final NanoTimerAction timerAction;
    private final long timestamp;

    public SplitAction(final NanoTimerAction timerAction, final long timestamp) {
      this.timerAction = timerAction;
      this.timestamp = timestamp;
    }
  }

}
