package top.infra.maven.extension;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import top.infra.maven.CiOption;
import top.infra.maven.MavenPhase;
import top.infra.maven.utils.ClassPathUtils;

public abstract class OptionCollections {

    private OptionCollections() {
    }

    public static List<List<CiOption>> optionCollections() {
        final Set<Class<CiOptionFactoryBean>> factories = ClassPathUtils.scan(
            MavenPhase.class.getPackage().getName(),
            new ClassPathUtils.InterfaceFilter(CiOptionFactoryBean.class)
        );

        return factories
            .stream()
            .map(clazz -> {
                try {
                    return clazz.newInstance();
                } catch (final InstantiationException | IllegalAccessException ex) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .sorted()
            .map(CiOptionFactoryBean::getOptions)
            .collect(toList());
    }
}
