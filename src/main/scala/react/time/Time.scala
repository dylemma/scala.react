/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package scala.react
package time

// TODO: maybe use Lex's unit package?
class Time(protected val rep: Double) extends Ordered[Time] {
	def +(that: Time) = new Time(rep + that.rep)
	def -(that: Time) = new Time(rep - that.rep)
	def /(that: Time) = this.rep / that.rep
	def /(x: Double) = new Time(rep / x)

	def compare(that: Time) = math.signum(that.rep - this.rep).toInt

	def toDays = rep / days2Secs
	def toHours = rep / hours2Secs
	def toMinutes = rep / mins2Secs
	def toSeconds = rep
	def toMilliSeconds = rep / msecs2Secs
	def toNanoSeconds = rep / nsecs2Secs

	override def toString = rep + " secs"
}
