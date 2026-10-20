package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * The note's words drawn with Windows' own text smoothing, as every other word in the window is (the owner, 2026-10-13:
 * "the text is still not sharp on a blue background"): the page drawn into a picture on blue paper, never captured from the
 * screen, and its letters' edges looked at. ClearType leaves coloured edges; the grey smoothing the page had leaves none.
 */
public class DesktopSharpTextTest {
    @Test public void thePagesWordsAreSmoothedAsWindowsSmoothsText() throws Exception {
        Object hints=Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        Assume.assumeTrue("Windows smooths text with ClearType here",hints instanceof java.util.Map<?,?> map
            &&RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB.equals(map.get(RenderingHints.KEY_TEXT_ANTIALIASING)));
        int[] coloured={0},dark={0};
        SwingUtilities.invokeAndWait(()->{
            Desktop.Paper page=new Desktop.Paper();
            page.setFont(new Font("Segoe UI",Font.PLAIN,18));page.setForeground(new Color(43,48,43));page.setBackground(new Color(211,224,241));
            page.setLineWrap(true);page.setText("Test fichier externe\nThe quick brown fox jumps over 123");page.setSize(420,90);
            JFrame host=new JFrame();host.add(page);host.pack();host.setSize(440,120);
            try {
                page.doLayout();
                BufferedImage img=new BufferedImage(420,90,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=img.createGraphics();page.paint(g);g.dispose();
                for(int y=0;y<img.getHeight();y++)for(int x=40;x<img.getWidth();x++){int c=img.getRGB(x,y),r=c>>16&255,gr=c>>8&255,b=c&255;
                    if(r+gr+b<450){dark[0]++;if(Math.max(r,Math.max(gr,b))-Math.min(r,Math.min(gr,b))>40)coloured[0]++;}}
            } finally{host.dispose();}
        });
        assertTrue("words drawn: "+dark[0],dark[0]>200);
        assertTrue("ClearType's coloured edges on the letters: "+coloured[0]+" of "+dark[0],coloured[0]>20);
    }
}
