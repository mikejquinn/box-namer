#!/bin/sh

if [ -f environment.sh ]; then
  source environment.sh
fi

if [ "$RING_ENV" = "production" ]; then
  java -jar target/box-namer-0.1.0-SNAPSHOT-standalone.jar
else
  lein ring server-headless
fi
