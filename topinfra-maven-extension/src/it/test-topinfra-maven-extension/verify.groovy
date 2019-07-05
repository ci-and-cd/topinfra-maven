
def assertMagicFileExists(String filename) {
    File magicFile = new File( basedir, filename )
    assert magicFile.exists()
}

def assertMagicFileNotExists(String filename) {
    File magicFile = new File( basedir, filename )
    assert !magicFile.exists()
}

def assertUserProperty(String propertyName, String expectedValue) {
    Properties properties = new Properties()
    File propertiesFile = new File( basedir, '/target/user.properties' )
    propertiesFile.withInputStream { properties.load(it) }

    String actualValue = properties.getProperty(propertyName)
    if (expectedValue != null) {
        assert expectedValue == actualValue
    } else {
        assert actualValue == null
    }
}

assertMagicFileExists('/target/parent-infrastructure_ossrh.md')
assertMagicFileNotExists('/target/parent-infrastructure_ossrh-nexus2_staging.md')
assertMagicFileExists('/target/parent-java8-profile1.md')
assertMagicFileExists('/target/parent-java-8-profile2.md')
assertMagicFileExists('/target/parent-profile-sonar.md')
assertMagicFileExists('/build-docker/target/build-docker-java8-profile1.md')
assertMagicFileExists('/build-docker/target/build-docker-java-8-profile2.md')

assertMagicFileExists('target/run-on-multi_module_root_only.md')
assertMagicFileNotExists('/build-docker/target/run-on-multi_module_root_only.md')
assertMagicFileExists('target/run-on-multi-module-root-and-sub-modules.md')
assertMagicFileExists('/build-docker/target/run-on-multi-module-root-and-sub-modules.md')

assertUserProperty('infrastructure', 'ossrh')
assertUserProperty('git.commit.id.skip', 'false')
