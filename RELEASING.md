1. `git pull origin`
2. Change the version in `gradle.properties` to a non-snapshot version.
3. Update the `README.md` with the new version.
4. `git commit -am "Prepare for release X.Y.Z"` (where X.Y.Z is the new version)
5. `./publish.sh`.
6. Close and release on [Sonatype](https://oss.sonatype.org/#stagingRepositories).
7. `git push origin`
8. Release on GitHub:
   1. Create a new release [here](https://github.com/ansman/kotshi/releases/new).
   2. Use the automatic release notes as a base.
   3. Ensure you pick the "Prepare for release X.Y.Z" as the target commit.
9. `git pull origin main --tags`
10. Update the `gradle.properties` to the next SNAPSHOT version.
11. `git commit -am "Prepare next development version"`
12. `git push origin`