package top.infra.maven.extension;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionFactoryBean;

@Named
@Singleton
public class MavenOptionFactoryBean implements CiOptionFactoryBean {

    @Override
    public List<CiOption> getOptions() {
        return Arrays.asList(MavenOption.values());
    }

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_MAVEN;
    }

    @Override
    public Class<?> getType() {
        return MavenOption.class;
    }
}
