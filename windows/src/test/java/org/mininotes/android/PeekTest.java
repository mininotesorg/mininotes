// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;

/**
 * A document's card shows its first page or its first words, not only its kind (decision 208); and a long description
 * is kept to a width so it is never cut at the edge of the screen. Made-up documents only.
 */
public class PeekTest {
    private static byte[] zip(String part,String xml) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry("[Content_Types].xml"));z.write("<Types/>".getBytes());
            z.putNextEntry(new ZipEntry(part));z.write(xml.getBytes(StandardCharsets.UTF_8));}
        return out.toByteArray();
    }

    @Test public void aWordDocumentsParagraphsAreItsLines() throws Exception {
        byte[] docx=zip("word/document.xml","<w:document><w:body><w:p><w:pPr/><w:r><w:t>Trip </w:t></w:r><w:r><w:t xml:space=\"preserve\">to Lyon</w:t></w:r></w:p>"
            +"<w:p/><w:p><w:r><w:t>Bread &amp; milk</w:t></w:r></w:p></w:body></w:document>");
        assertEquals(List.of("Trip to Lyon","Bread & milk"),Peek.lines(docx,"Plan.docx","",8));
        assertTrue(Peek.previewed("Plan.docx","",docx.length));
    }

    @Test public void slidesSheetsAndPlainTextToo() throws Exception {
        assertEquals(List.of("Q3 plan"),Peek.lines(zip("ppt/slides/slide1.xml","<p:sld><a:p><a:pPr/><a:r><a:t>Q3 plan</a:t></a:r></a:p></p:sld>"),"Deck.pptx","",8));
        assertEquals(List.of("Name","Ana"),Peek.lines(zip("xl/sharedStrings.xml","<sst><si><t>Name</t></si><si><t>Ana</t></si></sst>"),"Sheet.xlsx","",8));
        assertEquals(List.of("one","two"),Peek.lines("one\r\n\r\ntwo\nthree".getBytes(StandardCharsets.UTF_8),"notes.txt","text/plain",2));
        assertTrue("not a document it can read",Peek.lines(new byte[]{1,2,3},"Plan.docx","",8).isEmpty());
        assertFalse(Peek.previewed("song.mp3","audio/mpeg",10));
        assertFalse("too big to read for a glance",Peek.previewed("big.pdf","application/pdf",Peek.MOST+1));
    }

    @Test public void aPdfsFirstPageIsDrawnAndAWordDocumentsWordsOnAPage() throws Exception {
        ByteArrayOutputStream made=new ByteArrayOutputStream();
        try(org.apache.pdfbox.pdmodel.PDDocument pdf=new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page=new org.apache.pdfbox.pdmodel.PDPage();pdf.addPage(page);
            try(org.apache.pdfbox.pdmodel.PDPageContentStream ink=new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf,page)) {
                ink.setNonStrokingColor(java.awt.Color.BLACK);ink.addRect(0,page.getMediaBox().getHeight()-200,page.getMediaBox().getWidth(),200);ink.fill();
            }
            pdf.save(made);
        }
        BufferedImage first=DesktopFileCards.page(made.toByteArray(),"Ticket.pdf","application/pdf",132,72);
        assertNotNull(first);assertEquals(132,first.getWidth());assertEquals(72,first.getHeight());
        assertEquals("the top of the page, where it is black",0x000000,first.getRGB(66,10)&0xFFFFFF);
        BufferedImage words=DesktopFileCards.page(zip("word/document.xml","<w:p><w:r><w:t>Hello</w:t></w:r></w:p>"),"a.docx","",132,72);
        assertNotNull(words);
        assertNull("not a PDF after all",DesktopFileCards.page(new byte[]{1,2,3},"broken.pdf","application/pdf",132,72));
    }

    @Test public void aLongDescriptionIsKeptToAWidthAndAShortOneAsItIs() {
        assertEquals("Close Mininotes.",DesktopMenus.wrapped("Close Mininotes."));
        String long_="The people and devices you share with, and your groups <all of them> & more.";
        String w=DesktopMenus.wrapped(long_);
        assertTrue(w,w.startsWith("<html><body style='width:")&&w.contains("&lt;all of them&gt; &amp; more."));
    }
}
