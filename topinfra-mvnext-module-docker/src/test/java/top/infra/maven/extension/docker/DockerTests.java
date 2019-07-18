package top.infra.maven.extension.docker;

import static org.junit.Assert.assertEquals;
import static top.infra.maven.shared.utils.SupportFunction.lines;

import java.util.List;

import org.junit.Test;
import org.slf4j.LoggerFactory;

public class DockerTests {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(DockerTests.class);

    private final String dockerImagesOutput = "" +
        "REPOSITORY                                 TAG                                                  IMAGE ID            CREATED             SIZE\n" +
        "cloudready/spring-cloud-config-server      2.0.1-SNAPSHOT                                       b5aaf91d5804        5 weeks ago         938MB\n" +
        "<none>                                     <none>                                               242b4e3eb157        5 weeks ago         938MB\n" +
        "docker_cat                                 latest                                               9a41b4bcad23        6 weeks ago         835MB\n" +
        "<none>                                     <none>                                               e1f703331d2f        6 weeks ago         586MB\n" +
        "<none>                                     <none>                                               d5da83562472        6 weeks ago         835MB\n" +
        "<none>                                     <none>                                               b9e98727acaf        6 weeks ago         834MB\n" +
        "<none>                                     <none>                                               1985fd5e89a7        6 weeks ago         834MB\n" +
        "<none>                                     <none>                                               482ee588bb91        6 weeks ago         834MB\n" +
        "cloudready/cat                             3.0.0                                                53b21cc8391d        6 weeks ago         654MB\n" +
        "cloudready/mysql                           5.7.22-SNAPSHOT                                      ddd3305036ef        6 weeks ago         397MB\n" +
        "<none>                                     <none>                                               9d355eb9533e        6 weeks ago         938MB\n" +
        "cloudready/spring-cloud-eureka-server      2.0.1-SNAPSHOT                                       31532010a228        6 weeks ago         932MB\n" +
        "cirepo/service-base-image-java             openjdk-11.0.2-en_US.UTF-8_Etc.UTC-alpine-SNAPSHOT   e349dd275c16        6 weeks ago         884MB\n" +
        "cloudready/spring-cloud-eureka-sidecar     2.0.1-SNAPSHOT                                       fe6dea2b1358        6 weeks ago         931MB\n" +
        "cloudready/spring-boot-admin               2.1.4-SNAPSHOT                                       289dde7300e9        7 weeks ago         976MB\n" +
        "cloudready/spring-boot-admin               2.1.4                                                97798584fa33        7 weeks ago         973MB\n" +
        "cirepo/cibase                              latest-bionic                                        3eee908e1ed5        8 weeks ago         6.79GB\n" +
        "cirepo/service-base-image-java             <none>                                               4b99a52f652a        8 weeks ago         884MB\n" +
        "cirepo/locale                              en_US.UTF-8_Etc.UTC-bionic-archive                   7ec7942eda64        8 weeks ago         18.4MB\n" +
        "cirepo/nix                                 2.2.1-bionic                                         ff9a2d603075        8 weeks ago         690MB\n" +
        "cirepo/jenkins                             lts-alpine                                           06f9b06b77e1        8 weeks ago         341MB\n" +
        "cloudready/mysql                           5.6.40-SNAPSHOT                                      8f149bde230a        8 weeks ago         282MB\n" +
        "jenkins/jenkins                            lts-alpine                                           273ab4b2844b        2 months ago        225MB\n" +
        "alpine                                     3.9                                                  cdf98d1859c1        2 months ago        5.53MB\n" +
        "centos                                     7.6.1810                                             f1cb7c7d58b7        3 months ago        202MB\n" +
        "centos                                     centos6                                              d0957ffdf8a2        3 months ago        194MB\n" +
        "centos                                     7                                                    9f38484d220f        3 months ago        202MB\n" +
        "fedora                                     29                                                   d09302f77cfc        3 months ago        275MB\n" +
        "ubuntu                                     18.04                                                94e814e2efa8        3 months ago        88.9MB\n" +
        "fedora                                     27                                                   f89698585456        3 months ago        236MB\n" +
        "phpmyadmin/phpmyadmin                      4.8.5                                                12ade2c2316a        4 months ago        166MB\n" +
        "k8s.gcr.io/kube-proxy-amd64                v1.10.11                                             7387003276ac        6 months ago        98.3MB\n" +
        "k8s.gcr.io/kube-apiserver-amd64            v1.10.11                                             e851a7aeb6e8        6 months ago        228MB\n" +
        "k8s.gcr.io/kube-controller-manager-amd64   v1.10.11                                             978cfa2028bf        6 months ago        151MB\n" +
        "k8s.gcr.io/kube-scheduler-amd64            v1.10.11                                             d2c751d562c6        6 months ago        51.2MB\n" +
        "cloudready/redis                           3.0.6                                                11486cf6be79        7 months ago        150MB\n" +
        "docker/kube-compose-controller             v0.4.12                                              02a45592fbea        8 months ago        27.8MB\n" +
        "docker/kube-compose-api-server             v0.4.12                                              0f92c77fa676        8 months ago        41.2MB\n" +
        "mysql                                      5.6.40                                               50328380b2b4        10 months ago       256MB\n" +
        "mysql                                      5.7.22                                               6bb891430fb6        10 months ago       372MB\n" +
        "k8s.gcr.io/etcd-amd64                      3.1.12                                               52920ad46f5b        15 months ago       193MB\n" +
        "quay.io/testcontainers/ryuk                0.2.2                                                527073eb32d1        16 months ago       9.31MB\n" +
        "k8s.gcr.io/k8s-dns-dnsmasq-nanny-amd64     1.14.8                                               c2ce1ffb51ed        17 months ago       41MB\n" +
        "k8s.gcr.io/k8s-dns-sidecar-amd64           1.14.8                                               6f7f2dc7fab5        17 months ago       42.2MB\n" +
        "k8s.gcr.io/k8s-dns-kube-dns-amd64          1.14.8                                               80cc5ea4b547        17 months ago       50.5MB\n" +
        "k8s.gcr.io/pause-amd64                     3.1                                                  da86e6ba6ca1        17 months ago       742kB\n" +
        "redis                                      3.0.2                                                5191b3931369        3 years ago         110MB";

    @Test
    public void testBaseImages() {
        final List<String> dockerfiles = Docker.dockerfiles();
        final List<String> baseImages = Docker.baseImages(dockerfiles);
        slf4jLogger.info("baseImages: {}", baseImages);
        assertEquals(1, baseImages.size());
    }

    @Test
    public void testDockerfiles() {
        final List<String> dockerfiles = Docker.dockerfiles();
        slf4jLogger.info("dockerfiles: {}", dockerfiles);
        assertEquals(1, dockerfiles.size());
    }

    @Test
    public void testImagesToClean() {
        final List<String> lines = lines(this.dockerImagesOutput);
        final List<String> imageIds = Docker.imagesToClean(lines);

        slf4jLogger.info("lines: {}", lines);
        slf4jLogger.info("imageIdsToClean: {}", imageIds);
        assertEquals(8, imageIds.size());
    }
}
