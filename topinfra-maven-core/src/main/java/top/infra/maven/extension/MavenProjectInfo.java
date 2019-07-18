package top.infra.maven.extension;

import org.apache.maven.model.Model;

public interface MavenProjectInfo {

    String getArtifactId();

    String getId();

    String getGroupId();

    String getJavaVersion();

    String getPackaging();

    String getVersion();

    boolean idEquals(Model model);

    boolean idEqualsExceptInheritedGroupId(Model model);
}
