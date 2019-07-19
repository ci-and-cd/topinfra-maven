package top.infra.maven.extension;

import java.util.List;

import top.infra.maven.CiOption;
import top.infra.maven.Ordered;

public interface CiOptionFactory extends Ordered {

    Class<?> getType();

    List<CiOption> getObjects();
}
