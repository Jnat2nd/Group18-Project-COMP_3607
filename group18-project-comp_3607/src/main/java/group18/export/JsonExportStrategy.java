package group18.export;

import group18.model.SessionReport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import java.util.List;
import java.util.Objects;

public final class JsonExportStrategy implements ExportStrategy {

    private final JsonTelemetrySerialiser serialiser;

    private final ExportFileNames names;

    public JsonExportStrategy(JsonTelemetrySerialiser serialiser, ExportFileNames names) {
        this.serialiser = Objects.requireNonNull(serialiser, "serialiser");
        this.names = Objects.requireNonNull(names, "names");
    }

    @Override
    public List<Path> export(SessionReport report, Path outputDirectory) throws IOException {
        byte[] json = serialiser.serialise(report).getBytes(StandardCharsets.UTF_8);
        Path target = outputDirectory.resolve(names.json(report.metadata()));
        AtomicFiles.write(target, out -> out.write(json));
        return List.of(target);
    }
}