package top.infra.maven.extension.infra;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactory;
import top.infra.maven.shared.extension.Orders;

@Named
@Singleton
public class InfraOptionFactory implements CiOptionFactory {

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_INFRA;
    }

    @Override
    public Class<?> getType() {
        return InfraOption.class;
    }

    @Override
    public List<CiOption> getObjects() {
        return Arrays.asList(InfraOption.values());
    }
}
