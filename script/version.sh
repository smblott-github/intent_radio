#!/bin/sh

sed -n '/name="version/ { s/^[^>]*>//; s/<\/string.*//; p }' "$@" \
   | tr '\n' '-' \
   | sed 's/-$//'
