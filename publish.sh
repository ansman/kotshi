#!/usr/bin/env bash

./gradlew clean
./gradlew publishAllPublicationsToMavenCentralRepository \
  -PsignArtifacts=true \
  -Psigning.gnupg.executable=/opt/homebrew/bin/gpg \
  -Psigning.gnupg.keyName=$(op --account my.1password.com read op://private/GnuPG/keyID | xargs) \
  -Psigning.gnupg.passphrase=$(op --account my.1password.com read op://private/GnuPG/password | xargs)