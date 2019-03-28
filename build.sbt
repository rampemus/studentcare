// Project template

// Supported operating systems: Windows, Mac, Linux
// Supported JDKs: all except Java 9

// For Java versions 10+, the script will fetch JavaFX SDK OS libraries
// and store in projectDirectory / openjfx
//
// Please set up environment variable JAVAFX_HOME if you'd want to reuse
// the same SDK libraries for multiple projects.

// Project name
name := "studentcare"

// organization name
organization := "fi.utu"

version := "1.0"

// project description
description := "Simple test template for java / javafx projects"

// main class
Compile/mainClass := Some("org.utu.studentcare.Main")

// force the java version by typing it here (remove the comment)
val force_javaVersion = None // Some(11)

// force the javafx version by typing it here (remove the comment)
val force_javaFxVersion = None // Some(11)

// END_OF_SIMPLE_CONFIGURATION
// you can copy the rest for each new project
// --- --- ---

def fail(msg: String) = {
  println("Error :-/")
  println
  println(msg)
  System.exit(1)
  null
}

// the script will automatically pick the best libraries for you
val javaVersionNum =
  force_javaVersion getOrElse System.getProperty("java.version").split('.').dropWhile(_.toInt<8).head.toInt

val javaVersionString = javaVersionNum match {
  case 7 => "1.7"
  case 8 => "1.8"
  case 9 => "9"
  case 10 => "10"
  case 11 => "11"
  case 12 => "12"
  case x if x > 12 => println("Using unsupported beta version of Java!"); x.toString
  case x if force_javaVersion.isEmpty => fail("Your Java JDK version ["+x+"] is unsupported! Try upgrading to Java 11 LTS.")
  case x => fail("The requested Java JDK version ["+x+"] is unsupported! Try upgrading to Java 11 LTS and replace the force_javaVersion value with None in build.sbt.")
}

javacOptions ++= Seq("-source", javaVersionString, "-target", javaVersionString, "-encoding", "utf8", "-Xlint:unchecked")

enablePlugins(JShellPlugin)

compileOrder := CompileOrder.JavaThenScala

// Enables publishing to maven repo
publishMavenStyle := true

// Do not append Scala versions to the generated artifacts
crossPaths := false

// This forbids including Scala related libraries into the dependency
autoScalaLibrary := false

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

// contains non-broken cofoja & oomkit libraries
resolvers += "utujemma" at "http://users.utu.fi/jmjmak/repository/"


//// JQWIK / JUNIT configuration

// library dependencies. (orginization name) % (project name) % (version)
libraryDependencies ++= Seq(
  "net.aichler"        % "jupiter-interface"              % JupiterKeys.jupiterVersion.value % Test,
  "org.junit.platform" % "junit-platform-commons"         % "1.4.1" % Test,
  "org.junit.platform" % "junit-platform-runner"          % "1.4.1" % Test,
  "org.junit.jupiter"  % "junit-jupiter-engine"           % "5.4.1" % Test,
  "org.junit.jupiter"  % "junit-jupiter-api"              % "5.4.1" % Test,
  "org.junit.jupiter"  % "junit-jupiter-migrationsupport" % "5.4.1" % Test,
  "org.junit.jupiter"  % "junit-jupiter-params"           % "5.4.1" % Test,
  "net.jqwik"          % "jqwik"                          % "1.1.1" % Test,
  "org.scalatest"      %% "scalatest"                     % "3.0.5" % Test
  //"io.cucumber"        % "cucumber-junit"                 % "4.2.2", does not support junit 5 yet
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-c")

fork in Global := true


//// JAVAFX configuration

val javafx_versions = force_javaFxVersion getOrElse (javaVersionNum match {
  case 7 => (7, "7", "8.0.181-R13")
  case 8 => (8, "8", "8.0.181-R13")
  case 10 | 11 => (11, "11.0.2", "11-R16")
  case 12 => (12, "12", "11-R16")
  case 13 => (12, "12", "11-R16")
  case ver if force_javaFxVersion.isEmpty => fail("The JavaFX version ["+ver+"] derived from your Java SDK version ["+javaVersionString+"] is not supported. Try using 7/8/10/11/12 instead.")
  case ver => fail("The JavaFX version you had defined ["+ver+"] is not supported. Try using 7/8/10/11/12 instead.")
})

val jfx_sdk_version = javafx_versions._2
val jfx_scalafx_version = javafx_versions._3

// JAVA_HOME location
val javaHomeDir = {
  val path = try {
    scala.sys.env("JAVA_HOME")
  } catch {
    case _: Throwable => System.getProperty("java.home") // not set -> ask from current JVM
  }

  val f = file(path)
  if (!f.exists()) fail("Currently the environment variable JAVA_HOME points to a non-existent directory! You need to fix this in order to compile stuff.")
  f
}

val osName: SettingKey[String] = SettingKey[String]("osName")

osName := (System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
})

def legacyJavaFX(jfxVersion: Int, badVersions: Seq[Int]) = {
  val searchDirs = Seq(
    "/jre/lib/jfxrt.jar", // OpenJDK 7
    "/jre/lib/ext/jfxrt.jar", // OpenJDK 8
    "/lib/ext/jfxrt.jar" // Windows & Oracle Java 8
  )

  val javaFxJAR = searchDirs.map{ searchDir => file(javaHomeDir + searchDir) }.find{ _.exists() }

  javaFxJAR.getOrElse {
    val p = javaHomeDir.toString
    fail("Java FX runtime not installed in [" + p + "]!" +
      (if (badVersions.exists { v => p.contains("jdk-" + v) }) " Did you try to run JavaFX " + jfxVersion + " with JDK " + badVersions.sorted.mkString("/") + "?" else "")
    )
  }
}

val javaFxPath = Def.taskKey[File]("OpenJFX fetcher")
javaFxPath := {
  val javaFxHome =
    try {
      val envHome = file(scala.sys.env("JAVAFX_HOME"))
      println("Using OpenJFX from " + envHome)
      envHome
    }
    catch { case _: Throwable =>
        println("Using local OpenJFX")
        baseDirectory.value / "openjfx"
    }
    
  if (!javaFxHome.exists()) java.nio.file.Files.createDirectory(javaFxHome.toPath)

  val jfx_os = osName.value match {
    case "linux" => "linux"
    case "mac"   => "osx"
    case "win"   => "windows"
  }

  val sdkURL = "http://download2.gluonhq.com/openjfx/" + jfx_sdk_version + "/openjfx-" + jfx_sdk_version + "_" + jfx_os + "-x64_bin-sdk.zip"

  try {
    val testDir = javaFxHome / "all.ok"
    if (!testDir.exists()) {
      println("Fetching OpenJFX from "+sdkURL+"..")
      IO.unzipURL(new URL(sdkURL), javaFxHome)
      java.nio.file.Files.createDirectory(testDir.toPath)
      println("Fetching OpenJFX done.")
    } else {
      println("Using OpenJFX from "+javaFxHome)
    }

    javaFxHome
  }
  catch {
    case t: Throwable => fail("Could not load OpenJFX! Reason:" + t.getMessage)
  }
}

javafx_versions._1 match {
  case 7 =>
    // TODO libraryDependencies
    Seq(unmanagedJars in Compile += Attributed.blank(legacyJavaFX(8, Seq(10, 11, 12, 13, 8))))
  case 8 =>
    Seq(
      libraryDependencies += "org.scalafx" %% "scalafx" % jfx_scalafx_version,
      unmanagedJars in Compile += Attributed.blank(legacyJavaFX(8, Seq(10, 11, 12, 13, 7))),
    )
  case 10 | 11 | 12 | 13 =>
    Seq(
      javaOptions in run ++= Seq(
        "--module-path", (javaFxPath.value / ("javafx-sdk-" + jfx_sdk_version) / "lib").toString,
        "--add-modules=javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web"),

      libraryDependencies ++= Seq(
        "org.scalafx" % "scalafx_2.12" % jfx_scalafx_version,
        "org.openjfx" % "javafx-base" % jfx_sdk_version classifier osName.value,
        "org.openjfx" % "javafx-controls" % jfx_sdk_version classifier osName.value,
        "org.openjfx" % "javafx-fxml" % jfx_sdk_version classifier osName.value,
        "org.openjfx" % "javafx-graphics" % jfx_sdk_version classifier osName.value,
        "org.openjfx" % "javafx-media" % jfx_sdk_version classifier osName.value,
        "org.openjfx" % "javafx-swing" % jfx_sdk_version classifier osName.value,
        "org.openjfx" % "javafx-web" % jfx_sdk_version classifier osName.value
      )
    )
}

import xml.transform.{RewriteRule, RuleTransformer}
import xml.{Node,NodeSeq,Elem}

pomPostProcess := {
  def rule(f: PartialFunction[Node, NodeSeq]): RewriteRule = new RewriteRule {
    override def transform(n: Node) = if (f.isDefinedAt(n)) f(n) else n
  }

  def depName(e: Elem) =
    Seq(e).filter(_.label == "dependency").flatMap(_.child).filter{_.label == "artifactId" }.flatMap(_.child).mkString

  def toolVersions =
    <properties>
      <maven.compiler.source>{javaVersionString}</maven.compiler.source>
      <maven.compiler.target>{javaVersionString}</maven.compiler.target>
    </properties>

  new RuleTransformer(rule {
    case e: Elem if depName(e) == "jupiter-interface" => println("Skipped "+depName(e)); NodeSeq.Empty
    case e: Elem if e.label == "organization" => NodeSeq.seqToNodeSeq(Seq(e, toolVersions))
  })
}

val javaVersion = taskKey[Unit]("Prints the Java version.")

javaVersion := { println("SBT uses Java SDK located at "+System.getProperty("java.home")) }

val netbeans = taskKey[Unit]("Makes a Netbeans compatible pom.xml.")
val eclipse  = taskKey[Unit]("Makes a Eclipse compatible pom.xml.")

netbeans := {
  println("Tehdään Netbeans-yhteensopiva pom.xml!")
  IO.copyFile(makePom.value, file("pom.xml"))
}

eclipse := {
  println("Tehdään Eclipse-yhteensopiva pom.xml!")
  IO.copyFile(makePom.value, file("pom.xml"))
}

publishTo := Some(Resolver.file("file", new File("/tmp/repository")))
