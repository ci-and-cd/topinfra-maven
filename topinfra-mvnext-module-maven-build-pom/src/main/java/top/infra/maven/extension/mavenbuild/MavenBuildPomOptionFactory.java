package top.infra.maven.extension.mavenbuild;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactory;
import top.infra.maven.shared.extension.Orders;

@Named
@Singleton
public class MavenBuildPomOptionFactory implements CiOptionFactory {

    @Override
    public List<CiOption> getObjects() {
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
