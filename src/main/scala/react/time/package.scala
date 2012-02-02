/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

package object time {
	val nsecs2Secs = 0.000000001
	val msecs2Secs = 0.001
	val mins2Secs = 60
	val hours2Secs = mins2Secs * 60
	val days2Secs = hours2Secs * 24

	implicit def double2TimeValue(v: Double): TimeValue = new TimeValue {
		def days = new Time(v * days2Secs)
		def hours = new Time(v * hours2Secs)
		def mins = new Time(v * mins2Secs)
		def secs = new Time(v)
		def msecs = new Time(v * msecs2Secs)
		def nsecs = new Time(v * nsecs2Secs)
	}
	implicit def int2TimeValue(v: Int): TimeValue = double2TimeValue(v)
	implicit def long2TimeValue(v: Long): TimeValue = double2TimeValue(v)

	trait TimeValue {
		def days: Time
		def hours: Time
		def mins: Time
		def secs: Time
		def msecs: Time
		def nsecs: Time
	}
}