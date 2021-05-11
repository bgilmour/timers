# Timers
A utility class that provides a simple timer based on nanosecond timestamps provided by
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

After stopping the timer, the recorded times can be retrieved in the following ways:
- Elapsed time (TimeUnit.NANOSECOND)
- Elapsed time (specified TimeUnit)
- Split times (array of longs, expressed in TimeUnit.NANOSECONDS)
- Split times (array of longs, converted using specified TimeUnit)
- Split time (long from specified index, converted using specified TimeUnit.NANOSECONDS)
- Split periods (array of longs, expressed in TimeUnit.NANOSECONDS)
- Split periods (array of longs, converted using specified TimeUnit)
- Split period (long from specified index, converted using specified TimeUnit.NANOSECONDS)
