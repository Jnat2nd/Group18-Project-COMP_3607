package group18.export;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import model.SessionReport;

public class JsonExportStrategy implements ExportStrategy {

    private final JsonTelemetrySerialiser serialiser;

    public JsonExportStrategy(JsonTelemetrySerialiser serialiser) {
        this.serialiser = serialiser;
    }

    @Override
    public List<Path> export(SessionReport report, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        Path jsonFile = outputDirectory.resolve(ExportFileNames.json(report));
        Files.writeString(jsonFile, serialiser.serialise(report), StandardCharsets.UTF_8);
        return List.of(jsonFile);
    }
}
