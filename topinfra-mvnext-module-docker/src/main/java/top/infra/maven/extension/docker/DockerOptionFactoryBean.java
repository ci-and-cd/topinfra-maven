package top.infra.maven.extension.docker;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactoryBean;
import top.infra.maven.extension.shared.Orders;

@Named
@Singleton
public class DockerOptionFactoryBean implements CiOptionFactoryBean {

    @Override
    public List<CiOption> getOptions() {
        return Arrays.asList(DockerOption.values());
    }

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_DOCKER;
    }

    @Override
    public Class<?> getType() {
        return DockerOption.class;
    }
}
