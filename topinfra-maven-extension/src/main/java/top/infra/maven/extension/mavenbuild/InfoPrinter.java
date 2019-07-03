package top.infra.maven.extension.mavenbuild;

import static top.infra.maven.utils.SystemUtils.systemUserHome;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.rtinfo.RuntimeInformation;

import top.infra.maven.core.CiOptionNames;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.extension.Orders;
import top.infra.maven.logging.Logger;
import top.infra.maven.logging.LoggerPlexusImpl;
import top.infra.maven.utils.MavenUtils;
import top.infra.maven.utils.PropertiesUtils;

@Named
@Singleton
public class InfoPrinter implements MavenEventAware {

    private Logger logger;

    // @org.codehaus.plexus.component.annotations.Requirement
    private final RuntimeInformation runtime;

    @Inject
    public InfoPrinter(
        final org.codehaus.plexus.logging.Logger logger,
        final RuntimeInformation runtime
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.runtime = runtime;
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_INFO_PRINTER;
    }

    @Override
    public void onInit(final Context context) {
        this.printInfo(
            context.getClass(),
            MavenUtils.systemProperties(context),
            MavenUtils.userProperties(context)
        );
    }

    private void printInfo(
        final Class<?> mavenClass,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        this.printClassPath(mavenClass);

        logger.info(">>>>>>>>>> ---------- init systemProperties ---------- >>>>>>>>>>");
        logger.info(PropertiesUtils.toString(systemProperties, CiOptionNames.PATTERN_VARS_ENV_DOT_CI));
        logger.info("<<<<<<<<<< ---------- init systemProperties ---------- <<<<<<<<<<");
        logger.info(">>>>>>>>>> ---------- init userProperties ---------- >>>>>>>>>>");
        logger.info(PropertiesUtils.toString(userProperties, null));
        logger.info("<<<<<<<<<< ---------- init userProperties ---------- <<<<<<<<<<");

        final Path rootProjectPath = MavenUtils.executionRootPath(systemProperties).toAbsolutePath();
        final String artifactId = new File(rootProjectPath.toString()).getName();
        if (logger.isInfoEnabled()) {
            logger.info(String.format("artifactId: [%s]", artifactId));
        }

        if (logger.isInfoEnabled()) {
            logger.info(">>>>>>>>>> ---------- build context info ---------- >>>>>>>>>>");
            logger.info(String.format("user.language [%s], user.region [%s], user.timezone [%s]",
                System.getProperty("user.language"), System.getProperty("user.region"), System.getProperty("user.timezone")));
            logger.info(String.format("USER [%s]", System.getProperty("user.name")));
            logger.info(String.format("HOME [%s]", systemUserHome()));
            logger.info(String.format("JAVA_HOME [%s]", System.getProperty("java.home")));
            logger.info(String.format("PWD [%s]", rootProjectPath));

            final String runtimeImplVersion = Runtime.class.getPackage().getImplementationVersion();
            final String javaVersion = runtimeImplVersion != null ? runtimeImplVersion : System.getProperty("java.runtime.version");

            logger.info(String.format("Java version [%s]", javaVersion));
            logger.info(String.format("Maven version [%s]", this.runtime.getMavenVersion()));
            logger.info("<<<<<<<<<< ---------- build context info ---------- <<<<<<<<<<");
        }
    }

    private void printClassPath(final Class<?> mavenClass) {
        if (logger.isInfoEnabled()) {
            classPathEntries(logger, ClassLoader.getSystemClassLoader()).forEach(entry ->
                logger.info(String.format("                system classpath entry: %s", entry)));
            classPathEntries(logger, Thread.currentThread().getContextClassLoader()).forEach(entry ->
                logger.info(String.format("current thread context classpath entry: %s", entry)));
            classPathEntries(logger, mavenClass.getClassLoader()).forEach(entry ->
                logger.info(String.format("          apache-maven classpath entry: %s", entry)));
            classPathEntries(logger, this.getClass().getClassLoader()).forEach(entry ->
                logger.info(String.format(" maven-build-extension classpath entry: %s", entry)));
        }
    }

    private static List<String> classPathEntries(
        final Logger logger,
        final ClassLoader cl
    ) {
        final List<String> result = new LinkedList<>();
        if (cl instanceof URLClassLoader) {
            final URL[] urls = ((URLClassLoader) cl).getURLs();
            for (final URL url : urls) {
                result.add(url.toString());
            }
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn(String.format("Inspecting entries of [%s] is not supported", cl.getClass().getCanonicalName()));
            }
        }
        return result;
    }
}
