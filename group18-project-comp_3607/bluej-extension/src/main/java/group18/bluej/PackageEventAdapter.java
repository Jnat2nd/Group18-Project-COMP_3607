package group18.bluej;

// Sources:
//  https://www.bluej.org/extensions/writingextensions2.html (PackageListener)
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/PackageEvent.html

import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;
import group18.facade.ProjectTelemetry;

import java.util.Objects;

/**
 * <b>Adapter</b>: turns BlueJ's package-opened and package-closing callbacks into project lifecycle calls on the
 * telemetry facade. Opening any package of a project starts its session; closing the project's top-level package
 * ends it.
 */
final class PackageEventAdapter implements PackageListener {

    /** Receives the project lifecycle calls. */
    private final ProjectTelemetry telemetry;

    /**
     * Creates the adapter.
     *
     * @param telemetry receives the project lifecycle calls
     */
    PackageEventAdapter(ProjectTelemetry telemetry) {
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    @Override
    public void packageOpened(PackageEvent event) {
        BlueJProjects.refOf(event.getPackage()).ifPresent(telemetry::projectOpened);
    }

    @Override
    public void packageClosing(PackageEvent event) {
        if (BlueJProjects.isTopLevel(event.getPackage())) {
            BlueJProjects.refOf(event.getPackage()).ifPresent(project -> telemetry.projectClosed(project.key()));
        }
    }
}
