package com.langtoun.timers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Lightweight nanosecond timer class supporting reset, start, split, pause,
 * resume, and stop actions with results presented in an optional specified
 * {@link TimeUnit}.
 */
@SuppressWarnings("nls")
public final class Timer {

  private static final String TIMER_START_EXCEPTION = "timer can only be started if uninitialised";
  private static final String TIMER_SPLIT_EXCEPTION = "timer can only be split if running";
  private static final String TIMER_PAUSE_EXCEPTION = "timer can only be paused if it is running or paused";
  private static final String TIMER_RESUME_EXCEPTION = "timer can only be resumed if it is running or paused";
  private static final String TIMER_STOP_EXCEPTION = "timer is not stopped";
  private static final String TIMER_ELAPSED_TIME_EXCEPTION = "elapsed time is only available after the timer is stopped";
  private static final String TIMER_SPLIT_TIMES_EXCEPTION = "split times are only available after the timer is stopped";
  private static final String TIMER_SPLIT_PERIODS_EXCEPTION = "split periods are only available after the timer is stopped";

  private final String name;

  private List<List<SplitAction>> splits;

  private Long elapsedTime;
  private long[] splitTimes;
  private long[] splitPeriods;

  private TimerState timerState = TimerState.UNINITIALISED;

  private Timer() {
    this.name = "";
  }

  private Timer(final String name) {
    this.name = name;
  }

  /**
   * Static factory method that creates a new unnamed timer.
   *
   * @return the unnamed timer object
   */
  public static Timer newTimer() {
    return new Timer();
  }

  /**
   * Static factory method that creates a new named timer.
   *
   * @param name the name of the timer
   * @return the named timer object
   */
  public static Timer newTimer(final String name) {
    return new Timer(name);
  }

  public String getName() { return name; }

  /**
   * Start this timer if it is in the {@link TimerState.UNINITIALISED} state,
   * otherwise throw an exception.
   *
   * @return a reference to this timer
   */
  public Timer start() {
    final long startTime = System.nanoTime();
    if (timerState == TimerState.UNINITIALISED) {
      splits = new ArrayList<>();
      final List<SplitAction> actions = new ArrayList<>();
      actions.add(new SplitAction(TimerAction.START, startTime));
      splits.add(actions);
      timerState = TimerState.RUNNING;
    } else {
      throw new IllegalStateException(TIMER_START_EXCEPTION);
    }
    return this;
  }

  /**
   * Create a new split entry for this timer.
   */
  public void split() {
    final long splitTime = System.nanoTime();
    if (timerState == TimerState.RUNNING) {
      final List<SplitAction> actions = new ArrayList<>();
      actions.add(new SplitAction(TimerAction.SPLIT, splitTime));
      splits.add(actions);
    } else {
      throw new IllegalStateException(TIMER_SPLIT_EXCEPTION);
    }
  }

  /**
   * Pause this timer. Time elapsed between {@link TimerAction.PAUSE} and
   * {@link TimerAction.RESUME} actions does not contribute to the overall elapsed
   * time.
   */
  public void pause() {
    final long pauseTime = System.nanoTime();
    if (timerState == TimerState.RUNNING) {
      final List<SplitAction> actions = splits.get(splits.size() - 1);
      actions.add(new SplitAction(TimerAction.PAUSE, pauseTime));
      timerState = TimerState.PAUSED;
    } else if (timerState != TimerState.PAUSED) {
      throw new IllegalStateException(TIMER_PAUSE_EXCEPTION);
    }
  }

  /**
   * Resume this timer. Time elapsed between {@link TimerAction.PAUSE} and
   * {@link TimerAction.RESUME} actions does not contribute to the overall elapsed
   * time.
   */
  public void resume() {
    final long resumeTime = System.nanoTime();
    if (timerState == TimerState.PAUSED) {
      final List<SplitAction> actions = splits.get(splits.size() - 1);
      actions.add(new SplitAction(TimerAction.RESUME, resumeTime));
      timerState = TimerState.RUNNING;
    } else if (timerState != TimerState.RUNNING) {
      throw new IllegalStateException(TIMER_RESUME_EXCEPTION);
    }
  }

  /**
   * Stop this timer if it is in the {@link TimerState.RUNNING} or
   * {@link TimerState.PAUSED} state, otherwise throw an exception.
   *
   * @return a reference to this timer
   */
  public Timer stop() {
    final long stopTime = System.nanoTime();
    if (timerState == TimerState.PAUSED) {
      final List<SplitAction> previousActions = splits.get(splits.size() - 1);
      previousActions.add(new SplitAction(TimerAction.RESUME, stopTime));
    }
    if (timerState == TimerState.PAUSED || timerState == TimerState.RUNNING) {
      final List<SplitAction> actions = new ArrayList<>();
      actions.add(new SplitAction(TimerAction.STOP, stopTime));
      splits.add(actions);
      timerState = TimerState.STOPPED;
    } else {
      throw new IllegalStateException(TIMER_STOP_EXCEPTION);
    }
    return this;
  }

  /**
   * Reset this timer to the {@link TimerState.UNINITIALISED} state.
   */
  public void reset() {
    timerState = TimerState.UNINITIALISED;
    elapsedTime = null;
    splitTimes = null;
    splitPeriods = null;
  }

  /**
   * Calculate the elapsed time for which the timer has been running. This
   * excludes any periods during which the timer was paused. If the timer is not
   * in the {@link TimerState.STOPPED} state an exception is thrown.
   *
   * @return the elapsed time expressed in {@link TimeUnit.NANOSECONDS}
   */
  public long elapsedTime() {
    if (timerState == TimerState.STOPPED) {
      if (elapsedTime == null) {
        elapsedTime = splits.get(splits.size() - 1).get(0).timestamp - splits.get(0).get(0).timestamp - calcAllPauses();
      }
      return elapsedTime;
    } else {
      throw new IllegalStateException(TIMER_ELAPSED_TIME_EXCEPTION);
    }
  }

  /**
   * Calculate the elapsed time in the specified {@link TimeUnit} for which the
   * timer has been running. This excludes any periods during which the timer was
   * paused. If the timer is not in the {@link TimerState.STOPPED} state an
   * exception is thrown.
   *
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return the elapsed time expressed in the specified {@link TimeUnit}
   */
  public long elapsedTime(final TimeUnit timeUnit) {
    if (timerState == TimerState.STOPPED) {
      return timeUnit.convert(elapsedTime(), TimeUnit.NANOSECONDS);
    } else {
      throw new IllegalStateException(TIMER_ELAPSED_TIME_EXCEPTION);
    }
  }

  /**
   * Return the split times recorded for this timer. The split times represent the
   * elapsed time from the {@link TimerAction.START} action to each optional
   * {@link TimerAction.SPLIT} action, ending with the {@link TimerAction.STOP}
   * action. Each split time is presented with the calculated pauses removed.
   *
   * @return an array of split times expressed in {@link TimeUnit.NANOSECONDS}
   */
  public long[] splitTimes() {
    if (timerState == TimerState.STOPPED) {
      if (splitTimes == null) {
        splitTimes = new long[splits.size() - 1];
        final long startTime = splits.get(0).get(0).timestamp;
        long totalPauses = 0;
        for (int i = 1; i < splits.size(); i++) {
          totalPauses += calcPauses(splits.get(i - 1));
          splitTimes[i - 1] = splits.get(i).get(0).timestamp - startTime - totalPauses;
        }
      }
      return splitTimes;
    } else {
      throw new IllegalStateException(TIMER_SPLIT_TIMES_EXCEPTION);
    }
  }

  /**
   * Return the split times recorded for this timer. The split times represent the
   * elapsed time from the {@link TimerAction.START} action to each optional
   * {@link TimerAction.SPLIT} action, ending with the {@link TimerAction.STOP}
   * action. Each split time is presented with the calculated pauses removed.
   *
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return an array of split times expressed in the specified {@link TimeUnit}
   */
  public long[] splitTimes(final TimeUnit timeUnit) {
    if (timerState == TimerState.STOPPED) {
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
   * split times represent the elapsed time from the {@link TimerAction.START}
   * action to each optional {@link TimerAction.SPLIT} action, ending with the
   * {@link TimerAction.STOP} action. Each split time is presented with the
   * calculated pauses removed.
   *
   * @param index    the index of the requested split time
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return the indexed split time expressed in the specified {@link TimeUnit}
   */
  public long splitTime(final int index, final TimeUnit timeUnit) {
    if (timerState == TimerState.STOPPED) {
      if (index < 0 || index >= splits.size() - 1) {
        throw new IllegalArgumentException(String.format("split time index %d out of range: 0 < index <= %d", index, splits.size() - 1));
      }
      return timeUnit.convert(splitTimes()[index], TimeUnit.NANOSECONDS);
    } else {
      throw new IllegalStateException(TIMER_SPLIT_TIMES_EXCEPTION);
    }
  }

  /**
   * Return the split periods recorded for this timer. The split periods are the
   * elapsed times between the {@link TimerAction} events starting with the
   * {@link TimerAction.START} action, followed by optional
   * {@link TimerAction.SPLIT} actions, and ending with the
   * {@link TimerAction.STOP} action. Each split period is presented with the
   * calculated pauses removed.
   *
   * @return an array of split periods expressed in {@link TimeUnit.NANOSECONDS}.
   */
  public long[] splitPeriods() {
    if (timerState == TimerState.STOPPED) {
      if (splitPeriods == null) {
        splitPeriods = new long[splits.size() - 1];
        for (int i = 1; i < splits.size(); i++) {
          splitPeriods[i - 1] = splits.get(i).get(0).timestamp - splits.get(i - 1).get(0).timestamp - calcPauses(splits.get(i - 1));
        }
      }
      return splitPeriods;
    } else {
      throw new IllegalStateException(TIMER_SPLIT_PERIODS_EXCEPTION);
    }
  }

  /**
   * Return the split periods recorded for this timer. The split periods are the
   * elapsed times between the {@link TimerAction} events starting with the
   * {@link TimerAction.START} action, followed by optional
   * {@link TimerAction.SPLIT} actions, and ending with the
   * {@link TimerAction.STOP} action. Each split period is presented with the
   * calculated pauses removed.
   *
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return an array of split periods expressed in the specified
   *         {@link TimeUnit}.
   */
  public long[] splitPeriods(final TimeUnit timeUnit) {
    if (timerState == TimerState.STOPPED) {
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
   * The split periods are the elapsed times between the {@link TimerAction}
   * events starting with the {@link TimerAction.START} action, followed by
   * optional {@link TimerAction.SPLIT} actions, and ending with the
   * {@link TimerAction.STOP} action. Each split period is presented with the
   * calculated pauses removed.
   *
   * @param index    the index of the requested split period
   * @param timeUnit the {@link TimeUnit} to be used for the result
   * @return the indexed split period expressed in the specified {@link TimeUnit}.
   */
  public long splitPeriod(final int index, final TimeUnit timeUnit) {
    if (timerState == TimerState.STOPPED) {
      if (index < 0 || index >= splits.size() - 1) {
        throw new IllegalArgumentException(String.format("split period index %d out of range: 0 < index <= %d", index, splits.size() - 1));
      }
      return timeUnit.convert(splitPeriods()[index], TimeUnit.NANOSECONDS);
    } else {
      throw new IllegalStateException(TIMER_SPLIT_PERIODS_EXCEPTION);
    }
  }

  @Override
  public String toString() {
    if (timerState != TimerState.STOPPED) {
      return "timer " + timerState.state();
    } else {
      return "{\n" + "  name: \"" + name + "\",\n  elapsed: " + elapsedTime() + ",\n" + "  splits: " + splits.stream().map(actions -> {
        final StringBuilder s = new StringBuilder();
        final String actionsStr = actions.stream().map(sa -> sa.timerAction.action() + "(" + sa.timestamp + ")").collect(Collectors.joining(",", "[", "]"));
        final long pauses = calcPauses(actions);
        s.append("    {").append("\n");
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

  private long calcPauses(final List<SplitAction> actions) {
    long pauseTime = 0;
    for (int i = 1; i < actions.size(); i += 2) {
      pauseTime += (actions.get(i + 1).timestamp - actions.get(i).timestamp);
    }
    return pauseTime;
  }

  public static class SplitAction {

    private final TimerAction timerAction;
    private final long timestamp;

    public SplitAction(final TimerAction timerAction, final long timestamp) {
      this.timerAction = timerAction;
      this.timestamp = timestamp;
    }

  }

  private enum TimerState {
    UNINITIALISED("uninitialised"),
    RUNNING("running"),
    PAUSED("paused"),
    STOPPED("stopped");

    private String state;

    private TimerState(final String state) {
      this.state = state;
    }

    public String state() {
      return state;
    }
  }

  private enum TimerAction {
    RESET("reset"),
    START("start"),
    SPLIT("split"),
    PAUSE("pause"),
    RESUME("resume"),
    STOP("stop");

    private String action;

    private TimerAction(final String action) {
      this.action = action;
    }

    public String action() {
      return action;
    }
  }

}
