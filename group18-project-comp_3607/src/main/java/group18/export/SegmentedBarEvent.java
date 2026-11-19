package group18.export;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;
import java.util.List;

final class SegmentedBarEvent implements PdfPCellEvent {

    record Segment(double weight, Color color) {
    }

    private static final float INSET = 2;

    private final List<Segment> segments;

    private SegmentedBarEvent(List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    static PdfPCell cell(List<Segment> segments, float height) {
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(height);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setCellEvent(new SegmentedBarEvent(segments));
        return cell;
    }

    @Override
    public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
        PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
        float x = position.getLeft() + INSET;
        float y = position.getBottom() + INSET;
        float width = position.getWidth() - 2 * INSET;
        float height = position.getHeight() - 2 * INSET;
        canvas.saveState();
        canvas.setColorFill(PdfStyle.TILE_BACKGROUND);
        canvas.rectangle(x, y, width, height);
        canvas.fill();
        double total = segments.stream().mapToDouble(Segment::weight).sum();
        if (total > 0) {
            for (Segment segment : segments) {
                float segmentWidth = (float) (width * segment.weight() / total);
                if (segment.color() != null && segmentWidth > 0) {
                    canvas.setColorFill(segment.color());
                    canvas.rectangle(x, y, segmentWidth, height);
                    canvas.fill();
                }
                x += segmentWidth;
            }
        }
        canvas.restoreState();
    }
}
