package com.langtoun.timers;

/**
 * The states through which a {@link NanoTimer} object transitions.
 */
public enum NanoTimerState {
  UNINITIALISED("uninitialised"),
  RUNNING("running"),
  PAUSED("paused"),
  STOPPED("stopped");

  private String state;

  private NanoTimerState(final String state) {
    this.state = state;
  }

  /**
   * Get the display name of the timer state.
   *
   * @return the display name of the timer state
   */
  public String state() {
    return state;
  }
}
