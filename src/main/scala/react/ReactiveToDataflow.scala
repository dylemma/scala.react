/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react

import scala.util.continuations._

/** A constructor class for dataflow reactives. The type of resulting reactives is given by type
  * paramter `R`. The local self type of the reactive under construction is given by `DF`.
  */
trait ReactiveToDataflow[M, N, R <: Reactive[M, N], DR <: DataflowReactive[M, N, R]] extends Reactive[M, N] {
	protected def init: R
	protected def make(body: DR => Unit @suspendable): R

	def loop(body: DR => Unit @suspendable): R = make(self => while (!self.isDisposed) { body(self) })
	def once(body: DR => Unit @suspendable): R = make(body)

	protected[react] def _value = init._value
	def subscribe(dep: Dependent) = init.subscribe(dep)
	override def current(dep: Dependent): N = init.current(dep)
	protected[react] def message(dep: Dependent): Option[M] = init.message(dep)
}
