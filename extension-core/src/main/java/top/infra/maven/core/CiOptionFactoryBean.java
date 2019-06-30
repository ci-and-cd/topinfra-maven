package top.infra.maven.core;

import java.util.List;

import top.infra.maven.extension.Ordered;

public interface CiOptionFactoryBean extends Ordered {

    List<CiOption> getOptions();
}
