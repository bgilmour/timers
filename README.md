# Timers
A utility class that provides a simple timer based on nanoseconds timestamps provided by
calls to `System.nanoTime()`. The timer provides the following actions:
- reset
- start
- split
- pause
- resume
- stop

The timer states are:
- UNINITIALISED
- RUNNING
- PAUSED
- STOPPED