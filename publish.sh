#!/usr/bin/env bash

./gradlew clean
PASSPHRASE=$(op read op://private/GnuPG/password | xargs)
./gradlew publishAllPublicationsToMavenCentralRepository -PsignArtifacts=true "-Psigning.gnupg.passphrase=$PASSPHRASE" --no-parallel --no-configuration-cache