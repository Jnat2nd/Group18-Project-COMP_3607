package group18.bluej;

// Source: https://www.bluej.org/extensions/writingextensions2.html (MenuGenerator)

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import group18.facade.ReportTelemetry;
import group18.model.ProjectRef;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

final class TelemetryMenu extends MenuGenerator {

    static final String LABEL = "Export Telemetry Report (JSON + PDF)";

    /** Runs the export. */
    private final ReportTelemetry reports;

    TelemetryMenu(ReportTelemetry reports) {
        this.reports = Objects.requireNonNull(reports, "reports");
    }

    @Override
    public MenuItem getToolsMenuItem(BPackage bPackage) {
        MenuItem item = new MenuItem(LABEL);
        item.setOnAction(event -> {
            String projectKey = BlueJProjects.refOf(bPackage).map(ProjectRef::key).orElse(null);
            reports.exportNow(projectKey) .whenComplete((files, error) -> FxThread.run(() -> showResult(files, error)));
        });
        return item;
    }

    private static void showResult(List<Path> files, Throwable error) {
        Alert alert;
        if (error == null) {
            String list = files.stream().map(Path::toString).collect(Collectors.joining("\n"));
            alert = new Alert(Alert.AlertType.INFORMATION, "Telemetry reports saved:\n" + list);
        } else {
            Throwable cause = error instanceof CompletionException && error.getCause() != null
                    ? error.getCause() : error;
            alert = new Alert(Alert.AlertType.ERROR, "Telemetry export failed: " + cause.getMessage());
        }
        alert.setHeaderText("BlueJ Telemetry");
        alert.show();
    }
}
