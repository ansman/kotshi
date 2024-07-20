#!/usr/bin/env bash

./gradlew clean
PASSPHRASE=$(op read op://private/GnuPG/password | xargs)
./gradlew publishAllPublicationsToMavenCentralRepository -PsignArtifacts=true -Psigning.gnupg.keyName=7A743C13 "-Psigning.gnupg.passphrase=$PASSPHRASE"