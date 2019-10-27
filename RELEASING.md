1. `git pull origin`
2. Change the version in `gradle.properties` to a non-snapshot version.
3. Update the `README.md` with the new version.
4. `git commit -am "Prepare for release X.Y.Z"` (where X.Y.Z is the new version)
5. `git push origin`
6. `./gradlew clean bintrayUpload`.
7. Visit the [api artifact](https://bintray.com/ansman/kotshi/api#central) and [compiler artifact](https://bintray.com/ansman/kotshi/compiler#central) and publish to maven central.
8. Release on GitHub
9. Update the `gradle.properties` to the next SNAPSHOT version.
10. `git commit -am "Prepare next development version"`
11. `git push origin && git push origin --tags`
