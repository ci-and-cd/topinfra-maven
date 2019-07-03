package top.infra.maven.extension;

import org.apache.maven.cli.CliRequest;
import org.apache.maven.settings.building.SettingsBuildingRequest;

/**
 * Run before {@link SettingsBuildingRequest}.
 */
public interface OrderedConfigurationProcessor extends Ordered {

    void process(CliRequest request) throws Exception;
}
