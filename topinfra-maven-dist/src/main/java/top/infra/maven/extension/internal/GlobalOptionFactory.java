package top.infra.maven.extension.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactory;
import top.infra.maven.shared.extension.GlobalOption;
import top.infra.maven.shared.extension.Orders;

@Named
@Singleton
public class GlobalOptionFactory implements CiOptionFactory {

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_FAST;
    }

    @Override
    public Class<?> getType() {
        return GlobalOption.class;
    }

    @Override
    public List<CiOption> getObjects() {
        return Arrays.asList(GlobalOption.values());
    }
}
