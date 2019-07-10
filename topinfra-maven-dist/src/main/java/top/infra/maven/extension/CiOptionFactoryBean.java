package top.infra.maven.extension;

import java.util.List;

import top.infra.maven.Ordered;
import top.infra.maven.CiOption;

public interface CiOptionFactoryBean extends Ordered {

    Class<?> getType();

    List<CiOption> getOptions();
}
