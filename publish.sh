#!/usr/bin/env bash

./gradlew clean
./gradlew publishAllPublicationsToMavenCentralRepository -PsignArtifacts=true --no-parallel