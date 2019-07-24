package top.infra.maven.extension;

import top.infra.maven.CiOptionContext;

public interface CiOptionContextFactory {

    CiOptionContext getObject();
}
