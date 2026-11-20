package group18.bluej;

// Sources:
//  https://www.bluej.org/extensions/writingextensions2.html
//  https://www.geeksforgeeks.org/java/java-nio-file-paths-class-in-java/

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import group18.facade.TelemetryPluginFacade;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * The BlueJ extension: the class named as {@code Main-Class} in the plugin jar's manifest, which BlueJ creates when
 * it starts.
 *
 * <p>It is the composition root: it builds the {@link TelemetryPluginFacade} and registers the adapters that turn
 * BlueJ's callbacks into facade calls. Everything else (sessions, analysis, JSON and PDF export) lives in the
 * BlueJ-independent {@code telemetry-core} module.</p>
 */
public final class BlueJTelemetryExtension extends Extension {

    /** Plugin version shown in BlueJ's Help &gt; Installed Extensions. */
    static final String VERSION = "1.0.0";

    /** Time between source snapshots. */
    static final Duration SAMPLE_PERIOD = Duration.ofSeconds(10);

    /** Longest wait for the final reports when BlueJ closes. */
    static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(20);

    private static final Logger LOG = Logger.getLogger(BlueJTelemetryExtension.class.getName());

    /** The telemetry engine, created in {@link #startup(BlueJ)}. */
    private TelemetryPluginFacade facade;

    /** Takes the periodic source snapshots. */
    private SourceSampler sampler;

    /** Creates the extension. BlueJ requires a public constructor without parameters. */
    public BlueJTelemetryExtension() {
    }

    /** Requires the BlueJ 5 extensions API (version 3). */
    @Override
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    @Override
    public void startup(BlueJ bluej) {
        StudentIdPreferences preferences = new StudentIdPreferences(bluej);
        facade = TelemetryPluginFacade.builder()
                .studentId(preferences::studentId)
                .fallbackDirectory(Path.of(System.getProperty("user.home"), "BlueJ Telemetry"))
                .build();

        bluej.setPreferenceGenerator(preferences);
        bluej.setMenuGenerator(new TelemetryMenu(facade));
        bluej.addPackageListener(new PackageEventAdapter(facade));
        bluej.addCompileListener(new CompileEventAdapter(facade));
        bluej.addInvocationFinishedListener(new InvocationEventAdapter(facade, new BlueJTerminalReader()));

        sampler = new SourceSampler(bluej, facade, facade, new EditorListeners(facade), SAMPLE_PERIOD);
        sampler.start();
    }

    /** Called when BlueJ closes: ends every open session and writes its reports. */
    @Override
    public void terminate() {
        if (sampler != null) {
            sampler.stop();
        }
        if (facade != null && !facade.shutdown(SHUTDOWN_TIMEOUT)) {
            LOG.warning("Some telemetry reports could not be written before BlueJ closed.");
        }
    }

    @Override
    public String getName() {
        return "COMP3607 Group 18 Telemetry";
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "Records project, editor, compilation and execution telemetry and saves a JSON file and a PDF "
                + "report for each session in the project's telemetry folder.";
    }

    @Override
    public URL getURL() {
        try {
            return new URL("https://www.bluej.org/extensions/extensions2.html");
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
