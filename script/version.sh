#!/bin/sh

sed -n '/name="version/ { s/^[^>]*>//; s/<\/.*//; p }' "$@" \
   | tr '\n' '-' \
   | sed 's/-$//'
