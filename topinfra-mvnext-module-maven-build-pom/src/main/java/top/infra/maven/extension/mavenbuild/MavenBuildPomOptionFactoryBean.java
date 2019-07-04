package top.infra.maven.extension.mavenbuild;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionFactoryBean;
import top.infra.maven.extension.Orders;

@Named
@Singleton
public class MavenBuildPomOptionFactoryBean implements CiOptionFactoryBean {

    @Override
    public List<CiOption> getOptions() {
        return Arrays.asList(MavenBuildPomOption.values());
    }

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_MAVEN_BUILD_POM;
    }

    @Override
    public Class<?> getType() {
        return MavenBuildPomOption.class;
    }
}
