#!/bin/sh

MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"

if [ ! -r "$WRAPPER_JAR" ]; then
  echo "Missing Maven wrapper JAR. Please run mvnw.cmd on Windows or download wrapper jar." 1>&2
  exit 1
fi

JAVA_EXE="java"
exec "$JAVA_EXE" -classpath "$WRAPPER_JAR" -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
