#!/bin/bash

# Il est également possible d'avoir les fichiers en SVG, pour ça : FORMAT="svg"
FORMAT="png"

rm -rf output
mkdir -p output

for file in $(ls *.puml); do
 cat ${file} | docker run --rm -v ${PWD}/.common.puml:/.common.puml -i think/plantuml -charset UTF-8 -t${FORMAT} > output/${file%.puml}.${FORMAT}
done
