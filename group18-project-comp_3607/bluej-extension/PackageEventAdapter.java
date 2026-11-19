package group18.bluej;

// Sources:
//  https://www.bluej.org/extensions/writingextensions2.html (PackageListener)
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/PackageEvent.html

import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;
import group18.facade.ProjectTelemetry;

import java.util.Objects;


final class PackageEventAdapter implements PackageListener {


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
