package top.infra.maven.extension;

import top.infra.maven.CiOptionContext;

public interface CiOptionContextFactoryBean {

    CiOptionContext getObject();
}
