package top.infra.maven.extension;

import static org.junit.Assert.assertEquals;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import top.infra.maven.core.CiOption;

@Slf4j
public class OptionCollectionsTest {

    @Test
    public void testOptionCollections() {
        final List<List<CiOption>> optionCollections = OptionCollections.optionCollections();

        optionCollections.forEach(it -> log.info("optionCollections: {}", it));

        assertEquals(4, optionCollections.size());
    }
}
