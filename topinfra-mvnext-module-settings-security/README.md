# topinfra-mvnext-module-settings-security

A module of topinfra-maven-extension that supports customizing location of settings-security.xml.


You can not customize location of settings-security.xml by user property 
('-Dproperty=value' args following mvn or mvnw command) without this extension.


May be you can customize location of settings-security.xml by setting '-Dsettings.security=path/to/settings-security.xml' in MAVEN_OPTS
environment variable (maven will take properties in MAVEN_OPTS as system property, not user property), I did not test this.
see: [org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher](https://github.com/sonatype/plexus-sec-dispatcher/blob/f4cfab63bf5e1d5eb43a8be3fc1332ec1ae0cd43/src/main/java/org/sonatype/plexus/components/sec/dispatcher/DefaultSecDispatcher.java#L198)



### How this extension works

It will find settings-security.xml in following locations (by order):

- userProperties `settings.security` (in `-Dsettings.security=file` following mvn command or ./mvnw maven wrapper)
- systemProperties `settings.security` (in `MAVEN_OPTS="-Dsettings.security=file"` environment variable)
- systemProperties `env.CI_OPT_SETTINGS_SECURITY` (`CI_OPT_SETTINGS_SECURITY` system environment variable)
- Current working directory
- The same directory as `-s` cli option specified settings.xml
