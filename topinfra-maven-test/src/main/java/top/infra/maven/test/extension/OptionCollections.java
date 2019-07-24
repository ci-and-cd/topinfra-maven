package top.infra.maven.test.extension;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import top.infra.maven.CiOption;
import top.infra.maven.extension.CiOptionFactory;
import top.infra.maven.test.utils.ClassPathUtils;

public abstract class OptionCollections {

    private OptionCollections() {
    }

    public static List<List<CiOption>> optionCollections() {
        final Set<Class<CiOptionFactory>> factories = ClassPathUtils.scan(
            "top.infra.maven",
            new ClassPathUtils.InterfaceFilter(CiOptionFactory.class)
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
            .map(CiOptionFactory::getObjects)
            .collect(toList());
    }
}
