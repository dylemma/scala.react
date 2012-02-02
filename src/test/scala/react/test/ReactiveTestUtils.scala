package scala.react.test

import org.scalatest.junit.ShouldMatchersForJUnit._

trait ReactiveTestUtils {
	import scala.react._

	class ListLog[A] {
		private val _log = new scala.collection.mutable.ListBuffer[Any]
		def log(a: A) { _log += a }
		def assert(as: A*) { _log should equal(as) }
	}

	class MsgLog extends ListLog[Option[_]]

	class MockObserver(r: Reactive[_, _])(op: => Unit) extends Observer {
		val currents = new ListLog[Any]
		val messages = new ListLog[Any]
		def run() {
			currents log r.current(this)
			r.message(this) map (messages.log)
			op
		}
	}

	def mockOb(r: Reactive[_, _])(op: => Unit) = {
		val d = new MockObserver(r)(op)
		r.subscribe(d)
		d
	}

	def advanceTime() {
		val dummy = new EventSource[Int]
		dummy emit 0
	}
}
