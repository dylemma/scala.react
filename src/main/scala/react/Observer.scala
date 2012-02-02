/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

object Observer {
	case object Nil extends Observer {
		def run() {}
	}
}

/** An observer is a special node in the dependency graph that has the
  * maximum level and no dependents
  */
trait Observer extends Dependent {
	// Implementation note: max value will keep observers at the end of the 
	// eval queue, i.e., observers are run only after all reactives have 
	// been validated.
	def level = Int.MaxValue
	def dependents = Set.empty

	def receive(eng: Engine) = {
		run()
		Engine.processed(this)
	}
	def run()

	def valid = true

	final private[react] def invalidate() {
		// Observers are leafs in the dep graph, so nothing to do here
	}
	private[react] def clearDependents() = scala.collection.Set.empty

	def dependsOn[A, B](reactive: Dependency[A, B]) {}
}
