package top.infra.maven.extension.gpg;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.core.CiOption;
import top.infra.maven.core.CiOptionFactoryBean;
import top.infra.maven.extension.Orders;

@Named
@Singleton
public class GpgOptionFactoryBean implements CiOptionFactoryBean {

    @Override
    public List<CiOption> getOptions() {
        return Arrays.asList(GpgOption.values());
    }

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_GPG;
    }
}
