scala.react
===========

This project is a port of [Ingo Maier's](http://lampwww.epfl.ch/~imaier/) implementation of scala.react, found in the paper, [Deprecating the Observer Pattern](http://lampwww.epfl.ch/~imaier/pub/DeprecatingObserversTR2010.pdf)

Changes
-------

 * Ported to [SBT](https://github.com/harrah/xsbt/wiki)
 * Dependencies are now managed
 * Updated scalatest code to a newer version
 * Auto-formatting

Usage
-----

To use `scala.react` with your sbt project, add the following resolver to your `build.sbt` definition.

    resolvers += "Dylemma's Repository" at "http://dylemma.github.com/scala.react/m2/releases"

Then, include the project as a library dependency. If you are using `scalaVersion := "2.9.1"`, you can type

    libraryDependencies += "scala" %% "scala-react" % "1.0"

Otherwise you have to specify the scala version:

    libraryDependencies += "scala" % "scala-react_2.9.1" % "1.0"
	
Developers
----------

This project is configured to use [sbteclipse](https://github.com/typesafehub/sbteclipse) to work within Eclipse's Scala IDE. When running SBT, enter `eclipse` to generate the necessary Eclipse project files.

Disclaimer
----------

The code is largely as I found it. I didn't modify any of the logic or data structures. My only intention for this project was to make it more easily available. I'm no scala expert and this is some fairly advanced stuff written by someone else. As such, I don't intend to try fixing any bugs that pop up.