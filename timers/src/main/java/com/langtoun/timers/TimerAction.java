package com.langtoun.timers;

public enum TimerAction {
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
