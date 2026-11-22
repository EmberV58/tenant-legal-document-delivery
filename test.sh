#!/bin/sh
set -eu
BUILD_DIR="${TMPDIR:-/tmp}/tenant-legal-delivery-test-classes"
mkdir -p "$BUILD_DIR"
javac -d "$BUILD_DIR" $(find src/main/java src/test/java -name '*.java')
java -cp "$BUILD_DIR" dev.infrai.legaltech.workflow.SignedDocumentDeliveryTest
