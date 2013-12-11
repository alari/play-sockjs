name := "play-sockjs"

organization := "ru.mirari"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
)

publishTo := {
  val artifactory = "http://mvn.quonb.org/artifactory/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("Artifactory Realm" at artifactory + "plugins-snapshot-local/")
  else
    Some("Artifactory Realm" at artifactory + "plugins-release-local/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

play.Project.playScalaSettings

resolvers ++= Seq(
  "quonb" at "http://mvn.quonb.org/artifactory/repo/"
)



testOptions in Test += Tests.Argument("junitxml")

