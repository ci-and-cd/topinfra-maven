# topinfra-maven-dist

A lib shared by topinfra-maven-extension and its modules.

Maven is designed to share nothing between extensions but I do need to share at least a lib between topinfra-maven-extension and its modules
 to make modules pluggable.

These extension need to be run before any maven plugins (so they have to be extension not plugin) and there are order restrictions.
It is hard to run extension by order without sharing any lib.

snapshots: https://oss.sonatype.org/content/repositories/snapshots/top/infra/maven/topinfra-maven-dist
