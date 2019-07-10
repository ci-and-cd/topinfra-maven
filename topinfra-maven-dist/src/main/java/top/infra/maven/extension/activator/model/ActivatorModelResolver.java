package top.infra.maven.extension.activator.model;

import java.util.Optional;

import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.profile.ProfileActivationContext;

public interface ActivatorModelResolver {

    Optional<Model> resolveModel(Profile profile, ProfileActivationContext context);
}
