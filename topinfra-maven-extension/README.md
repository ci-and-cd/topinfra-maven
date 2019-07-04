# topinfra-maven-extension

The 'main method' of topinfra-mvnext-module-* extension modules.


Those topinfra-mvnext-module-* extension modules will not run without topinfra-maven-extension. 





Support overriding maven local repository by user property settings.localRepository
Allow overriding value of localRepository in settings.xml by user property settings.localRepository.
e.g. `./mvnw -Dsettings.localRepository=${HOME}/.m3/repository clean install`


### References

[pom-manipulation-ext](https://github.com/release-engineering/pom-manipulation-ext/tree/master/ext/src/main/java/org/commonjava/maven/ext/manip)
[maven-help-plugin](https://github.com/apache/maven-help-plugin/blob/maven-help-plugin-3.2.0)
