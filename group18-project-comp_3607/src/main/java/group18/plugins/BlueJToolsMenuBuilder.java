package group18.plugins;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;


public class BlueJToolsMenuBuilder extends MenuGenerator {

    private final TelemetrySink sink;

    public BlueJToolsMenuBuilder(TelemetrySink sink) {
        this.sink = sink;
    }

    @Override
    public MenuItem getToolsMenuItem(BPackage bPackage) {
        MenuItem item = new MenuItem("Export Telemetry Report (JSON + PDF)");
        item.setOnAction(event -> sink.requestExport()
                .whenComplete((files, error) -> FxThread.run(() -> showResult(files, error))));
        return item;
    }

    private static void showResult(List<Path> files, Throwable error) {
        Alert alert;
        if (error == null) {
            String list = files.stream().map(Path::toString).collect(Collectors.joining("\n"));
            alert = new Alert(Alert.AlertType.INFORMATION, "Telemetry report saved:\n" + list);
        } else {
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            alert = new Alert(Alert.AlertType.ERROR, "Telemetry export failed: " + cause.getMessage());
        }
        alert.setHeaderText("BlueJ Telemetry");
        alert.show();
    }
}
