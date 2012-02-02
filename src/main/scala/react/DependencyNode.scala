/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

/** A node in the reactive dependency graph which knows about the its dependents
  * (usually reactives and observers) and its topological dependency level.
  */
trait DependencyNode {
	/** The topological dependency level of this reactive. The maximum number
	  * of `Dependencies` on any graph from this reactive to a reactive source,
	  * not counting the source but this reactive ''if it's not a source''.
	  */
	def level: Int

	protected def checkLevelNow() {
		if (level >= Engine.level) throw LevelMismatchNow(this, Engine.level)
	}

	protected def checkLevelPrevious() {
		if (Engine.evaluating && level < Engine.level) throw LevelMismatchPrevious(this, Engine.level)
	}

	/** Reactives and observers that depend on this node
	  */
	def dependents: Dependents

	private[react] def clearDependents(): Dependents

	def valid: Boolean
}

trait Dependency[+A, +B] extends Reactive[A, B] with DependencyNode
