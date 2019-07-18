package top.infra.maven.extension.main;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactoryBean;
import top.infra.maven.shared.extension.Orders;

@Named
@Singleton
public class MavenBuildExtensionOptionFactoryBean implements CiOptionFactoryBean {

    @Override
    public List<CiOption> getOptions() {
        return Arrays.asList(MavenBuildExtensionOption.values());
    }

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_MAVEN_BUILD_EXTENSION;
    }

    @Override
    public Class<?> getType() {
        return MavenBuildExtensionOption.class;
    }
}
