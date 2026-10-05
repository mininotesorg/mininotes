package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.swing.*;

/**
 * Bold, italic and underline on the PC's page (docs/HOME.md, decision 76): Ctrl+B, Ctrl+I and Ctrl+U put the marks in and
 * take them out through the page's document, and the page is drawn with them, in a picture beside the gallery's.
 */
public class DesktopMarksTest {
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void thePageTakesAndDrawsTheMarks() throws Exception {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.createDirectories(SHOTS);
        Desktop.Paper[] made={null};
        SwingUtilities.invokeAndWait(()->{
            Desktop.Paper page=new Desktop.Paper();made[0]=page;
            page.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,18));page.setLineWrap(true);page.setWrapStyleWord(true);
            page.setBorder(BorderFactory.createEmptyBorder(12,42,40,38));
            page.setText("Buy milk and eggs and bread today");page.setSize(560,180);
            page.select(4,8);page.style(Marks.Style.BOLD);
            assertEquals("Buy **milk** and eggs and bread today",page.getText());
            assertEquals("milk",page.getSelectedText());
            page.select(17,21);page.style(Marks.Style.ITALIC);
            page.select(28,33);page.style(Marks.Style.UNDERLINE);
            assertEquals("Buy **milk** and *eggs* and __bread__ today",page.getText());
            // Off again where it is on.
            page.select(6,10);page.style(Marks.Style.BOLD);
            assertEquals("Buy milk and *eggs* and __bread__ today",page.getText());
            page.select(4,8);page.style(Marks.Style.BOLD);
            // A page only read takes nothing.
            page.setEditable(false);page.select(0,3);page.style(Marks.Style.UNDERLINE);
            assertEquals("Buy **milk** and *eggs* and __bread__ today",page.getText());
            page.setEditable(true);
        });
        SwingUtilities.invokeAndWait(()->{
            Desktop.Paper page=made[0];
            JFrame frame=new JFrame();frame.add(page);frame.pack();frame.setSize(600,220);
            page.doLayout();
            BufferedImage image=new BufferedImage(560,180,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();page.paint(g);g.dispose();
            try{javax.imageio.ImageIO.write(image,"png",SHOTS.resolve("76-marks.png").toFile());}catch(Exception e){throw new RuntimeException(e);}
            frame.dispose();
        });
    }
}
