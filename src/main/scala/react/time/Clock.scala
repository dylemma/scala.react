/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react
package time

import java.util.{ Timer, TimerTask, Date }

object Clock {
	lazy val milliseconds = withResolution(1 msecs)
	lazy val seconds = withResolution(1 secs)

	private val timer = new Timer

	/** An event stream that fires one event after the given time
	  */
	def in(t: Time): Events[Unit] = new EventSource[Unit] {
		doIn(t) { this emit () }
	}
	/** An event stream that fires one event after the given date
	  */
	def at(d: Date): Events[Unit] = new EventSource[Unit] {
		doAt(d) { this emit () }
	}
	/** An event stream that periodically fires an event
	  */
	def every[A](period: Time): Events[Unit] = new EventSource[Unit] {
		doEvery(period) { this emit () }
	}

	def withResolution(time: Time): Signal[Time] = {
		val t0 = (System.nanoTime nsecs)
		every(time) tag ((System.nanoTime nsecs) - t0) hold (0 nsecs)
	}

	def doEvery(msecs: Long)(op: => Unit) {
		timer.schedule(task(op), 0, msecs)
	}

	def doEvery(time: Time)(op: => Unit) {
		doEvery(math.round(time.toMilliSeconds)) { op }
	}

	def doIn(msecs: Long)(op: => Unit) {
		timer.schedule(task(op), msecs)
	}

	def doIn(time: Time)(op: => Unit) {
		doIn(math.round(time.toMilliSeconds)) { op }
	}

	def doAt(date: Date)(op: => Unit) {
		timer.schedule(task(op), date)
	}

	protected def task(op: => Unit) = new TimerTask {
		def run { Engine.runBlocking { op } }
	}
}
