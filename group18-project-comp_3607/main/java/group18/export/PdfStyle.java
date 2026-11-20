package group18.export;

// Source: https://stackoverflow.com/questions/25166533/java-write-to-pdf-with-color (colours in OpenPDF)

import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import java.awt.Color;
import java.util.Locale;

/** Colours, fonts and cell builders shared by the parts of the PDF report, so the report looks consistent. */
final class PdfStyle {

    /** Headings and key figures. */
    static final Color NAVY = new Color(31, 56, 100);
    /** Active time, successes. */
    static final Color GREEN = new Color(46, 125, 50);
    /** Idle time. */
    static final Color GREY = new Color(176, 190, 197);
    /** Errors and failures. */
    static final Color RED = new Color(198, 40, 40);
    /** Second line in charts. */
    static final Color ORANGE = new Color(230, 126, 34);
    /** Table header background. */
    static final Color HEADER_BACKGROUND = new Color(232, 236, 241);
    /** Background of summary tiles and empty bar tracks. */
    static final Color TILE_BACKGROUND = new Color(245, 247, 250);
    /** Table borders and chart grid lines. */
    static final Color GRID = new Color(205, 210, 216);
    /** Secondary text. */
    static final Color MUTED = new Color(90, 98, 110);

    /** Report title. */
    static final Font TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, NAVY);
    /** Project and student line. */
    static final Font SUBTITLE = new Font(Font.HELVETICA, 10.5f, Font.BOLD, Color.DARK_GRAY);
    /** Section headings. */
    static final Font SECTION = new Font(Font.HELVETICA, 12, Font.BOLD, NAVY);
    /** Headings inside a section. */
    static final Font SUBSECTION = new Font(Font.HELVETICA, 9.5f, Font.BOLD, Color.DARK_GRAY);
    /** Normal text. */
    static final Font BODY = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, Color.BLACK);
    /** Emphasised text. */
    static final Font BOLD = new Font(Font.HELVETICA, 8.5f, Font.BOLD, Color.BLACK);
    /** Notes and secondary lines. */
    static final Font SMALL = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, MUTED);
    /** Session details under the title. */
    static final Font META = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, MUTED);
    /** Warnings, such as a missing student ID. */
    static final Font WARNING = new Font(Font.HELVETICA, 8.5f, Font.BOLD, RED);
    /** Table column headings. */
    static final Font HEADER = new Font(Font.HELVETICA, 8, Font.BOLD, Color.DARK_GRAY);
    /** The large figure in a summary tile. */
    static final Font TILE_VALUE = new Font(Font.HELVETICA, 14, Font.BOLD, NAVY);
    /** The label of a summary tile. */
    static final Font TILE_LABEL = new Font(Font.HELVETICA, 7.5f, Font.BOLD, Color.DARK_GRAY);
    /** Successful outcomes. */
    static final Font GOOD = new Font(Font.HELVETICA, 8.5f, Font.BOLD, GREEN);
    /** Failed outcomes. */
    static final Font BAD = new Font(Font.HELVETICA, 8.5f, Font.BOLD, RED);

    private PdfStyle() {
    }

    /**
     * A table spanning the page width.
     *
     * @param relativeWidths the relative width of each column
     * @return the table
     */
    static PdfPTable table(float... relativeWidths) {
        PdfPTable table = new PdfPTable(relativeWidths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(8);
        return table;
    }

    /**
     * A grey table header cell.
     *
     * @param text the heading
     * @return the cell
     */
    static PdfPCell header(String text) {
        PdfPCell cell = cell(text, HEADER, Element.ALIGN_LEFT);
        cell.setBackgroundColor(HEADER_BACKGROUND);
        return cell;
    }

    /**
     * A left-aligned body cell.
     *
     * @param text the text; {@code null} gives an empty cell
     * @param font the font
     * @return the cell
     */
    static PdfPCell cell(String text, Font font) {
        return cell(text, font, Element.ALIGN_LEFT);
    }

    /**
     * A body cell with the given alignment.
     *
     * @param text      the text; {@code null} gives an empty cell
     * @param font      the font
     * @param alignment an {@code Element.ALIGN_*} constant
     * @return the cell
     */
    static PdfPCell cell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(GRID);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(4);
        cell.setPaddingLeft(4);
        cell.setPaddingRight(4);
        return cell;
    }

    /**
     * A cell with a main line and a smaller grey second line.
     *
     * @param main     the main text
     * @param mainFont the font of the main text
     * @param second   the second line; {@code null} or empty for none
     * @return the cell
     */
    static PdfPCell twoLineCell(String main, Font mainFont, String second) {
        Paragraph text = new Paragraph(main, mainFont);
        if (second != null && !second.isEmpty()) {
            text.add(new Phrase("\n" + second, SMALL));
        }
        PdfPCell cell = new PdfPCell(text);
        cell.setBorderColor(GRID);
        cell.setPadding(4);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    /**
     * A summary tile: a large figure with a label above and a note below.
     *
     * @param label the label, shown in capitals
     * @param value the figure
     * @param note  the note under the figure
     * @return the cell
     */
    static PdfPCell tile(String label, String value, String note) {
        Paragraph text = new Paragraph(label.toUpperCase(Locale.ROOT), TILE_LABEL);
        text.add(new Phrase("\n" + value, TILE_VALUE));
        text.add(new Phrase("\n" + note, SMALL));
        PdfPCell cell = new PdfPCell(text);
        cell.setBackgroundColor(TILE_BACKGROUND);
        cell.setBorderColor(Color.WHITE);
        cell.setBorderWidth(2);
        cell.setPadding(6);
        cell.setPaddingBottom(8);
        return cell;
    }

    /**
     * A cell with no border, used for layout.
     *
     * @param content   the content
     * @param alignment an {@code Element.ALIGN_*} constant
     * @return the cell
     */
    static PdfPCell plain(Phrase content, int alignment) {
        PdfPCell cell = new PdfPCell(content);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(1);
        return cell;
    }
}
