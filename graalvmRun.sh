#!/bin/bash
SCRIPT_DIRECTORY="$(dirname -- "$( readlink -f -- "$0"; )")"

cd "$SCRIPT_DIRECTORY"/build/libs || exit
FIRST_JAR=$(find . -name '*.jar' | head -n 1)
java -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar "$FIRST_JAR"
cd "$OLDPWD"