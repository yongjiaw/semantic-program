ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.2"

lazy val root = (project in file("."))
  .settings(
    name := "CognitiveKernel",
    libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.18" % Test),
    Test / testOptions += Tests.Argument("-oD"),
    // Soar SML JARs (unmanaged, copied to lib/), currently there is no mvn release
    // the following will work with local build and breaks CCI
    // Compile / unmanagedJars += Attributed.blank(file("../Soar/out/java/sml.jar"))
  )