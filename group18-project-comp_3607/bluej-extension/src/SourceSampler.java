package group18.bluej;

// Sources:
//  BClass: https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/BClass.html (getJavaEditorIfOpen needs
//          BlueJ 5.0.2+; getJavaFile saves an open editor first, so it is only used for closed editors)
//  BProject: https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/BProject.html
//  BPackage: https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/BPackage.html
//  JavaEditor: https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/editor/JavaEditor.html

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;
import bluej.extensions2.BlueJ;
import bluej.extensions2.ExtensionException;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.editor.TextLocation;
import group18.facade.EditorTelemetry;
import group18.facade.ProjectTelemetry;
import group18.model.ProjectRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

final class SourceSampler {

    private static final Logger LOG = Logger.getLogger(SourceSampler.class.getName());

    private final BlueJ bluej;
    private final ProjectTelemetry projects;
    private final EditorTelemetry editors;
    private final EditorListeners listeners;
    private final Duration period;

    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "telemetry-sampler");
        thread.setDaemon(true);
        return thread;
    });

    SourceSampler(BlueJ bluej, ProjectTelemetry projects, EditorTelemetry editors, EditorListeners listeners,
                  Duration period) {
        this.bluej = bluej;
        this.projects = projects;
        this.editors = editors;
        this.listeners = listeners;
        this.period = period;
    }

    void start() {
        timer.scheduleAtFixedRate(() -> FxThread.run(this::sampleSafely), 0, period.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    /** Stops sampling. */
    void stop() {
        timer.shutdownNow();
    }

    private void sampleSafely() {
        try {
            sample();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Telemetry sample failed", e);
        }
    }

    private void sample() {
        Set<String> open = new HashSet<>();
        for (BProject project : bluej.getOpenProjects()) {
            Optional<ProjectRef> ref = BlueJProjects.refOf(project);
            if (ref.isEmpty() || !ref.get().directory().toFile().isDirectory()) {
                continue;
            }
            open.add(ref.get().key());
            projects.projectOpened(ref.get());
            editors.sourcesSampled(ref.get().key(), sources(project, ref.get().key()));
        }
        projects.openProjectsChanged(open);
    }

    private Map<String, String> sources(BProject project, String projectKey) {
        Map<String, String> sources = new HashMap<>();
        try {
            for (BPackage pkg : project.getPackages()) {
                for (BClass bClass : pkg.getClasses()) {
                    readSource(bClass, projectKey).ifPresent(text -> sources.put(bClass.getName(), text));
                }
            }
        } catch (ExtensionException e) {
            LOG.log(Level.FINE, "Project closed while sampling", e);
        }
        return sources;
    }
    private Optional<String> readSource(BClass bClass, String projectKey) {
        try {
            JavaEditor editor = bClass.getJavaEditorIfOpen();
            if (editor != null) {
                listeners.attach(projectKey, bClass.getName(), editor);
                TextLocation end = editor.getTextLocationFromOffset(editor.getTextLength());
                return Optional.ofNullable(editor.getText(new TextLocation(0, 0), end));
            }
            File file = bClass.getJavaFile();
            if (file != null && file.isFile()) {
                return Optional.of(Files.readString(file.toPath()));
            }
        } catch (ExtensionException | IOException | RuntimeException e) {
            LOG.log(Level.FINE, "Could not read " + bClass.getName(), e);
        }
        return Optional.empty();
    }
}
