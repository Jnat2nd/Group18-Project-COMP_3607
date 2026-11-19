package group18.export;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import group18.export.SegmentedBarEvent.Segment;

import group18.listener.EditorActivityTracker;

import group18.model.CompilationError;
import group18.model.CompilationEvent;
import group18.model.EditorEvent;
import group18.model.ExecutionEvent;
import group18.model.LifecycleEvent.Action;
import group18.model.ReportSummary;
import group18.model.ReportSummary.CompilationSummary;
import group18.model.ReportSummary.EditingSummary;
import group18.model.ReportSummary.ErrorFrequency;
import group18.model.ReportSummary.ExceptionFrequency;
import group18.model.ReportSummary.ExecutionSummary;
import group18.model.ReportSummary.TimeSummary;
import group18.model.SessionInfo;
import group18.model.SessionMetadata;
import group18.model.SessionReport;
import group18.model.TimelineEntry;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class PdfReportBuilder {

    static final int MAX_LIST_ROWS = 40;

    static final int MAX_TIMELINE_ROWS = 80;

    private static final float CHART_HEIGHT = 140;

    private final SessionReport report;
    private final ReportFormat format;
    private final Document document;
    private final PdfWriter writer;

    PdfReportBuilder(SessionReport report, ReportFormat format, Document document, PdfWriter writer) {
        this.report = report;
        this.format = format;
        this.document = document;
        this.writer = writer;
    }

    void build() {
        header();
        overview();
        timeSection();
        editingSection();
        compilationSection();
        executionSection();
        progressionSection();
    }

    private void header() {
        SessionMetadata m = report.metadata();
        document.add(new Paragraph("BlueJ Telemetry Session Report", PdfStyle.TITLE));
        Paragraph who = new Paragraph("Project: " + m.projectName() + "        Student ID: " + m.studentId(),PdfStyle.SUBTITLE);
        who.setSpacingBefore(4);
        document.add(who);
        document.add(new Paragraph("Session " + m.sessionId(), PdfStyle.META));
        document.add(new Paragraph("From " + format.dateTime(m.startTime()) + " to " + format.dateTime(m.endTime())+ " (" + format.zoneName() + ")  ·  Duration "+ ReportFormat.duration(m.sessionDurationSeconds()), PdfStyle.META));
        document.add(new Paragraph(m.exportReason().description() + "  ·  " + m.totalEvents()+ " events recorded  ·  " + m.generator(), PdfStyle.META));
        if (SessionInfo.STUDENT_ID_PLACEHOLDER.equals(m.studentId())) {
            document.add(new Paragraph("Student ID not set: enter it in BlueJ under Tools > Preferences > "+ "Extensions.", PdfStyle.WARNING));
        }
    }

    private void overview() {
        ReportSummary s = report.summary();
        section("Session overview");
        PdfPTable tiles = PdfStyle.table(1, 1, 1, 1);
        tiles.addCell(PdfStyle.tile("Active coding time", ReportFormat.duration(s.time().activeSeconds()),ReportFormat.percent(s.time().activePercent()) + " of measured time"));
        tiles.addCell(PdfStyle.tile("Idle time", ReportFormat.duration(s.time().idleSeconds()), s.time().idlePeriods() + " idle period(s)"));
        tiles.addCell(PdfStyle.tile("Compilations", String.valueOf(s.compilation().attempts()), ReportFormat.percent(s.compilation().successRatePercent()) + " successful"));
        tiles.addCell(PdfStyle.tile("Compiler errors", String.valueOf(s.compilation().totalErrors()), s.compilation().errorFrequency().size() + " different kind(s)"));
        tiles.addCell(PdfStyle.tile("Program runs", String.valueOf(s.execution().runs()),
        s.execution().runtimeExceptions() + " runtime exception(s)"));
        tiles.addCell(PdfStyle.tile("Typing cadence",ReportFormat.decimal(s.editing().typingCadenceCharsPerMinute()) + " chars/min",s.editing().charactersTyped() + " characters typed"));
        tiles.addCell(PdfStyle.tile("Edit frequency",ReportFormat.decimal(s.editing().editsPerActiveMinute()) + " edits/min",s.editing().editCount() + " edits in total"));
        tiles.addCell(PdfStyle.tile("Classes / methods",s.lifecycle().classesCreated() + " / " + s.lifecycle().methodsAdded(),"created / added this session"));
        document.add(tiles);
    }

    private void timeSection() {
        TimeSummary t = report.summary().time();
        section("Active coding time vs idle time");
        PdfPTable bar = PdfStyle.table(1);
        bar.setSpacingAfter(2);
        bar.addCell(SegmentedBarEvent.cell(List.of(new Segment(t.activeSeconds(), PdfStyle.GREEN), new Segment(t.idleSeconds(), PdfStyle.GREY)), 20));
        document.add(bar);

        Paragraph legend = new Paragraph();
        legend.add(swatch(PdfStyle.GREEN));
        legend.add(new Phrase(" Active " + ReportFormat.percent(t.activePercent()) + " ("+ ReportFormat.duration(t.activeSeconds()) + ")        ", PdfStyle.BOLD));
        legend.add(swatch(PdfStyle.GREY));
        legend.add(new Phrase(" Idle " + ReportFormat.percent(t.idlePercent()) + " (" + ReportFormat.duration(t.idleSeconds()) + ")", PdfStyle.BOLD));
        document.add(legend);
        document.add(new Paragraph("Idle periods: " + t.idlePeriods() + "  ·  Longest idle stretch: "+ ReportFormat.duration(t.longestIdleSeconds()) + "  ·  Time counts as idle after " + EditorActivityTracker.DEFAULT_IDLE_THRESHOLD.toSeconds() + " seconds without editing, compiling or running code.", PdfStyle.META));

        List<EditorEvent> intervals = report.events().stream().filter(EditorEvent.class::isInstance).map(EditorEvent.class::cast).toList();
        if (intervals.isEmpty()) {
            return;
        }
        subsection("Activity across the session (green = active, grey = idle)");
        List<Segment> segments = new ArrayList<>();
        for (EditorEvent interval : intervals) {
            segments.add(new Segment(interval.activeMs(), PdfStyle.GREEN));
            segments.add(new Segment(interval.idleMs(), PdfStyle.GREY));
        }
        PdfPTable strip = PdfStyle.table(1);
        strip.setSpacingAfter(0);
        strip.addCell(SegmentedBarEvent.cell(segments, 16));
        document.add(strip);
        EditorEvent first = intervals.get(0);
        EditorEvent last = intervals.get(intervals.size() - 1);
        PdfPTable axis = PdfStyle.table(1, 1);
        axis.setSpacingBefore(0);
        axis.addCell(PdfStyle.plain(new Phrase(format.time(first.timestamp().minusMillis(first.intervalMs())),PdfStyle.SMALL), Element.ALIGN_LEFT));
        axis.addCell(PdfStyle.plain(new Phrase(format.time(last.timestamp()), PdfStyle.SMALL), Element.ALIGN_RIGHT));
        document.add(axis);
    }

    private void editingSection() {
        EditingSummary e = report.summary().editing();
        section("Editor interactions");
        PdfPTable table = PdfStyle.table(1, 1, 1, 1, 1, 1);
        for (String heading : List.of("Edits", "Characters typed", "Characters pasted", "Characters deleted","Typing cadence", "Edit frequency")) {table.addCell(PdfStyle.header(heading));
        }
        table.addCell(number(e.editCount()));
        table.addCell(number(e.charactersTyped()));
        table.addCell(number(e.charactersPasted()));
        table.addCell(number(e.charactersDeleted()));
        table.addCell(PdfStyle.cell(ReportFormat.decimal(e.typingCadenceCharsPerMinute()) + " chars/min",PdfStyle.BODY, Element.ALIGN_RIGHT));
        table.addCell(PdfStyle.cell(ReportFormat.decimal(e.editsPerActiveMinute()) + " edits/min",PdfStyle.BODY, Element.ALIGN_RIGHT));
        document.add(table);
        document.add(new Paragraph("Cadence and frequency are per minute of active time. A single edit that inserts "+ "more than " + EditorActivityTracker.PASTE_THRESHOLD + " characters counts as pasted.",PdfStyle.META));
    }

    private void compilationSection() {
        CompilationSummary c = report.summary().compilation();
        section("Compilation");
        if (c.attempts() == 0) {
            document.add(new Paragraph("No compilations were requested in this session.", PdfStyle.BODY));
            return;
        }
        Paragraph totals = new Paragraph();
        totals.add(new Phrase(c.attempts() + " compilation attempt(s): ", PdfStyle.BOLD));
        totals.add(new Phrase(c.successful() + " successful (" + ReportFormat.percent(c.successRatePercent())+ "), ", PdfStyle.GOOD));
        totals.add(new Phrase(c.failed() + " failed (" + ReportFormat.percent(c.errorRatePercent()) + "). ", PdfStyle.BAD));
        totals.add(new Phrase(c.totalErrors() + " compiler error(s), " + ReportFormat.decimal(c.averageErrorsPerFailedAttempt()) + " per failed attempt. Average time "+ "between attempts: " + ReportFormat.duration(Math.round(c.averageSecondsBetweenAttempts())) + ".",PdfStyle.BODY));
        document.add(totals);

        subsection("Frequency of compilation errors");
        if (c.errorFrequency().isEmpty()) {
            document.add(new Paragraph("No compilation errors were recorded.", PdfStyle.BODY));
        } else {
            PdfPTable table = PdfStyle.table(4.4f, 3f, 0.8f, 2.2f);
            table.setHeaderRows(1);
            for (String heading : List.of("Error", "javac code", "Count", "Share of errors")) {
                table.addCell(PdfStyle.header(heading));
            }
            for (ErrorFrequency frequency : c.errorFrequency()) {
                table.addCell(PdfStyle.twoLineCell(frequency.label(), PdfStyle.BOLD, "e.g. " + ReportFormat.clean(frequency.exampleMessage(), 90)));
                table.addCell(PdfStyle.cell(frequency.errorCode(), PdfStyle.SMALL));
                table.addCell(number(frequency.count()));
                table.addCell(shareCell(frequency.count(), c.totalErrors(), frequency.percentOfTotal()));
            }
            document.add(table);
        }

        subsection("Compilation attempts");
        List<CompilationEvent> attempts = report.events().stream().filter(CompilationEvent.class::isInstance).map(CompilationEvent.class::cast).toList();
        PdfPTable table = PdfStyle.table(1.2f, 1.1f, 0.9f, 6.8f);
        table.setHeaderRows(1);
        for (String heading : List.of("Time", "Result", "Errors", "Files and errors (line: message)")) {
            table.addCell(PdfStyle.header(heading));
        }
        for (CompilationEvent attempt : attempts.subList(0, Math.min(MAX_LIST_ROWS, attempts.size()))) {
            table.addCell(PdfStyle.cell(format.time(attempt.timestamp()), PdfStyle.BODY));
            table.addCell(PdfStyle.cell(attempt.successful() ? "OK" : "Failed",attempt.successful() ? PdfStyle.GOOD : PdfStyle.BAD));
            table.addCell(number(attempt.errorCount()));
            table.addCell(PdfStyle.twoLineCell(String.join(", ", attempt.files()), PdfStyle.BODY,errorLines(attempt.errors())));
        }
        document.add(table);
        moreRowsNote(attempts.size(), MAX_LIST_ROWS, "compilation attempts");
    }

    private void executionSection() {
        ExecutionSummary x = report.summary().execution();
        section("Program runs");
        if (x.runs() == 0) {
            document.add(new Paragraph("No methods or constructors were run from BlueJ in this session.",PdfStyle.BODY));
            return;
        }
        Paragraph totals = new Paragraph();
        totals.add(new Phrase(x.runs() + " call(s) run from BlueJ: ", PdfStyle.BOLD));
        totals.add(new Phrase(x.successfulRuns() + " returned normally (" + ReportFormat.percent(x.runSuccessPercent())
                + "), ", PdfStyle.GOOD));
        totals.add(new Phrase(x.runtimeExceptions() + " ended with a runtime exception.", PdfStyle.BAD));
        document.add(totals);

        if (!x.exceptionFrequency().isEmpty()) {
            subsection("Runtime exceptions");
            PdfPTable table = PdfStyle.table(8, 1);
            table.addCell(PdfStyle.header("Exception type"));
            table.addCell(PdfStyle.header("Count"));
            for (ExceptionFrequency frequency : x.exceptionFrequency()) {
                table.addCell(PdfStyle.cell(frequency.exceptionType(), PdfStyle.BODY));
                table.addCell(number(frequency.count()));
            }
            document.add(table);
        }

        subsection("Runs");
        List<ExecutionEvent> runs = report.events().stream().filter(ExecutionEvent.class::isInstance).map(ExecutionEvent.class::cast).toList();
        PdfPTable table = PdfStyle.table(1.2f, 2.6f, 1.3f, 5.9f);
        table.setHeaderRows(1);
        for (String heading : List.of("Time", "Call", "Result", "Exception or console output")) {
            table.addCell(PdfStyle.header(heading));
        }
        for (ExecutionEvent run : runs.subList(0, Math.min(MAX_LIST_ROWS, runs.size()))) {
            table.addCell(PdfStyle.cell(format.time(run.timestamp()), PdfStyle.BODY));
            table.addCell(PdfStyle.cell(run.invocation(), PdfStyle.BODY));
            table.addCell(PdfStyle.cell(resultLabel(run.exitType()), run.successful() ? PdfStyle.GOOD : PdfStyle.BAD));
            table.addCell(runDetailCell(run));
        }
        document.add(table);
        moreRowsNote(runs.size(), MAX_LIST_ROWS, "runs");
    }

    private void progressionSection() {
        section("Progression timeline");
        List<TimelineEntry> timeline = report.timeline();
        if (timeline.isEmpty()) {
            document.add(new Paragraph("No project, class or method milestones were recorded.", PdfStyle.BODY));
            return;
        }
        boolean hasClassOrMethod = timeline.stream().anyMatch(entry -> !entry.action().name().startsWith("PROJECT"));
        if (hasClassOrMethod) {
            document.add(new Paragraph("Classes and methods added since the session started, less those removed",PdfStyle.META));
            Image chart = ProgressionChart.create(writer, report.metadata(), timeline, format,document.right() - document.left(), CHART_HEIGHT);
            document.add(chart);
        }
        PdfPTable table = PdfStyle.table(1.2f, 1.8f, 3f, 4f);
        table.setHeaderRows(1);
        for (String heading : List.of("Time", "Event", "Project or class", "Method or constructor")) {
            table.addCell(PdfStyle.header(heading));
        }
        for (TimelineEntry entry : timeline.subList(0, Math.min(MAX_TIMELINE_ROWS, timeline.size()))) {
            table.addCell(PdfStyle.cell(format.time(entry.timestamp()), PdfStyle.BODY));
            table.addCell(PdfStyle.cell(actionLabel(entry.action()), PdfStyle.BOLD));
            table.addCell(PdfStyle.cell(entry.target(), PdfStyle.BODY));
            table.addCell(PdfStyle.cell(entry.detail() == null ? "" : entry.detail(), PdfStyle.BODY));
        }
        document.add(table);
        moreRowsNote(timeline.size(), MAX_TIMELINE_ROWS, "timeline entries");
    }

    static String actionLabel(Action action) {
        return switch (action) {
            case PROJECT_CREATED -> "Project created";
            case PROJECT_OPENED -> "Project opened";
            case PROJECT_CLOSED -> "Project closed";
            case PROJECT_DELETED -> "Project deleted";
            case CLASS_CREATED -> "Class created";
            case CLASS_MODIFIED -> "Class modified";
            case CLASS_DELETED -> "Class deleted";
            case METHOD_ADDED -> "Method added";
            case METHOD_REMOVED -> "Method removed";
        };
    }

    private static String resultLabel(ExecutionEvent.ExitType exitType) {
        return switch (exitType) {
            case NORMAL_EXIT -> "Returned";
            case EXCEPTION_EXIT -> "Exception";
            case TERMINATED_EXIT -> "Terminated";
            case UNKNOWN_EXIT -> "Unknown";
        };
    }

    private static PdfPCell runDetailCell(ExecutionEvent run) {
        ExecutionEvent.ExceptionDetails exception = run.exception();
        String output = run.console().lineCount() == 0 ? "No console output" : "Output (" + run.console().lineCount() + " line(s)): "+ ReportFormat.clean(run.console().summary(), 160);
        if (exception == null) {
            return PdfStyle.cell(output, PdfStyle.BODY);
        }
        String main = exception.type() + (exception.message() == null ? "" : ": " + exception.message())+ (exception.location() == null ? "" : " (at " + exception.location() + ")");
        boolean outputIsJustTheStackTrace = run.console().summary().startsWith(exception.type());
        return PdfStyle.twoLineCell(ReportFormat.clean(main, 160), PdfStyle.BAD,outputIsJustTheStackTrace ? "" : output);
    }

    private static String errorLines(List<CompilationError> errors) {
        StringBuilder text = new StringBuilder();
        int shown = 0;
        for (CompilationError error : errors) {
            if (shown == 3) {
                text.append("\n... and ").append(errors.size() - shown).append(" more");
                break;
            }
            if (shown > 0) {
                text.append('\n');
            }
            text.append(error.fileName()).append(" line ").append(error.lineNumber()).append(": ").append(ReportFormat.clean(error.message(), 80));
            shown++;
        }
        return text.toString();
    }

    private PdfPCell shareCell(int count, int total, double percent) {
        PdfPTable inner = new PdfPTable(new float[]{3f, 1.5f});
        inner.setWidthPercentage(100);
        inner.addCell(SegmentedBarEvent.cell(List.of(new Segment(count, PdfStyle.RED), new Segment(Math.max(0, total - count), null)), 11));
        inner.addCell(PdfStyle.plain(new Phrase(ReportFormat.percent(percent), PdfStyle.BODY), Element.ALIGN_RIGHT));
        PdfPCell cell = new PdfPCell(inner);
        cell.setBorderColor(PdfStyle.GRID);
        cell.setPadding(3);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private static PdfPCell number(long value) {
        return PdfStyle.cell(String.valueOf(value), PdfStyle.BODY, Element.ALIGN_RIGHT);
    }

    private static Chunk swatch(Color color) {
        Chunk chunk = new Chunk("    ", PdfStyle.BODY);
        chunk.setBackground(color);
        return chunk;
    }

    private void section(String title) {
        Paragraph heading = new Paragraph(title, PdfStyle.SECTION);
        heading.setSpacingBefore(12);
        heading.setSpacingAfter(2);
        document.add(heading);
    }

    private void subsection(String title) {
        Paragraph heading = new Paragraph(title, PdfStyle.SUBSECTION);
        heading.setSpacingBefore(6);
        document.add(heading);
    }

    private void moreRowsNote(int total, int shown, String what) {
        if (total > shown) {
            document.add(new Paragraph("Showing the first " + shown + " of " + total + " " + what + "; the JSON file contains all of them.", PdfStyle.META));
        }
    }
}