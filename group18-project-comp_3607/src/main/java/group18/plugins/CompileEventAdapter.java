package group18.plugins;


import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;


import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class CompileEventAdapter implements CompileListener {

    private final TelemetrySink sink;
    private final Clock clock;

    public CompileEventAdapter(TelemetrySink sink, Clock clock) {
        this.sink = sink;
        this.clock = clock;
    }

    @Override
    public void compileStarted(CompileEvent event) {
        forward(Phase.STARTED, event);
    }

    @Override
    public void compileError(CompileEvent event) {
        forward(Phase.ERROR, event);
    }

    @Override
    public void compileWarning(CompileEvent event) {
        forward(Phase.WARNING, event);
    }

    @Override
    public void compileSucceeded(CompileEvent event) {
        forward(Phase.SUCCEEDED, event);
    }

    @Override
    public void compileFailed(CompileEvent event) {
        forward(Phase.FAILED, event);
    }

    private void forward(Phase phase, CompileEvent event) {
        boolean diagnostic = phase == Phase.ERROR || phase == Phase.WARNING;
        sink.recordCompileSignal(new CompileSignal(
                phase,
                fileNames(event.getFiles()),
                event.isUserGeneratedCompilation(),
                diagnostic ? event.getErrorLineNumber() : -1,
                diagnostic ? event.getErrorMessage() : null,
                clock.instant()));
    }

    private static List<String> fileNames(File[] files) {
        List<String> names = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file != null) {
                    names.add(file.getName());
                }
            }
        }
        return names;
    }
}
