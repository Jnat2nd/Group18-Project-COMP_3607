package group18;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import group18.demo.DemoRunner;
import group18.demo.DemoScenario;


public final class Main {
    static final String DEFAULT_OUTPUT = "telemetry-demo";

    private Main() {
    }


    public static void main(String[] args) throws IOException {
        run(Path.of(args.length > 0 ? args[0] : DEFAULT_OUTPUT), System.out);
    }

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
