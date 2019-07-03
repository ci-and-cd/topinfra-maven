# maven-core-extensions
Maven core extensions (for github.com/ci-and-cd/maven-build)


snapshots: https://oss.sonatype.org/content/repositories/snapshots/top/infra/maven/

```bash
git clone -b feature/distributionUrl git@github.com:ci-and-cd/takari-maven-plugin.git
mvn -f takari-maven-plugin clean install

mvn -N io.takari:maven:0.7.7-SNAPSHOT:wrapper -DdistributionUrl=https://oss.sonatype.org/content/repositories/snapshots/top/infra/maven/topinfra-maven-dist/0.0.1-SNAPSHOT/topinfra-maven-dist-0.0.1-20190703.071528-2.zip
```

Support overriding maven local repository by user property settings.localRepository
Allow overriding value of localRepository in settings.xml by user property settings.localRepository.
e.g. `./mvnw -Dsettings.localRepository=${HOME}/.m3/repository clean install`

Auto fill empty or blank properties (e.g. CI_OPT_GPG_PASSPHRASE) in maven settings.xml.
Fix 'Failed to decrypt passphrase for server foo: org.sonatype.plexus.components.cipher.PlexusCipherException...'.


### Usage

```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">

    <extension>
        <groupId>top.infra</groupId>
        <artifactId>maven-build-extension</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </extension>
</extensions>
```


### Build this extension

```bash
./mvnw -s settings.xml clean install

CI_OPT_SONAR="true" CI_OPT_SONAR_ORGANIZATION="home1-oss-github" ./mvnw -Dgpg.executable=gpg -Dgpg.loopback=true -s settings.xml clean deploy

./mvnw dependency:tree

#-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
```

### References

[pom-manipulation-ext](https://github.com/release-engineering/pom-manipulation-ext/tree/master/ext/src/main/java/org/commonjava/maven/ext/manip)
[maven-help-plugin](https://github.com/apache/maven-help-plugin/blob/maven-help-plugin-3.2.0)

[faster-maven-builds-with-maven-opts](https://medium.com/@john_freeman/faster-maven-builds-with-maven-opts-822cdc82fa85)
https://docs.oracle.com/javase/8/docs/technotes/guides/vm/class-data-sharing.html#skip2content

[Lifecycles Reference](https://maven.apache.org/ref/3.6.1/maven-core/lifecycles.html)
[introduction-to-the-lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
[standard set of bindings](https://maven.apache.org/ref/3.6.1/maven-core/default-bindings.html)
