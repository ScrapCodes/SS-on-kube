val dottyVersion = "0.11.0-RC1"
val scala212Version = "2.12.8"

lazy val root = project
  .in(file("."))
  .settings(
    name := "streaming-benchmark",
    version := "0.1.0", 
    resolvers += Resolver.mavenLocal,
    libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
    libraryDependencies += ("org.apache.spark" %% "spark-sql-kafka-0-10" % "2.4.0"),
    libraryDependencies += ("org.apache.spark" %% "spark-sql" % "2.4.0"),
    libraryDependencies += ("io.fabric8" % "kubernetes-client" % "4.1.x"),
    libraryDependencies += ("org.scalatest" %% "scalatest" % "3.0.5" % "test"),
    // updateOptions := updateOptions.value.withLatestSnapshots(false),
    scalaVersion := scala212Version
  )
