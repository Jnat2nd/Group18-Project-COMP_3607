package group18;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import group18.demo.DemoRunner;
import group18.demo.DemoScenario;


public final class Main {

    /** Folder used when no output folder is given. */
    static final String DEFAULT_OUTPUT = "telemetry-demo";

    private Main() {
    }

    /**
     * Runs the demo.
     *
     * @param args optionally, the output folder
     * @throws IOException if a report cannot be written
     */
    public static void main(String[] args) throws IOException {
        run(Path.of(args.length > 0 ? args[0] : DEFAULT_OUTPUT), System.out);
    }

    /**
     * Runs the demo and lists the files written.
     *
     * @param outputRoot the output folder
     * @param out        where the list of files is printed
     * @return the files written
     * @throws IOException if a report cannot be written
     */
    static List<Path> run(Path outputRoot, PrintStream out) throws IOException {
        DemoRunner runner = new DemoRunner();
        List<Path> written = new ArrayList<>();
        for (DemoScenario scenario : DemoScenario.values()) {
            out.println(scenario + " (" + scenario.projectName() + "): " + scenario.description());
            for (Path file : runner.run(scenario, outputRoot)) {
                out.println("  " + file.toAbsolutePath());
                written.add(file);
            }
        }
        return written;
    }
}
