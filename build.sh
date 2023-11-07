#!/bin/bash

DIR=(dirname $0)
pushd "$DIR/client" > /dev/null
pnpm install
pnpm run build
mkdir -p ../server/src/main/resources/web
cp dist/index.html ../server/src/main/resources/web/
cd ../server
./gradlew nativeCompile
cp build/native/nativeCompile/hvm ../
popd > /dev/null
