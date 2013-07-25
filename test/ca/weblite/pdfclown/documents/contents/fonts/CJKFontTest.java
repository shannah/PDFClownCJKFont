/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.weblite.pdfclown.documents.contents.fonts;

import ca.weblite.pdfclown.documents.contents.fonts.CJKFont;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import org.pdfclown.documents.Document;
import org.pdfclown.documents.Page;
import org.pdfclown.documents.contents.composition.PrimitiveComposer;
import org.pdfclown.documents.contents.fonts.Font;
import org.pdfclown.documents.contents.fonts.StandardType1Font;
import org.pdfclown.files.File;
import org.pdfclown.files.SerializationModeEnum;

/**
 *
 * @author shannah
 */
public class CJKFontTest {
    Document doc;
    File file;
    Page page;
    PrimitiveComposer composer;
    Font font;
    
    public void init() throws IOException{
        file = new File();
        doc = file.getDocument();
        page = new Page(doc);
        doc.getPages().add(page);
        composer = new PrimitiveComposer(page);
        composer.beginLocalState();
        
        /*
        font = new StandardType1Font(
                doc,
                StandardType1Font.FamilyEnum.Times,
                false,
                false
                );
                */
        //font = TCPDFFont.get(doc, "/Users/shannah/Documents/Shared/htdocs/recipedb/modules/pdfreports/jquery-pdf/tcpdf/fonts/cid0cs.php");
        //font = TCPDFFont.get(doc, "/Users/shannah/Documents/Shared/htdocs/recipedb/modules/pdfreports/jquery-pdf/tcpdf/fonts/cid0jp.php");
        
        font = CJKFont.loadJapanese(doc);
        composer.setFont(font, 50);
        //composer.setCharSpace(0);
        //composer.setWordSpace(0);
        //composer.showText("特色条目", new Point2D.Double(10,10));
        String str = "1234こんにちは世界";
        double width = font.getWidth(str, 50);
        System.out.println("Width: "+width);
        System.out.println(page.getCropBox());
                
        //composer.setCharSpace(-3.25);
        composer.showText(str, new Point2D.Double(10,10));
        composer.drawRectangle(new Rectangle2D.Double(10,10, width, 10));
        composer.fillStroke();
        
            // Write some Japanese
        Font japaneseFont = CJKFont.loadJapanese(doc);
        composer.setFont(japaneseFont, 18);
        composer.showText("1234こんにちは世界", new Point2D.Double(10,100));

        // Write some Simplified Chinese
        Font simpChineseFont = CJKFont.loadChineseSimplified(doc);
        composer.setFont(simpChineseFont, 18);
        composer.showText("特色条目", new Point2D.Double(10, 130));


        // Write some Traditional Chinese
        Font tradChineseFont = CJKFont.loadChineseTraditional(doc);
        composer.setFont(tradChineseFont, 18);
        composer.showText("弱冠擢進士第", new Point2D.Double(10, 160));

        // Write some Korean
        Font koreanFont = CJKFont.loadKorean(doc);
        composer.setFont(koreanFont, 18);
        composer.showText("위키백과, 우리 모두의 백과사전", new Point2D.Double(10,190));

        composer.end();
        composer.flush();
        file.save("UnicodeTest.pdf", SerializationModeEnum.Standard);
        
    }
    
    public static void main(String[] args) throws IOException{
        new CJKFontTest().init();
    }
}
