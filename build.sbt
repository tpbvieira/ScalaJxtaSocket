name := "ScalaJxtaSocket"

version := "1.0"

organization  := "cin.ufpe"

scalaVersion := "2.9.1"

resolvers += "Local Maven Repository" at "file:///home/thiago/.m2/repository"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "repository.jboss.org" at "http://repository.jboss.org/maven2/"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.2"

libraryDependencies += "com.typesafe.akka" % "akka-testkit" % "2.0.2"

libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0.2"

libraryDependencies += "com.kenai.jxse" % "jxse" % "2.6"

libraryDependencies += "javax.servlet" % "servlet-api" % "2.3"

libraryDependencies += "log4j" % "log4j" % "1.2.13"

libraryDependencies += "org.apache.felix" % "org.apache.felix.main" % "2.0.1"

libraryDependencies += "org.jboss.netty" % "netty" % "3.1.5.GA"

libraryDependencies += "org.apache.derby" % "derby" % "10.5.3.0_1"

libraryDependencies += "com.h2database" % "h2" % "1.1.118"
