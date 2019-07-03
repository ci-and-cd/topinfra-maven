
def asssertMagicFileExists(filename) {
    File magicFile = new File( basedir, filename )
    assert magicFile.exists()
}

def asssertMagicFileNotExists(filename) {
    File magicFile = new File( basedir, filename )
    assert !magicFile.exists()
}

asssertMagicFileExists('/target/parent-infrastructure_ossrh.md')
asssertMagicFileNotExists('/target/parent-infrastructure_ossrh-nexus2_staging.md')
asssertMagicFileExists('/target/parent-java8-profile1.md')
asssertMagicFileExists('/target/parent-java-8-profile2.md')
asssertMagicFileExists('/target/parent-profile-sonar.md')
asssertMagicFileExists('/build-docker/target/build-docker-java8-profile1.md')
asssertMagicFileExists('/build-docker/target/build-docker-java-8-profile2.md')

asssertMagicFileExists('target/run-on-multi_module_root_only.md')
asssertMagicFileNotExists('/build-docker/target/run-on-multi_module_root_only.md')
asssertMagicFileExists('target/run-on-multi-module-root-and-sub-modules.md')
asssertMagicFileExists('/build-docker/target/run-on-multi-module-root-and-sub-modules.md')
