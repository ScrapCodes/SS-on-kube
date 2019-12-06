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
ThisBuild / scalaVersion := "2.12.10"
ThisBuild / resolvers    += Resolver.mavenLocal

val sparkVersion = "3.0.0-preview"

lazy val sb = project.in(file(".")).settings(
  // To force the user to run submodule specific run, by doing project submodule.
  (run / aggregate) := false,
  // To turn off packaging for the empty aggregator project.
  publishArtifact :=  false,
  test := {})
  .aggregate(deploy, integrationTests, demo)

lazy val deploy = project
  .in(file("deploy"))
  .settings(
    libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.11.2",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.11.2",
    libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.11.2",
    libraryDependencies += ("io.fabric8" % "kubernetes-client" % "4.6.1"),
    libraryDependencies += ("org.scalatest" %% "scalatest" % "3.0.5" % "test")
  )

lazy val demo = project
  .in(file("demo"))
  .dependsOn(deploy)
  .settings(
    // Since spark 2.4.4 uses okttp 3.8.1, we are forced to use it too.
    libraryDependencies += ("com.squareup.okhttp3" % "okhttp" % "3.8.1"),
    libraryDependencies += ("org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion),
    libraryDependencies += ("org.apache.spark" %% "spark-sql" % sparkVersion),
    libraryDependencies += ("org.scalatest" %% "scalatest" % "3.0.5" % "test"),
    libraryDependencies += ("com.googlecode.json-simple" % "json-simple" % "1.1")
  )

lazy val integrationTests = project
  .in(file("integration-tests"))
  .dependsOn(deploy)
  .settings(
    parallelExecution in Test := false,
    libraryDependencies += ("org.scalatest" %% "scalatest" % "3.0.5" % "test"),
    libraryDependencies += ("com.typesafe.akka" %% "akka-http-testkit" % "10.1.8" % "test"),
    libraryDependencies += ("com.typesafe.akka" %% "akka-stream-testkit" % "2.5.19" % "test")
  )
