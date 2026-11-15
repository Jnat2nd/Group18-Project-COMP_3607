package group18.plugins;
// Sources:
//  https://www.bluej.org/extensions/writingextensions2.html (PackageListener)
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/PackageEvent.html

import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;
import group18.listener.SourceTracker;

// Opening or closing a project changes which classes exist; that must not look like classes created or deleted. //
class PackageEventAdapter implements PackageListener {

    private final SourceTracker tracker;

    PackageEventAdapter(SourceTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void packageOpened(PackageEvent event) {
        tracker.rebaseline();
    }

    @Override
    public void packageClosing(PackageEvent event) {
        tracker.rebaseline();
    }
}
