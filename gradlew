#!/usr/bin/env sh
# Gradle Wrapper script for Unix-like environments
DIR=$(cd "$(dirname "$0")" && pwd)
${JAVA_HOME:-java} -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
