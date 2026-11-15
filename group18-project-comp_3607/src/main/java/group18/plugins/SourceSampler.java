package group18.plugins;
// Sources:
//  - BClass: https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/BClass.html (getJavaEditorIfOpen needs BlueJ 5.0.2+; getJavaFile saves an open file first)
//  - BProject: https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/BProject.html
//  - BPackage: https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/BPackage.html
//  - JavaEditor: https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/editor/JavaEditor.html
//  - Adapted from the BlueJ telemetry plugin zip shared in our group (5 second sampling)

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.BlueJ;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.editor.TextLocation;
import group18.listener.SourceTracker;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


class SourceSampler {

    private final BlueJ bluej;
    private final SourceTracker tracker;
    private final long periodMs;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "telemetry-sampler");
        t.setDaemon(true);
        return t;
    });

    SourceSampler(BlueJ bluej, SourceTracker tracker, long periodMs) {
        this.bluej = bluej;
        this.tracker = tracker;
        this.periodMs = periodMs;
    }

    void start() {
        timer.scheduleAtFixedRate(() -> FxThread.run(this::sample), periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    void stop() {
        timer.shutdownNow();
    }

    private void sample() {
        try {
            Map<String, String> sources = new HashMap<>();
            for (BProject project : bluej.getOpenProjects()) {
                for (BPackage pkg : project.getPackages()) {
                    for (BClass bClass : pkg.getClasses()) {
                        sources.put(project.getName() + "/" + bClass.getName(), read(bClass));
                    }
                }
            }
            tracker.sample(sources, System.currentTimeMillis());
        } catch (Exception e) {
        
        }
    }

   
    private static String read(BClass bClass) throws Exception {
        JavaEditor editor = bClass.getJavaEditorIfOpen();
        if (editor != null) {
            TextLocation end = editor.getTextLocationFromOffset(editor.getTextLength());
            return editor.getText(new TextLocation(0, 0), end);
        }
        return Files.readString(bClass.getJavaFile().toPath()); 
    }
}
