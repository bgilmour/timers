package com.langtoun.timers;

import java.util.HashMap;
import java.util.Map;

public final class NanoTimers {

  private static ThreadLocal<Map<String, NanoTimer>> timers = ThreadLocal.withInitial(() -> new HashMap<>());

  private NanoTimers() {
    // class contains static methods for working with timers
  }

  public static NanoTimer createTimer(final String name) {
    final NanoTimer timer = NanoTimer.newTimer(name);
    timers.get().put(name, timer);
    return timer;
  }

  public static NanoTimer findTimer(final String name) {
    return timers.get().get(name);
  }

}
