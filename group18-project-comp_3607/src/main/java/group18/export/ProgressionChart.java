package group18.export;

import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import group18.model.LifecycleEvent.Action;
import group18.model.SessionMetadata;
import group18.model.TimelineEntry;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

final class ProgressionChart {

    private static final float LEFT = 30;
    private static final float RIGHT = 10;
    private static final float TOP = 16;
    private static final float BOTTOM = 18;
    private static final float FONT_SIZE = 7;

    private record Step(Instant time, int count) {
    }

    private ProgressionChart() {
    }

    static Image create(PdfWriter writer, SessionMetadata metadata, List<TimelineEntry> timeline, ReportFormat format, float width, float height) {
        BaseFont font = helvetica();
        PdfTemplate canvas = writer.getDirectContent().createTemplate(width, height);
        List<Step> classes = steps(timeline, Action.CLASS_CREATED, Action.CLASS_DELETED, metadata);
        List<Step> methods = steps(timeline, Action.METHOD_ADDED, Action.METHOD_REMOVED, metadata);
        int max = Math.max(1, Math.max(highest(classes), highest(methods)));
        int yStep = Math.max(1, (int) Math.ceil(max / 4.0));
        int yMax = yStep * (int) Math.ceil((double) max / yStep);

        float plotWidth = width - LEFT - RIGHT;
        float plotHeight = height - TOP - BOTTOM;
        long start = metadata.startTime().toEpochMilli();
        long span = Math.max(1, metadata.endTime().toEpochMilli() - start);

        canvas.setLineWidth(0.4f);
        canvas.setColorStroke(PdfStyle.GRID);
        for (int value = 0; value <= yMax; value += yStep) {
            float y = BOTTOM + plotHeight * value / yMax;
            canvas.moveTo(LEFT, y);
            canvas.lineTo(LEFT + plotWidth, y);
            canvas.stroke();
            text(canvas, font, String.valueOf(value), LEFT - 4, y - 2.5f, PdfContentByte.ALIGN_RIGHT);
        }
        Instant middle = metadata.startTime().plus(Duration.ofMillis(span / 2));
        text(canvas, font, format.shortTime(metadata.startTime()), LEFT, 4, PdfContentByte.ALIGN_LEFT);
        text(canvas, font, format.shortTime(middle), LEFT + plotWidth / 2, 4, PdfContentByte.ALIGN_CENTER);
        text(canvas, font, format.shortTime(metadata.endTime()), LEFT + plotWidth, 4, PdfContentByte.ALIGN_RIGHT);

        line(canvas, classes, PdfStyle.NAVY, start, span, plotWidth, plotHeight, yMax);
        line(canvas, methods, PdfStyle.ORANGE, start, span, plotWidth, plotHeight, yMax);
        legend(canvas, font, width, height);
        return Image.getInstance(canvas);
    }

    private static List<Step> steps(List<TimelineEntry> timeline, Action up, Action down, SessionMetadata metadata) {
        List<Step> steps = new ArrayList<>();
        int count = 0;
        steps.add(new Step(metadata.startTime(), 0));
        for (TimelineEntry entry : timeline) {
            if (entry.action() == up || entry.action() == down) {
                count += entry.action() == up ? 1 : -1;
                Step last = steps.get(steps.size() - 1);
                if (last.time().equals(entry.timestamp())) {
                    steps.remove(steps.size() - 1);
                }
                steps.add(new Step(entry.timestamp(), Math.max(0, count)));
            }
        }
        steps.add(new Step(metadata.endTime(), Math.max(0, count)));
        return steps;
    }

    private static int highest(List<Step> steps) {
        return steps.stream().mapToInt(Step::count).max().orElse(0);
    }

    private static void line(PdfContentByte canvas, List<Step> steps, Color color, long start, long span,
                             float plotWidth, float plotHeight, int yMax) {
        canvas.saveState();
        canvas.setColorStroke(color);
        canvas.setLineWidth(1.6f);
        float previousY = BOTTOM;
        boolean first = true;
        for (Step step : steps) {
            float x = LEFT + plotWidth * Math.min(1f, Math.max(0f, (step.time().toEpochMilli() - start) / (float) span));
            float y = BOTTOM + plotHeight * step.count() / yMax;
            if (first) {
                canvas.moveTo(x, y);
                first = false;
            } else {
                canvas.lineTo(x, previousY);
                canvas.lineTo(x, y);
            }
            previousY = y;
        }
        canvas.stroke();
        canvas.restoreState();
    }

    private static void legend(PdfContentByte canvas, BaseFont font, float width, float height) {
        float y = height - 7;
        float x = width - RIGHT - 185;
        swatch(canvas, x, y, PdfStyle.NAVY);
        text(canvas, font, "Classes", x + 12, y - 2, PdfContentByte.ALIGN_LEFT);
        swatch(canvas, x + 75, y, PdfStyle.ORANGE);
        text(canvas, font, "Methods and constructors", x + 87, y - 2, PdfContentByte.ALIGN_LEFT);
    }

    private static void swatch(PdfContentByte canvas, float x, float y, Color color) {
        canvas.saveState();
        canvas.setColorFill(color);
        canvas.rectangle(x, y - 2, 9, 4);
        canvas.fill();
        canvas.restoreState();
    }

    private static void text(PdfContentByte canvas, BaseFont font, String text, float x, float y, int alignment) {
        canvas.beginText();
        canvas.setFontAndSize(font, FONT_SIZE);
        canvas.setColorFill(PdfStyle.MUTED);
        canvas.showTextAligned(alignment, text, x, y, 0);
        canvas.endText();
    }

    private static BaseFont helvetica() {
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}