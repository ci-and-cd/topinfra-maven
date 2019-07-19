package top.infra.maven.extension.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactory;
import top.infra.maven.shared.extension.MavenOption;
import top.infra.maven.shared.extension.Orders;

@Named
@Singleton
public class MavenOptionFactory implements CiOptionFactory {

    @Override
    public int getOrder() {
        return Orders.CI_OPTION_MAVEN;
    }

    @Override
    public Class<?> getType() {
        return MavenOption.class;
    }

    @Override
    public List<CiOption> getObjects() {
        return Arrays.asList(MavenOption.values());
    }
}
