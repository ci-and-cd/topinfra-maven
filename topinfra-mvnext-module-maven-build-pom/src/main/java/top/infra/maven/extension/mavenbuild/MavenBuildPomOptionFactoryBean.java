package top.infra.maven.extension.mavenbuild;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactoryBean;
import top.infra.maven.extension.shared.Orders;

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
