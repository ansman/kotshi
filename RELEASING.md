1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
2. Update the `CHANGELOG.md` for the impending release.
3. Update the `README.md` with the new version.
4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
5. `./gradlew clean bintrayUpload`.
6. Visit the [api artifact](https://bintray.com/ansman/kotshi/api#central) and [compiler artifact](https://bintray.com/ansman/kotshi/compiler#central) and publish to maven central.
7. Release on GitHub
8. Update the `gradle.properties` to the next SNAPSHOT version.
9. `git commit -am "Prepare next development version"`
10. `git push && git push --tags`