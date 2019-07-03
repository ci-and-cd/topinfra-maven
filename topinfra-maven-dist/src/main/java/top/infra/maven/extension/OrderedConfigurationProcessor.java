package top.infra.maven.extension;

import org.apache.maven.cli.CliRequest;

public interface OrderedConfigurationProcessor extends Ordered {

    void process(CliRequest request) throws Exception;
}
