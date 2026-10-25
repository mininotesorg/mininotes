package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * Mininotes on your phone… (decision 241): the box's code opens Mininotes for Android, the same link is written out under
 * it, Copy link is there, and the menu entry wears its symbol and says what it does under the pointer.
 */
public class DesktopOtherAppTest {
    @Test public void theCodeOpensMininotesForAndroid() throws Exception {
        assertEquals("https://mininotes.org/android",DesktopOtherApp.ANDROID_LINK);
        assertEquals(DesktopOtherApp.ANDROID_LINK,DesktopQr.read(DesktopOtherApp.code()));
    }

    @Test public void theBoxShowsTheCodeTheLinkAndCopyLink() throws Exception {
        JPanel[] body={null};SwingUtilities.invokeAndWait(()->body[0]=DesktopOtherApp.body());
        JLabel code=(JLabel)find(body[0],"android code");assertNotNull("the code is drawn",code);
        Icon icon=code.getIcon();BufferedImage drawn=new BufferedImage(icon.getIconWidth(),icon.getIconHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=drawn.createGraphics();icon.paintIcon(null,g,0,0);g.dispose();
        assertEquals(DesktopOtherApp.ANDROID_LINK,DesktopQr.read(drawn));
        Component link=find(body[0],"android link");assertNotNull("the link is written out",link);
        assertEquals(DesktopOtherApp.ANDROID_LINK,((DesktopUi.Text)link).getText());
        Component copy=find(body[0],"copy link");assertNotNull("Copy link is there",copy);
        assertEquals("Copy link",((JButton)copy).getClientProperty("words"));
    }

    @Test public void bothEntriesHaveASymbolAndADescription() {
        assertEquals("smartphone",Symbols.of("Mininotes on your phone…"));
        assertEquals("monitor",Symbols.of("Mininotes on your PC…"));
        Say.use("en");
        assertEquals("Mininotes on Android phones, with a code to scan to get it.",Describe.of("Mininotes on your phone…"));
        assertEquals("Mininotes on Windows, with the link to get it.",Describe.of("Mininotes on your PC…"));
    }

    private static Component find(Container in,String name) {
        for(Component one:in.getComponents()) {
            if(name.equals(one.getName()))return one;
            if(one instanceof Container inner){Component found=find(inner,name);if(found!=null)return found;}
        }
        return null;
    }
}
