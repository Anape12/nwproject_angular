#!/usr/bin/env bash
set -euo pipefail
APP_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SOURCE="${JPA_SECURITY_SOURCE:-}"
if [ -z "$SOURCE" ]; then
    if [ -f "$APP_ROOT/../jpa-security/security-core/pom.xml" ]; then
        SOURCE="$APP_ROOT/../jpa-security"
    else
        SOURCE="$APP_ROOT/.build-deps/jpa-security-source"
    fi
fi
if [ ! -f "$SOURCE/security-core/pom.xml" ]; then
    echo 'Private jpa-security checkout is required. Clone it with authorized Git credentials and set JPA_SECURITY_SOURCE, or configure the CI checkout token.' >&2
    exit 1
fi
REPOSITORY="$APP_ROOT/.build-deps/cache"
bash "$APP_ROOT/mvnw" -f "$SOURCE/pom.xml" "-Dmaven.repo.local=$REPOSITORY" -pl security-core -am -DskipTests --batch-mode install
for artifact in jpa-security-parent jpa-security; do
    relative="dev/kobun/$artifact/0.1.0-SNAPSHOT"
    mkdir -p "$APP_ROOT/.build-deps/maven/$relative"
    cp "$REPOSITORY/$relative/$artifact-0.1.0-SNAPSHOT.pom" "$APP_ROOT/.build-deps/maven/$relative/"
    if [ "$artifact" = jpa-security ]; then
        cp "$REPOSITORY/$relative/$artifact-0.1.0-SNAPSHOT.jar" "$APP_ROOT/.build-deps/maven/$relative/"
    fi
done
