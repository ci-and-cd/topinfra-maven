# topinfra-maven

Maven core extensions run before maven plugins help you logging into docker registry, decrypting gpg key 
or customizing settings files etc...


### 1. Install topinfra-maven

See [homebrew-topinfra](https://github.com/ci-and-cd/homebrew-topinfra) to find out 
how to install topinfra-maven as an alternative maven distribution or just install a shaded jar into you existing maven installation

snapshots: https://oss.sonatype.org/content/repositories/snapshots/top/infra/maven/


### 2. Use topinfra-maven-extension

You need to install topinfra-maven before using topinfra-maven-extension, 
see [homebrew-topinfra](https://github.com/ci-and-cd/homebrew-topinfra).

.mvn/extensions.xml
```xml
<extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">

    <!-- @formatter:off -->
    <extension><artifactId>topinfra-maven-extension</artifactId><groupId>top.infra.maven</groupId><version>1.0.1</version></extension>
    <extension><artifactId>topinfra-mvnext-module-docker</artifactId><groupId>top.infra.maven</groupId><version>1.0.1</version></extension>
    <extension><artifactId>topinfra-mvnext-module-gitflow-semver</artifactId><groupId>top.infra.maven</groupId><version>1.0.1</version></extension>
    <extension><artifactId>topinfra-mvnext-module-gpg-key</artifactId><groupId>top.infra.maven</groupId><version>1.0.1</version></extension>
    <extension><artifactId>topinfra-mvnext-module-infrastructure-settings</artifactId><groupId>top.infra.maven</groupId><version>1.0.1</version></extension>
    <extension><artifactId>topinfra-mvnext-module-maven-build-pom</artifactId><groupId>top.infra.maven</groupId><version>1.0.1</version></extension>
    <extension><artifactId>topinfra-mvnext-module-settings-security</artifactId><groupId>top.infra.maven</groupId><version>1.0.1</version></extension>
    <!-- @formatter:on -->
</extensions>
```

(e.g. [ci-and-cd/maven-build/.mvn/extensions.xml](https://github.com/ci-and-cd/maven-build/blob/develop/.mvn/extensions.xml))


### Build topinfra-maven

```bash
./mvnw -s settings.xml clean install

CI_OPT_SONAR="true" CI_OPT_SONAR_ORGANIZATION="home1-oss-github" ./mvnw -Dgpg.executable=gpg -Dgpg.loopback=true -s settings.xml clean deploy

./mvnw dependency:tree
```


### References

[faster-maven-builds-with-maven-opts](https://medium.com/@john_freeman/faster-maven-builds-with-maven-opts-822cdc82fa85)
https://docs.oracle.com/javase/8/docs/technotes/guides/vm/class-data-sharing.html#skip2content

[Lifecycles Reference](https://maven.apache.org/ref/3.6.1/maven-core/lifecycles.html)
[introduction-to-the-lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
[standard set of bindings](https://maven.apache.org/ref/3.6.1/maven-core/default-bindings.html)
