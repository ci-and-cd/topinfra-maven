package top.infra.maven.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newLinkedHashSetWithExpectedSize;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

public abstract class ClassPathUtils {

    private ClassPathUtils() {
    }

    @AllArgsConstructor
    public static class InterfaceFilter extends AbstractClassTestingTypeFilter {

        private final Class<?> type;

        @Override
        protected boolean match(final ClassMetadata metadata) {
            return contains(metadata.getInterfaceNames(), this.type.getName());
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private static <T> Class<T> classForName(final String className) {
        return (Class<T>) Class.forName(className);
    }

    public static <T> Set<Class<T>> scan(final String basePackage, final TypeFilter includeFilter) {
        checkArgument(isNotBlank(basePackage));
        //log.info("domainEnums basePackage: {}", basePackage);

        final ClassPathScanningCandidateProvider provider = new ClassPathScanningCandidateProvider();
        provider.addIncludeFilter(includeFilter);
        final Set<BeanDefinition> beanDefinitions = provider.findCandidateComponents(basePackage.replaceAll("\\.", "/"));

        final Set<Class<T>> result = newLinkedHashSetWithExpectedSize(beanDefinitions.size());
        for (final BeanDefinition beanDefinition : beanDefinitions) {
            result.add(classForName(beanDefinition.getBeanClassName()));
        }
        return result;
    }

    public static class ClassPathScanningCandidateProvider extends ClassPathScanningCandidateComponentProvider {

        public ClassPathScanningCandidateProvider() {
            super(false);
        }

        @Override
        protected boolean isCandidateComponent(final AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isIndependent();
        }
    }
}
