#!/bin/bash
# Gradle wrapper script for non-Windows

WRAPPER_JAR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-8.2-bin/*/gradle-8.2/lib/gradle-launcher-8.2.jar"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

if [ -z "$JAVA_HOME" ]; then
  JAVA_CMD="java"
else
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

exec $JAVA_CMD $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  -Dorg.gradle.appname=gradlew \
  -jar gradle/wrapper/gradle-wrapper.jar "$@"
