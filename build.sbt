/*
 *
 * Streaming Benchmark
 *
 * Copyright IBM.
 *
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

ThisBuild / organization := "org.codait"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / resolvers    += Resolver.mavenLocal

lazy val sb = project.in(file(".")).settings(
  // To force the user to run submodule specific run, by doing project submodule.
  (run / aggregate) := false,
  // To turn off packaging for the empty aggregator project.
  publishArtifact :=  false,
  test := {})
  .aggregate(deploy, bench, integrationTests)

lazy val deploy = project
  .in(file("deploy"))
  .settings(
    libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.11.2",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.11.2",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.2",
    libraryDependencies += ("io.fabric8" % "kubernetes-client" % "4.1.x"),
    libraryDependencies += ("org.scalatest" %% "scalatest" % "3.0.5" % "test")
  )

lazy val bench = project
  .in(file("bench"))
  .settings(
      libraryDependencies += ("org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.3"),
      libraryDependencies += ("org.apache.spark" %% "spark-sql" % "2.4.3"),
      libraryDependencies += ("org.scalatest" %% "scalatest" % "3.0.5" % "test")
  )

lazy val integrationTests = project
  .in(file("integration-tests"))
  .dependsOn(deploy, bench)
  .settings(
    libraryDependencies += ("org.scalatest" %% "scalatest" % "3.0.5" % "test")
  )