/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

case class Val[+A](protected[react] val _value: A) extends Signal[A] with Reactive.Nil[A, A] {
	def subscribe(dep: Dependent) = dep.dependsOn(this)
	override def now: A = _value
	override def apply(): A = _value
	override def cached: Signal[A] = this
	override def changes: Events[A] = Events.Never
}
