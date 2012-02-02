name := "scalaReact"

version := "1.0"

scalaVersion := "2.9.1"

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.1")

scalacOptions ++= Seq(
	"-deprecation",
	"-unchecked",
	"-P:continuations:enable"
)

libraryDependencies ++= Seq(
	"org.scalatest" %% "scalatest" % "1.6.1" % "test",
	"junit" % "junit" % "4.10" % "test"
)