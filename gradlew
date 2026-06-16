#!/bin/sh
# Gradle wrapper script for non-Windows

# Resolve paths
GRADLE_WRAPPER_JAR="$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar"

if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

exec "$JAVA" -Xmx64m -Xms64m \
  -Dorg.gradle.appname=gradlew \
  -classpath "$GRADLE_WRAPPER_JAR" \
  org.gradle.wrapper.GradleWrapperMain "$@"
