package top.infra.maven.extension.activator;

import org.apache.maven.model.Profile;
import org.apache.maven.model.profile.activation.ProfileActivator;

public interface CustomActivator extends ProfileActivator {

    boolean supported(Profile profile);
}
