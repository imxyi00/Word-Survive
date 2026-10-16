#!/bin/sh
cd "$(dirname "$0")" || exit 1
rm -rf build && mkdir build
javac --release 17 -encoding UTF-8 -d build src/*.java || exit 1
jar cfe WordSurvive.jar wordsurvive.Main -C build .
rm -rf build
echo "Built WordSurvive.jar"
