package com.langtoun.timers;

/**
 * Actions that can be sent to a {@link NanoTimer} object.
 */
public enum NanoTimerAction {
  RESET("reset"),
  START("start"),
  SPLIT("split"),
  PAUSE("pause"),
  RESUME("resume"),
  STOP("stop");

  private String action;

  private NanoTimerAction(final String action) {
    this.action = action;
  }

  /**
   * Get the display name of the timer action.
   *
   * @return the display name of the timer action
   */
  public String action() {
    return action;
  }
}
