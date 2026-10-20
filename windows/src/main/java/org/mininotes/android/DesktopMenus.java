package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * Right-click, the same way everywhere: whatever is under the pointer offers what can be done to it. A row with
 * buttons offers those buttons, so its menu can never say or do anything else.
 */
final class DesktopMenus {
    private DesktopMenus(){}

    /**
     * The menu comes up on a right-click on this or anything on it: Windows says so as the button comes up,
     * other systems as it goes down, so both are heard. Said once the row is built, since what is on it then
     * is what is listened to - a child that listens takes the clicks its parent would have had.
     */
    static void onRightClick(Component on,Consumer<MouseEvent> show) {
        MouseAdapter asked=new MouseAdapter(){
            @Override public void mousePressed(MouseEvent e){if(e.isPopupTrigger())show.accept(e);}
            @Override public void mouseReleased(MouseEvent e){if(e.isPopupTrigger())show.accept(e);}
        };
        listen(on,asked);
    }
    /**
     * A menu line that says what it does, with its symbol before the words (decision 157): the glyph Symbols gives them, the
     * same as on the phone. Words with none are words alone.
     */
    static JMenuItem item(){return new JMenuItem();}
    static JMenuItem item(String said){JMenuItem line=new JMenuItem(shortly(Say.t(said)));line.putClientProperty("words",said);symbol(line,said);described(line,said);return line;}
    /** What it does in one line, under the pointer while descriptions are on (decision 205; see Describe). */
    static void described(javax.swing.JComponent line,String said){String does=Describe.of(said);if(does!=null)line.setToolTipText(wrapped(does));}
    /**
     * A line of words kept to a width that fits beside a menu, on as many lines as it takes, so a description shown near
     * the edge of the screen is never cut (the owner, 2026-10-18: "we have to make sure the mouse-over info are fully
     * displayed"; decision 208). Short ones as they are.
     */
    static String wrapped(String words) {
        if(words==null||words.length()<=48)return words;
        String safe=words.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
        return "<html><body style='width:"+Math.round(260*DesktopUi.BODY.getSize2D()/13f)+"px'>"+safe+"</body></html>";
    }
    /** Whether descriptions show under the pointer: My profile's switch, on unless switched off. */
    static final String DESCRIBE="describe";
    static void describing(org.mininotes.desktop.platform.content.Context context){ToolTipManager.sharedInstance().setEnabled(!"false".equals(context.getSharedPreferences("settings",0).getString(DESCRIBE,"true")));}
    static JMenuItem item(String said,Icon icon){JMenuItem line=new JMenuItem(shortly(Say.t(said)),icon);line.putClientProperty("words",said);return line;}
    /**
     * A line kept to one line of a reasonable length: a name inside it - a note's first words, a long file name - made the
     * whole menu as wide as the screen (the owner, 2026-10-14, "the menu is full screen on desktop": an Undo naming a note
     * whose first line was a paragraph). The whole of it stays under the pointer.
     */
    static String shortly(String said) {
        if(said==null)return "";
        String one=said.replaceAll("\\s*[\\r\\n]+\\s*"," ").trim();
        return one.length()<=MOST?one:one.substring(0,MOST-1).trim()+"…";
    }
    /** The most characters a menu line shows. */
    static final int MOST=60;
    /** The English a line was made from, whatever language it shows: what code that picks lines by their words reads. */
    static String words(AbstractButton line){Object said=line.getClientProperty("words");return said instanceof String s?s:line.getText();}
    /** A line that is a name - a person, a file, a folder - which no symbol for a doing belongs before. */
    static JMenuItem plain(String said){return new JMenuItem(said);}
    /** The symbol for these words put on a line, or none. */
    static void symbol(AbstractButton line,String said) {
        String glyph=Symbols.of(said);
        if(glyph!=null&&DesktopIcons.known(glyph))line.setIcon(DesktopIcons.glyph(glyph,16));
    }

    private static void listen(Component c,MouseListener l) {
        c.addMouseListener(l);
        if(c instanceof Container k)for(Component one:k.getComponents())listen(one,l);
    }

    /** A row's own buttons, as a menu: each line presses its button. A null is a divider. */
    static JPopupMenu echo(AbstractButton... buttons) {
        JPopupMenu menu=new JPopupMenu();boolean divide=false;
        for(AbstractButton b:buttons) {
            if(b==null){divide=menu.getComponentCount()>0;continue;}
            if(divide){menu.addSeparator();divide=false;}
            String said=b.getText()==null||b.getText().isBlank()?b.getAccessibleContext().getAccessibleName():b.getText();
            JMenuItem line=b instanceof JCheckBox box?new JCheckBoxMenuItem(said,box.isSelected()):new JMenuItem(said);
            line.setEnabled(b.isEnabled());line.addActionListener(e->b.doClick(0));menu.add(line);
        }
        return menu;
    }

    /** A right-click on this row offers its buttons. */
    static void echoOnRightClick(JComponent row,AbstractButton... buttons) {
        onRightClick(row,e->echo(buttons).show(e.getComponent(),e.getX(),e.getY()));
    }
}
