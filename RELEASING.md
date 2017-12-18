1. `git pull origin`
2. Change the version in `gradle.properties` to a non-SNAPSHOT version.
3. Update the `CHANGELOG.md` for the impending release.
4. Update the `README.md` with the new version.
5. `git commit -am "Prepare for release X.Y.Z"` (where X.Y.Z is the new version)
6. `git push origin`
7. `./gradlew clean bintrayUpload`.
8. Visit the [api artifact](https://bintray.com/ansman/kotshi/api#central) and [compiler artifact](https://bintray.com/ansman/kotshi/compiler#central) and publish to maven central.
9. Release on GitHub
10. Update the `gradle.properties` to the next SNAPSHOT version.
11. `git commit -am "Prepare next development version"`
12. `git push origin && git push origin --tags`
