#!/bin/sh

< ./AndroidManifest.xml \
   sed -n '/android:version/ {s/.*="//; s/".*//; p}' \
   | tr '\n' '-' \
   | sed 's/-$//'
