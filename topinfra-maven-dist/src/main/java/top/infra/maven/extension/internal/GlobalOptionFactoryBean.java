package top.infra.maven.extension.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactoryBean;
import top.infra.maven.shared.extension.GlobalOption;
import top.infra.maven.shared.extension.Orders;

@Named
@Singleton
public class GlobalOptionFactoryBean implements CiOptionFactoryBean {

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_FAST;
    }

    @Override
    public Class<?> getType() {
        return GlobalOption.class;
    }

    @Override
    public List<CiOption> getOptions() {
        return Arrays.asList(GlobalOption.values());
    }
}
