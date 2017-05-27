name := """movie-ticket-reservation-system"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14",
  "org.mockito" % "mockito-core" % "1.9.0" % "test",
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.0-RC2" % "test"
)

coverageExcludedPackages := "<empty>;Reverse.*;app.Routes.*;main.Routes;testOnlyDoNotUseInAppConf.*;forms.*;config.*;models.*;views.html.*;main.*;repositories.*"
