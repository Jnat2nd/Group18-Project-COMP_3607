package group18.export;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

final class PageFooter extends PdfPageEventHelper {

    private final String text;

    PageFooter(String text) {
        this.text = text;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte canvas = writer.getDirectContent();
        float y = document.bottom() - 22;
        canvas.saveState();
        canvas.setColorStroke(PdfStyle.GRID);
        canvas.setLineWidth(0.5f);
        canvas.moveTo(document.left(), y + 10);
        canvas.lineTo(document.right(), y + 10);
        canvas.stroke();
        canvas.restoreState();
        ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(text, PdfStyle.SMALL),document.left(), y, 0);
        ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, new Phrase("Page " + writer.getPageNumber(), PdfStyle.SMALL), document.right(), y, 0);
    }
}