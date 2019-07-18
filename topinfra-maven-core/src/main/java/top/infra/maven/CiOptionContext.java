package top.infra.maven;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

public interface CiOptionContext {

    Properties getSystemProperties();

    Properties getUserProperties();

    Properties setCiOptPropertiesInto(
        Collection<List<CiOption>> optionGroups,
        Properties... targetProperties
    );
}
