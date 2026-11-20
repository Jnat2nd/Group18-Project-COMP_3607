package group18.export;

import java.awt.Color;
import java.util.List;

import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPCellEvent;
import com.lowagie.text.pdf.PdfPTable;


final class SegmentedBarEvent implements PdfPCellEvent {

    /**
     * One part of the bar.
     *
     * @param weight the part's size relative to the other parts
     * @param color  its colour, or {@code null} to leave the track showing
     */
    record Segment(double weight, Color color) {
    }

    /** Space between the bar and the cell edges, in points. */
    private static final float INSET = 2;

    /** The parts of the bar, left to right. */
    private final List<Segment> segments;

    private SegmentedBarEvent(List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    /**
     * Creates a borderless cell that shows the bar.
     *
     * @param segments the parts of the bar, left to right
     * @param height   the cell height in points
     * @return the cell
     */
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
