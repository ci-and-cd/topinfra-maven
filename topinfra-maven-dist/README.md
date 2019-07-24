# topinfra-maven-dist

A set of singleton components shared by all topinfra-maven modules.

Maven is designed to share nothing between extensions but I do need to have some components initialized only once so I can 
make modules pluggable and can manage components (there are order restrictions) from topinfra-maven-extension and its modules.

It is hard to run extension by order without sharing anything.

snapshots: https://oss.sonatype.org/content/repositories/snapshots/top/infra/maven/topinfra-maven-dist
