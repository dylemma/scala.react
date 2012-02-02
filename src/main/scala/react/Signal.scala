/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

object Signal {
	private[react] val dependentStack = {
		import scala.collection.mutable._
		val stack = new ArrayStack[Dependent]
		stack push Observer.Nil
		val tls = new ThreadLocal[ArrayStack[Dependent]]
		tls set stack
		tls
	}

	// Push the given dependent onto the stack to let reactives ultimately 
	// add it to their dependents.
	// Note: won't create a new closure class for op, if the given op is already a 
	// call-by-name argument in the caller.
	private[react] def withDep[A](dep: Dependent)(op: => A): A = {
		// do the dep stack magic here:
		// TODO: optimize: check for Dependent.Nil
		val stack = dependentStack.get
		stack push dep
		// eval and let reactives consulted in `op` obtain our dependent
		val v = op
		stack.pop
		v
	}

	def apply[A](op: => A) = logCreation(new PermeableSignal(op))

	class PermeableSignal[A](op: => A) extends Signal[A] {
		protected[react] def _value = now
		// we want to shortcut chains of light signals, i.e., keep your fingers off of the dep stack!
		override def apply() = op
		override def current(dep: Dependent) = withDep(dep)(op)
		def subscribe(dep: Dependent) = current(dep)
		protected[react] def message(dep: Dependent): Option[A] = Some(current(dep))
		// don't hold on to this signal, use the op directly
		override def cached: Signal[A] = new CachedSignal(op)
		override def changes: Events[A] = cached.changes
	}
}

/** Represents time-varying values. The base class of a continuous reactive, i.e.,
  * it has a value at any point in time. Compare `Events` or `Future`.
  *
  * Subclasses generally need to implement methods `current` and `subscribe` but
  * may want to override others for efficiency reasons; or to perform carefully
  * devised implementation tricks. See `DefSignal` for an example of the latter.
  *
  * Two signals are said to ''behave'' the same when they hold the same values at the
  * same time throughout their life time. Some signals are said to be ''partitioned'' into signals
  * `p0` ... `pn` if they behave like `p0` to `pn` at different, disjoint periods of time.
  */
trait Signal[+A] extends Reactive[A, A] { outer =>
	/** May only be used inside signal expressions.
	  * Returns this signal's current value and adds it as a dependency to the
	  * currently evaluating signal.
	  *
	  * Example:
	  * {{{
	  * def sum(a: Signal[Int], b: Signal[Int]) = Signal {
	  *   // we are in a signal expression now
	  *   // let this new signal depend on a and b:
	  *   a() + b()
	  * }
	  * }}}
	  *
	  * Implementation note: the default implementation is mostly suitable for
	  * caching signals.
	  */
	def apply(): A = current(Signal.dependentStack.get.top)

	/** Returns a caching version of `this` signal. The resulting signal behaves the
	  * same as `this` signal and propagates messages only when it really changes.
	  */
	def cached: Signal[A]

	/** Flattens a `Signal` of `R`'s with `R <: Reactive` to a plain `R` that behaves exactly like
	  * the `R` currently held by this signal.
	  *
	  * The given implicit witness ensures that we can obtain a signal of type `Signal[R]` and
	  * that `R` supports the reactive data-flow language.
	  */
	def flatten[M, N, R <: Reactive[M, N], DR <: DataflowReactive[M, N, R]](implicit df: A => R with ReactiveToDataflow[M, N, R, DR]): R =
		df(now) loop { self => self switchTo df(self next this) }

	/** Asymmetric merge of two signals.
	  * Equivalent to `(this.changes merge this.changes) hold this.now`.
	  */
	def merge[B >: A](that: Signal[B]): Signal[B] = (this.changes merge this.changes) hold this.now

	/** Alias for `toEvents`.
	  */
	def changes: Events[A] = toEvents
	override def toSignal = this
}