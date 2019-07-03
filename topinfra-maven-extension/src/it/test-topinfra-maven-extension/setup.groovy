
def deleteMagicFile(filename) {
    File magicFile = new File(basedir, filename)
    magicFile.delete()
    assert !magicFile.exists()
}

deleteMagicFile('/target/parent-java8-profile1.md')
deleteMagicFile('/target/parent-java-8-profile2.md')
deleteMagicFile('/build-docker/target/build-docker-java8-profile1.md')
deleteMagicFile('/build-docker/target/build-docker-java-8-profile2.md')
