/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

/** Indicates a mismatch between the expected level of a reactive dependency and
  * the actual one.
  */
case class LevelMismatchNow(dependency: DependencyNode, level: Int) extends Exception {
	// fast exception: don't generate stack trace 
	//override def fillInStackTrace() = this
	override def getMessage =
		"Level of dependency " + dbgInfo(dependency) + ": " + dependency.level + " was >= level of acessor: " + level
}

case class LevelMismatchPrevious(dependency: DependencyNode, level: Int) extends Exception {
	// fast exception: don't generate stack trace 
	//override def fillInStackTrace() = this
	override def getMessage =
		"Level of dependency " + dbgInfo(dependency) + ": " + dependency.level + " was < level of acessor: " + level
}
