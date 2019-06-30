# settings-security-extension


Find settings-security.xml in extra locations:

- userProperties `settings.security` (in `-Dsettings.security=file` following mvn command or ./mvnw maven wrapper)
- systemProperties `settings.security` (in `MAVEN_OPTS="-Dsettings.security=file"` environment variable)
- systemProperties `env.CI_OPT_SETTINGS_SECURITY` (`CI_OPT_SETTINGS_SECURITY` system environment variable)
- Current working directory
- The same directory as `-s` cli option specified settings.xml
