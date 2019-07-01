# maven-core-extensions
Maven core extensions (for github.com/ci-and-cd/maven-build)


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
CI_OPT_SONAR="true" CI_OPT_SONAR_ORGANIZATION="home1-oss-github" ./mvnw -ntp -s settings.xml clean install

CI_OPT_SONAR="true" CI_OPT_SONAR_ORGANIZATION="home1-oss-github" ./mvnw -ntp -Dgpg.executable=gpg2 -Dgpg.loopback=true -s settings.xml clean install

#CI_OPT_GITHUB_SITE_PUBLISH="true" CI_OPT_INFRASTRUCTURE=ossrh CI_OPT_OSSRH_GIT_AUTH_TOKEN="${CI_OPT_OSSRH_GIT_AUTH_TOKEN}" CI_OPT_SITE="true" CI_OPT_GITHUB_GLOBAL_REPOSITORYOWNER="ci-and-cd" CI_OPT_SITE_PATH_PREFIX="maven-build-extension" ./mvnw -e -U clean install site-deploy

#CI_OPT_GITHUB_SITE_PUBLISH="false" CI_OPT_INFRASTRUCTURE=ossrh CI_OPT_OSSRH_MVNSITE_PASSWORD="${CI_OPT_OSSRH_MVNSITE_PASSWORD}" CI_OPT_OSSRH_MVNSITE_USERNAME="${CI_OPT_OSSRH_MVNSITE_USERNAME}" CI_OPT_NEXUS3="https://nexus3.infra.top" CI_OPT_SITE="true" CI_OPT_SITE_PATH_PREFIX="ci-and-cd/maven-build-extension" ./mvnw -e -U clean install site site-deploy

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
