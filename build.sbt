name := "ifttt-salesforce"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

resolvers += "rediscala" at "http://dl.bintray.com/etaty/maven"

libraryDependencies ++= Seq(
  ws,
  "com.etaty.rediscala" %% "rediscala" % "1.3.1",
  "org.webjars" %% "webjars-play" % "2.3.0",
  "org.webjars" % "bootstrap" % "3.2.0"
)

enablePlugins(ForcePlugin)

username in Force := sys.env.get("SALESFORCE_USERNAME").getOrElse("")

password in Force := sys.env.get("SALESFORCE_PASSWORD").getOrElse("")

packagedComponents in Force := Seq("com.salesforce.ifttt")
