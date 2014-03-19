#!/bin/bash

if [ -f environment.sh ]; then
  source environment.sh
fi

lein ring server-headless
