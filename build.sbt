name := """imdb-mcp"""
organization := "com.dti"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala, ScalafmtPlugin)

scalaVersion := "2.13.17"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test

Compile / compile := (Compile / compile).dependsOn(scalafmtAll).value
