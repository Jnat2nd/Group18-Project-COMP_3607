package group18.bluej;

// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/BProject.html
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/BPackage.html

import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.ExtensionException;
import group18.model.ProjectRef;

import java.io.File;
import java.util.Optional;


final class BlueJProjects {

    private BlueJProjects() {
    }
    static Optional<ProjectRef> refOf(BProject project) {
        if (project == null) {
            return Optional.empty();
        }
        try {
            File directory = project.getDir();
            return directory == null ? Optional.empty() : Optional.of(ProjectRef.of(project.getName(), directory.toPath()));
        } catch (ExtensionException | RuntimeException e) {
            return Optional.empty();
        }
    }

    static Optional<ProjectRef> refOf(BPackage pkg) {
        if (pkg == null) {
            return Optional.empty();
        }
        try {
            return refOf(pkg.getProject());
        } catch (ExtensionException | RuntimeException e) {
            return Optional.empty();
        }
    }
    static boolean isTopLevel(BPackage pkg) {
        try {
            String name = pkg.getName();
            return name == null || name.isEmpty();
        } catch (ExtensionException | RuntimeException e) {
            return false;
        }
    }
}
