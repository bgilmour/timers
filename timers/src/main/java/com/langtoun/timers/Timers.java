package com.langtoun.timers;

import java.util.HashMap;
import java.util.Map;

public final class Timers {

  private static ThreadLocal<Map<String, Timer>> timers = ThreadLocal.withInitial(() -> new HashMap<>());

  private Timers() {
    // class contains static methods for working with timers
  }

  public static Timer createTimer(final String name) {
    final Timer timer = Timer.newTimer();
    timers.get().put(name, timer);
    return timer;
  }

  public static Timer findTimer(final String name) {
    return timers.get().get(name);
  }

}
