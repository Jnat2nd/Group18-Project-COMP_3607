package group18.facade;

import group18.analytics.TelemetryAggregator;
import group18.command.CloseProjectCommand;
import group18.command.CloseProjectsCommand;
import group18.command.CommandProcessor;
import group18.command.CompileSignalCommand;
import group18.command.ExecutionCommand;
import group18.command.ExportReportCommand;
import group18.command.OpenProjectCommand;
import group18.command.SampleSourcesCommand;
import group18.command.TextEditCommand;
import group18.export.CompositeExportStrategy;
import group18.export.ExportFileNames;
import group18.export.ExportStrategy;
import group18.export.JsonExportStrategy;
import group18.export.JsonTelemetrySerialiser;
import group18.export.PdfExportStrategy;
import group18.listener.CompileSignal;
import group18.listener.InvocationResult;
import group18.model.ExportReason;
import group18.model.ProjectRef;
import group18.model.SessionInfo;
import group18.session.NewProjectDetector;
import group18.session.OutputDirectories;
import group18.session.SessionExporter;
import group18.session.SessionRegistry;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * The single entry point the BlueJ adapters use: the <b>Facade</b> pattern.
 *
 * <p>Behind a handful of simple methods it hides the command queue, the per-project sessions, the trackers, the
 * aggregator and the exporters. Each method stamps the request with the current time, wraps it in a
 * {@link group18.command.TelemetryCommand} and queues it, so the caller (BlueJ's user-interface thread) never
 * waits for telemetry work. The facade implements five small role interfaces, and each BlueJ adapter depends only
 * on the one it needs (Interface Segregation principle).</p>
 *
 * <p>Instances are created with {@link #builder()} (the <b>Builder</b> pattern), which supplies sensible defaults
 * so callers only set what they need.</p>
 */
public final class TelemetryPluginFacade
        implements ProjectTelemetry, EditorTelemetry, CompileTelemetry, ExecutionTelemetry, ReportTelemetry {

    /** Runs the commands on one background thread. */
    private final CommandProcessor processor;

    /** Open sessions; only touched by commands, on the processor's thread. */
    private final SessionRegistry registry = new SessionRegistry();

    /** Writes reports. */
    private final SessionExporter exporter;

    /** Decides whether an opened project was just created. */
    private final BiPredicate<Path, Instant> isNewProject;

    /** Time source for event timestamps. */
    private final Clock clock;

    /** Supplies the student ID for new sessions. */
    private final Supplier<String> studentId;

    /** Supplies identifiers for new sessions. */
    private final Supplier<String> sessionIds;

    private TelemetryPluginFacade(Builder builder) {
        this.processor = builder.processor != null ? builder.processor : CommandProcessor.singleThreaded();
        ExportStrategy formats = builder.exportStrategy != null ? builder.exportStrategy : builder.defaultFormats();
        this.exporter = new SessionExporter(new TelemetryAggregator(), formats,
                new OutputDirectories(builder.fallbackDirectory));
        this.isNewProject = builder.isNewProject;
        this.clock = builder.clock;
        this.studentId = builder.studentId;
        this.sessionIds = builder.sessionIds;
    }

    /**
     * Starts building a facade.
     *
     * @return a builder with default settings
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public CompletableFuture<Void> projectOpened(ProjectRef project) {
        SessionInfo info = new SessionInfo(sessionIds.get(), studentId.get(), project, clock.instant());
        return processor.submit(new OpenProjectCommand(registry, info, isNewProject));
    }

    @Override
    public CompletableFuture<List<Path>> projectClosed(String projectKey) {
        CloseProjectCommand command = new CloseProjectCommand(registry, exporter, projectKey,
                ExportReason.PROJECT_CLOSED, clock.instant());
        return processor.submit(command).thenApply(done -> command.writtenFiles());
    }

    @Override
    public CompletableFuture<List<Path>> openProjectsChanged(Set<String> openProjectKeys) {
        Set<String> open = Set.copyOf(openProjectKeys);
        CloseProjectsCommand command = new CloseProjectsCommand(registry, exporter, key -> !open.contains(key),
                ExportReason.PROJECT_CLOSED, clock.instant());
        return processor.submit(command).thenApply(done -> command.writtenFiles());
    }

    @Override
    public CompletableFuture<Void> sourcesSampled(String projectKey, Map<String, String> classSources) {
        return processor.submit(new SampleSourcesCommand(registry, projectKey, classSources, clock.instant()));
    }

    @Override
    public CompletableFuture<Void> textEdited(String projectKey, String className, int removedChars,
                                              int insertedChars) {
        return processor.submit(new TextEditCommand(registry, projectKey, className, removedChars, insertedChars,
                clock.instant()));
    }

    @Override
    public CompletableFuture<Void> compileStarted(List<Path> files) {
        return processor.submit(new CompileSignalCommand(registry, CompileSignal.started(files, clock.instant())));
    }

    @Override
    public CompletableFuture<Void> compileError(Path file, int lineNumber, String message) {
        return processor.submit(new CompileSignalCommand(registry,
                CompileSignal.error(file, lineNumber, message, clock.instant())));
    }

    @Override
    public CompletableFuture<Void> compileFinished(List<Path> files, boolean successful) {
        return processor.submit(new CompileSignalCommand(registry,
                CompileSignal.finished(files, successful, clock.instant())));
    }

    @Override
    public CompletableFuture<Void> invocationFinished(String projectKey, String className, String methodName,
                                                      String exitType, String result, List<String> consolePanes) {
        InvocationResult invocation = new InvocationResult(className, methodName, exitType, result, consolePanes,
                clock.instant());
        return processor.submit(new ExecutionCommand(registry, projectKey, invocation));
    }

    @Override
    public CompletableFuture<List<Path>> exportNow(String projectKey) {
        ExportReportCommand command = new ExportReportCommand(registry, exporter, projectKey, clock.instant());
        return processor.submit(command).thenApply(done -> command.writtenFiles());
    }

    /**
     * Ends every open session, writes the final reports and stops the background thread. Called when BlueJ exits.
     *
     * @param timeout the longest time to wait for the reports to be written
     * @return {@code true} if every report was written in time
     */
    public boolean shutdown(Duration timeout) {
        CloseProjectsCommand closeAll = new CloseProjectsCommand(registry, exporter, key -> true,
                ExportReason.BLUEJ_EXIT, clock.instant());
        CompletableFuture<Void> done = processor.submit(closeAll);
        boolean finished = processor.shutdown(timeout);
        return finished && done.isDone() && !done.isCompletedExceptionally();
    }

    /**
     * Configures and creates a {@link TelemetryPluginFacade}. Every setting has a default, so
     * {@code TelemetryPluginFacade.builder().build()} gives a working facade that writes JSON and PDF reports.
     */
    public static final class Builder {

        private Clock clock = Clock.systemUTC();
        private ZoneId zone = ZoneId.systemDefault();
        private Supplier<String> studentId = () -> SessionInfo.STUDENT_ID_PLACEHOLDER;
        private Supplier<String> sessionIds = () -> UUID.randomUUID().toString();
        private Path fallbackDirectory = Path.of(System.getProperty("user.home"), "BlueJ Telemetry");
        private BiPredicate<Path, Instant> isNewProject = new NewProjectDetector()::isNewlyCreated;
        private ExportStrategy exportStrategy;
        private CommandProcessor processor;

        private Builder() {
        }

        /**
         * Sets the time source. Default: the system clock.
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Sets the time zone used in report file names and the PDF. Default: the computer's time zone.
         *
         * @param zone the time zone
         * @return this builder
         */
        public Builder zone(ZoneId zone) {
            this.zone = Objects.requireNonNull(zone, "zone");
            return this;
        }

        /**
         * Sets where the student ID comes from; it is read when each session starts. Default: a placeholder.
         *
         * @param studentId supplies the student ID
         * @return this builder
         */
        public Builder studentId(Supplier<String> studentId) {
            this.studentId = Objects.requireNonNull(studentId, "studentId");
            return this;
        }

        /**
         * Sets how session identifiers are made. Default: random UUIDs.
         *
         * @param sessionIds supplies a new identifier for each session
         * @return this builder
         */
        public Builder sessionIds(Supplier<String> sessionIds) {
            this.sessionIds = Objects.requireNonNull(sessionIds, "sessionIds");
            return this;
        }

        /**
         * Sets the folder used when a project folder cannot be written to. Default: {@code ~/BlueJ Telemetry}.
         *
         * @param fallbackDirectory the folder
         * @return this builder
         */
        public Builder fallbackDirectory(Path fallbackDirectory) {
            this.fallbackDirectory = Objects.requireNonNull(fallbackDirectory, "fallbackDirectory");
            return this;
        }

        /**
         * Sets the test for newly created projects. Default: {@link NewProjectDetector}.
         *
         * @param isNewProject decides from the folder and opening time whether a project was just created
         * @return this builder
         */
        public Builder newProjectTest(BiPredicate<Path, Instant> isNewProject) {
            this.isNewProject = Objects.requireNonNull(isNewProject, "isNewProject");
            return this;
        }

        /**
         * Sets how reports are written. Default: JSON and PDF.
         *
         * @param exportStrategy the export strategy
         * @return this builder
         */
        public Builder exportStrategy(ExportStrategy exportStrategy) {
            this.exportStrategy = Objects.requireNonNull(exportStrategy, "exportStrategy");
            return this;
        }

        /**
         * Sets the command processor. Default: a new processor with its own background thread.
         *
         * @param processor the processor
         * @return this builder
         */
        public Builder processor(CommandProcessor processor) {
            this.processor = Objects.requireNonNull(processor, "processor");
            return this;
        }

        /**
         * Creates the facade.
         *
         * @return the facade
         */
        public TelemetryPluginFacade build() {
            return new TelemetryPluginFacade(this);
        }

        private ExportStrategy defaultFormats() {
            ExportFileNames names = new ExportFileNames(zone);
            return new CompositeExportStrategy()
                    .add(new JsonExportStrategy(new JsonTelemetrySerialiser(), names))
                    .add(new PdfExportStrategy(names, zone));
        }
    }
}
