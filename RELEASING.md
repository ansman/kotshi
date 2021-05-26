1. `git pull origin`
2. Change the version in `gradle.properties` to a non-snapshot version.
3. Update the `README.md` with the new version.
4. `git commit -am "Prepare for release X.Y.Z"` (where X.Y.Z is the new version)
5. `git push origin`
6. `./gradlew clean publishAllPublicationsToMavenCentralRepository`.
7. Close and release on [Sonatype](https://oss.sonatype.org/#stagingRepositories).
8. Update the `gradle.properties` to the next SNAPSHOT version.
9. `git commit -am "Prepare next development version"`
10. `git push origin`
11. Release on GitHub:
    1. Create a new release [here](https://github.com/ansman/kotshi/releases/new).
    2. Use `git shortlog <previous-release>..HEAD` as a base for the release notes.
    3. Ensure you pick the "Prepare for release X.Y.Z" as the target commit.
12. `git push origin && git fetch origin --tags`