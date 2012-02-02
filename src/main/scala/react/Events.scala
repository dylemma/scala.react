/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

object Events {
	/** An event stream that never emits.
	  */
	case object Never extends Events[Nothing] with Reactive.Nil[Nothing, Unit] {
		override def subscribe(dep: Dependent) {}
		override def map[B](f: Nothing => B): Events[B] = this
		override def collect[B](p: PartialFunction[Nothing, B]): Events[B] = this
		override def filter(p: Nothing => Boolean): Events[Nothing] = this
		override def when(s: Signal[Boolean]): Events[Nothing] = this
		override def tag[B](v: => B) = this
	}

	/** An event stream that emits the given value only once, right now. It is safe to
	  * create an instance of this class in the middle of a propagation cycle and assume that it
	  * immediately emits an event.
	  */
	final case class Now[A](a: A) extends Events[A] with MessageCachingReactive[A, Unit] with Dependency[A, Unit] {
		cacheMessage(a)

		def valid = true
		// Level == 0 should be safe, since we never enter this stream into the propagation queue
		def level = 0
		def clearDependents() = NoDependents
		def dependents = NoDependents
		override def subscribe(dep: Dependent) { dep.dependsOn(this) }
	}

	import scala.util.continuations._

	def loop[A](code: DataflowEvents[A] => Unit @suspendable): Events[A] = new DataflowEvents[A](Never) {
		def body() = while (!this.isDisposed) { code(this) }
	}

	def once[A](code: DataflowEvents[A] => Unit @suspendable): Events[A] = new DataflowEvents[A](Never) {
		def body() = code(this)
	}
}

/** A stream that emits event messages at discrete times.
  * The discrete counterpart of a `Signal`. We say an event stream ''a''
  * ''contains'' the same events as an event stream ''b'' if ''b'' emits
  * the same messages as ''a'' at the same times.
  */
trait Events[+A] extends Reactive[A, Unit] { outer =>
	override protected[react] def _value = ()
	override def toEvents = this

	/** An event stream that contains all events from this stream, applied to the given function.
	  *
	  * Equivalent to `collect { case x => f(x) }`
	  *
	  * {{{ e!x@t <=> (e map f)!f(x)@t }}}
	  */
	def map[B](f: A => B): Events[B] = Events.loop[B] { self => self emit f(self next outer) }

	/** A partial `map` combinator. An event stream that contains all events from this stream,
	  * applied to the given partial function. Events for which the given function is not defined
	  * are discarded from the resulting stream.
	  *
	  * {{{ e!x@t <=> (e map pf)!pf(x)@t }}}
	  */
	def collect[B](p: PartialFunction[A, B]): Events[B] = Events.loop[B] { self =>
		val x = self next outer
		if (p isDefinedAt x) self emit p(x)
		else self.delay
	}

	/** A stream that contains all events of this stream for which the given predicate is `true`.
	  *
	  * Equivalent to `collect { case x if p(x) => x }`
	  */
	def filter(p: A => Boolean): Events[A] = collect { case x if p(x) => x }

	/** A stream that contains all events of this stream
	  *
	  * Equivalent to `filter(x => s.now)`
	  */
	def when(s: Signal[Boolean]): Events[A] = filter(x => s.now)

	/** A `map` whose function is independent of the source event.
	  * The resulting stream contains all event ocurrances of this stream.
	  *
	  * Equivalent to `map { a => v }`
	  */
	def tag[B](v: => B) = map { a => v }

	/** Returns a signal that holds the last event `e` from `this` stream beginning
	  * ''at'' the moment `e` was fired. Most straightforward conversion from an
	  * event stream to a signal. Dual of `Signal.changes`.
	  *
	  * The following relations are true:
	  * {{{
	  *  signal.changes.hold(x) = signal
	  *  events.hold(x).changes = events
	  * }}}
	  */
	def hold[B >: A](init: B): Signal[B] = Val(init) loop { self => self emit (self next outer) }

	/** Converts a stream of streams into a flat stream that behaves like the stream most recently
	  * fired by `this` stream. Initially starts with `Events.Never`.
	  * Also known as one of the ''switch'' combinators.
	  */
	def flatten[B](implicit witness: A => Events[B]): Events[B] =
		map(witness).hold(Events.Never).flatten

	/** A signal that indicates whether this stream has emitted an event starting from the
	  * current cycle.
	  */
	def happened: Signal[Boolean] = Val(false) once { self =>
		self next outer
		self emit true
	}

	/** A signal that indicates the number of events this stream has emitted starting with
	  * the current cycle.
	  */
	def count: Signal[Int] = Val(0) loop { self =>
		self next outer
		self emit self.previous + 1
	}

	/** Asymmetrically merges two event streams, i.e., emits all events from `this` and `that` stream
	  * except events in `that` stream that occur simultaneously with events from `this` stream
	  * (those events are dropped).
	  */
	def merge[B >: A](that: Events[B]): Events[B] = new Events[B] {
		def subscribe(dep: Dependent) {
			outer.subscribe(dep)
			that.subscribe(dep)
		}
		def message(dep: Dependent) = {
			val x = outer.message(dep)
			if (x != None) x
			else that.message(dep)
		}
	}

	/** An event stream that contains precisely those events of `this` stream for which
	  * `that` does not emit a simultaneous event.
	  */
	def not(that: Events[_]): Events[A] =
		this filter (x => that.message(Observer.Nil) == None)

	/** An event stream that contains precisely those events of `this` stream for which `that`
	  * emits a simultaneous event.
	  *
	  * TODO could emit tuples
	  */
	def and(that: Events[_]): Events[A] =
		this filter (x => that.message(Observer.Nil) != None)

	def unless(that: Events[_]): Events[A] =
		this when Signal(!that.happened())

	/** An event stream that emits the first n event from this stream, starting with the event
	  * during this cycle, if present.
	  */
	def take(n: Int): Events[A] = Events.once[A] { self =>
		var x = 0
		while (x < n) {
			self emit (self next outer)
			x += 1
		}
	}

	/** Switches between two given signals. The resulting signal has two partitions:
	  * `before` before this stream emits and event and `after` after this stream emitted an event.
	  * If this event is raising at creation time, the resulting signal will only have partition
	  * `after`.
	  *
	  * TODO: test
	  */
	def switch[B](before: Signal[B], after: => Signal[B]): Signal[B] = before once { self =>
		self next outer
		self switchTo after
	}

	/** A dual of `Signal.changes`. Every cycle this stream emits, the given call-by-name argument
	  * is evaluated and the signal returned by this method changes its value to the result of that
	  * evaluation.
	  */
	def sample[B](value: => B): Signal[B] = this tag { value } hold value
}
