/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

/** A heavyweight signal that supplies all necessary infrastructure for signals that cache their
  * value and level, that can be invalidated, and maintain a set of dependents. Clients need to
  * implement method `evaluateAndConnect()`.
  */
abstract class ValueCachingSignal[A]
	extends Signal[A]
	with Dependency[A, A]
	with MessageCachingReactive[A, A]
	with InvalidatableDependent
	with LevelCachingDependent
	with Publisher[A, A] { outer =>
	protected[react] var _value: A = _

	override def current(dep: Dependent) = {
		checkLevelNow()
		subscribe(dep)
		validate()
		_value
	}

	/** Update the value of this signal and connect to dependencies.
	  */
	@throws(classOf[LevelMismatchNow]) protected def evaluateAndConnect()

	protected def validate() = if (!valid) forceValidation()

	protected def forceValidation() {
		// we always need to connect, otherwise we we don't get invalidated when a dependency changes
		// and a later call to `now` might return an outdated value
		evaluateAndConnect()
		_valid = true
	}

	def receive(eng: Engine) {
		val oldValue = _value
		try {
			// always force a validation, as dataflow reactive might want switch to a different state, 
			// even if nobody is interested.
			forceValidation()
			// cap propagation if possible, but mark this as processed in any case:
			if (oldValue != _value) {
				Engine.setMessage(this, _value)
				eng.invalidate(clearDependents())
			} else {
				Engine.processed(this)
			}
		} catch {
			case e @ LevelMismatchNow(dep, _) =>
				_level = dep.level + 1
				// revert to old value, so that we don't drop event spuriosly
				_value = oldValue
				Engine lift this
		}
	}

	override def cached: Signal[A] = this
}