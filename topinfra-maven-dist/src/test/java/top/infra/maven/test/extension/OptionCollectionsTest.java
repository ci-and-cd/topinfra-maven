package top.infra.maven.test.extension;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.infra.maven.CiOption;

public class OptionCollectionsTest {

    private static final Logger log = LoggerFactory.getLogger(OptionCollectionsTest.class);

    @Test
    public void testOptionCollections() {
        final List<List<CiOption>> optionCollections = OptionCollections.optionCollections();

        optionCollections.forEach(it -> log.info("optionCollections: {}", it));

        assertEquals(2, optionCollections.size());
    }
}
