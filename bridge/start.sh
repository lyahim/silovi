#!/bin/sh
# ----------------------------------------------------------------------------
# SiLoVi bridge start script
# ----------------------------------------------------------------------------

# set JAVA_HOME if needed
# export JAVA_HOME=

java -jar ./bin/silovi-bridge.jar --spring.config.location=file:config.properties > /dev/null 2>&1 &