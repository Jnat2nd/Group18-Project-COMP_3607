package group18.bluej;

// Sources:
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/CompileListener.html
//  https://www.bluej.org/doc/extensionsAPI2/bluej/extensions2/event/CompileEvent.html

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;
import group18.facade.CompileTelemetry;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>Adapter</b>: turns BlueJ's compile callbacks into compile calls on the telemetry facade.
 *
 * <p>BlueJ also compiles in the background while the student types, to underline errors. Those checks are not
 * compilation attempts by the student, so only compilations BlueJ marks as user-generated are passed on.
 * Warnings are ignored.</p>
 */
final class CompileEventAdapter implements CompileListener {

    /** Receives the compile calls. */
    private final CompileTelemetry telemetry;

    /**
     * Creates the adapter.
     *
     * @param telemetry receives the compile calls
     */
    CompileEventAdapter(CompileTelemetry telemetry) {
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
    }

    @Override
    public void compileStarted(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            telemetry.compileStarted(files(event));
        }
    }

    @Override
    public void compileError(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            List<Path> files = files(event);
            telemetry.compileError(files.isEmpty() ? null : files.get(0), event.getErrorLineNumber(),
                    event.getErrorMessage());
        }
    }

    @Override
    public void compileWarning(CompileEvent event) {
    }

    @Override
    public void compileSucceeded(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            telemetry.compileFinished(files(event), true);
        }
    }

    @Override
    public void compileFailed(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            telemetry.compileFinished(files(event), false);
        }
    }

    private static List<Path> files(CompileEvent event) {
        List<Path> paths = new ArrayList<>();
        File[] files = event.getFiles();
        if (files != null) {
            for (File file : files) {
                if (file != null) {
                    paths.add(file.toPath());
                }
            }
        }
        return paths;
    }
}
