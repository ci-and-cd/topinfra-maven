package top.infra.maven.extension.main;

import static top.infra.maven.shared.utils.PropertiesUtils.PATTERN_VARS_ENV_DOT_CI;
import static top.infra.maven.shared.utils.PropertiesUtils.logProperties;
import static top.infra.maven.shared.utils.SupportFunction.logEnd;
import static top.infra.maven.shared.utils.SupportFunction.logStart;
import static top.infra.maven.shared.utils.SystemUtils.systemUserHome;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.eventspy.EventSpy.Context;
import org.apache.maven.rtinfo.RuntimeInformation;

import top.infra.logging.Logger;
import top.infra.maven.extension.MavenEventAware;
import top.infra.maven.shared.extension.Orders;
import top.infra.maven.shared.logging.LoggerPlexusImpl;
import top.infra.maven.shared.utils.MavenUtils;

@Named
@Singleton
public class InfoPrinter implements MavenEventAware {

    // @org.codehaus.plexus.component.annotations.Requirement
    private final RuntimeInformation runtime;
    private Logger logger;

    @Inject
    public InfoPrinter(
        final org.codehaus.plexus.logging.Logger logger,
        final RuntimeInformation runtime
    ) {
        this.logger = new LoggerPlexusImpl(logger);

        this.runtime = runtime;
    }

    @Override
    public boolean onInit() {
        return true;
    }

    @Override
    public void onInit(final Context context) {
        this.printInfo(
            context.getClass(),
            MavenUtils.systemProperties(context),
            MavenUtils.userProperties(context)
        );
    }

    @Override
    public int getOrder() {
        return Orders.ORDER_INFO_PRINTER;
    }

    private void printInfo(
        final Class<?> mavenClass,
        final Properties systemProperties,
        final Properties userProperties
    ) {
        logger.info(logStart(this, "printInfo"));

        if (logger.isDebugEnabled()) {
            this.printClassPath(mavenClass);
        }

        if (logger.isInfoEnabled()) {
            logger.info(String.format("    user.language [%s], user.region [%s], user.timezone [%s]",
                System.getProperty("user.language"), System.getProperty("user.region"), System.getProperty("user.timezone")));
            logger.info(String.format("    USER [%s]", System.getProperty("user.name")));
            logger.info(String.format("    HOME [%s]", systemUserHome()));
            logger.info(String.format("    JAVA_HOME [%s]", System.getProperty("java.home")));
            logger.info(String.format("    maven.home [%s]", System.getProperty("maven.home")));

            final String runtimeImplVersion = Runtime.class.getPackage().getImplementationVersion();
            final String javaVersion = runtimeImplVersion != null ? runtimeImplVersion : System.getProperty("java.runtime.version");

            logger.info(String.format("    Java version [%s]", javaVersion));
            logger.info(String.format("    Maven version [%s]", this.runtime.getMavenVersion()));
        }

        if (logger.isDebugEnabled()) {
            logProperties(logger, "    context.data.systemProperties", systemProperties, PATTERN_VARS_ENV_DOT_CI);
            logProperties(logger, "    context.data.userProperties", userProperties, null);
        }

        logger.info(logEnd(this, "printInfo", Void.TYPE));
    }

    private void printClassPath(final Class<?> mavenClass) {
        classPathEntries(logger, ClassLoader.getSystemClassLoader()).forEach(entry ->
            logger.debug(String.format("                    system classpath entry: %s", entry)));
        classPathEntries(logger, Thread.currentThread().getContextClassLoader()).forEach(entry ->
            logger.debug(String.format("    current thread context classpath entry: %s", entry)));
        classPathEntries(logger, mavenClass.getClassLoader()).forEach(entry ->
            logger.debug(String.format("              apache-maven classpath entry: %s", entry)));
        classPathEntries(logger, this.getClass().getClassLoader()).forEach(entry ->
            logger.debug(String.format("     maven-build-extension classpath entry: %s", entry)));
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
                logger.warn(String.format("    Inspecting entries of [%s] is not supported", cl.getClass().getCanonicalName()));
            }
        }
        return result;
    }
}
