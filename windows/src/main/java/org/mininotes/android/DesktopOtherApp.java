package org.mininotes.android;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * Mininotes on your phone…, in the menu's Mininotes part, near About (the owner, 2026-10-24: "In both platforms we should
 * present the other, in windows the android and reverse"; decision 241). One box: that Mininotes is on Android phones too
 * and keeps the same notes in step once paired, a code for the phone's camera (drawn as the pairing codes are, DesktopQr),
 * the link written out and Copy link, and Done, the one primary. The phone has the same for Windows (MainActivity.onYourPc).
 * Its parts are made here from nothing but the link, so DesktopOtherAppTest can read the code back.
 */
final class DesktopOtherApp {
    private DesktopOtherApp(){}
    /** Where Mininotes for Android is, the same on every PC. */
    static final String ANDROID_LINK="https://mininotes.org/android";
    static final String SAYS="Mininotes is also on Android phones. Once your phone and this PC are paired, they keep the same notes in step.";

    /** The code a phone's camera opens the link from. */
    static BufferedImage code() throws Exception {return DesktopQr.draw(ANDROID_LINK,220);}

    /** What the box says and holds, Done aside: the line, the code, the link and Copy link (which says so on {@code copied}). */
    static JPanel body() {
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note(SAYS,400,DesktopUi.INK,DesktopUi.BODY));
        DesktopUi.gap(body,DesktopUi.M);
        try {
            JLabel code=new JLabel(new ImageIcon(code()));code.setName("android code");code.getAccessibleContext().setAccessibleName(Say.t("The link as a code"));
            JPanel centred=new JPanel(new GridBagLayout());centred.setOpaque(false);centred.add(DesktopUi.card(null,code));
            DesktopUi.add(body,centred);DesktopUi.gap(body,DesktopUi.M);
        } catch(Exception noCode){/* the link below is enough */}
        DesktopUi.Text link=DesktopUi.note(ANDROID_LINK,400,DesktopUi.INK,DesktopUi.BODY);link.setName("android link");
        DesktopUi.add(body,link);DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.Text copied=DesktopUi.quiet(" ");copied.setName("copied");
        JButton copy=DesktopUi.button("Copy link",()->{
            try{Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ANDROID_LINK),null);copied.setText(Say.t("Link copied"));}
            catch(IllegalStateException|HeadlessException busy){copied.setText(Say.t("Could not copy. Select the link and copy it."));}});
        copy.setName("copy link");
        DesktopUi.add(body,DesktopUi.actions(copy));DesktopUi.gap(body,4);DesktopUi.add(body,copied);
        return body;
    }

    static void open(Window owner) {
        JDialog[] box={null};
        JButton done=DesktopUi.primary("Done",()->box[0].dispose());
        box[0]=DesktopUi.sheet(owner,"Mininotes on your phone",body(),DesktopUi.footer(done),true);
        box[0].setName("mininotesOnYourPhone");box[0].getRootPane().setDefaultButton(done);DesktopUi.show(box[0],460,720);
    }
}
