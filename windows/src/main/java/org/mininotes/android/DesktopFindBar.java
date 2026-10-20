// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

/**
 * Words looked for inside the open note: every place they are marked, the one in hand chosen and scrolled to, and the way
 * to the next and the one before (the owner, 2026-10-19: "when we open a file or folder from the search bar, the note
 * should open where the first occurrence of what we were looking for is, and in the note we should have a search window
 * open letting us browse through all occurrences of the searched term"; decision 216). Over the top right of the page,
 * as a browser's is; Enter and F3 go on, Shift with them goes back, Esc closes it and takes the marks away.
 */
final class DesktopFindBar extends JPanel {
    private final JTextComponent page;
    private final JTextField words=new JTextField(16);
    private final JLabel count=new JLabel(" ");
    private final List<int[]> found=new ArrayList<>();
    private final List<Object> marks=new ArrayList<>();
    private int at=-1;
    private static final Highlighter.HighlightPainter ALL=new DefaultHighlighter.DefaultHighlightPainter(new Color(255,224,130));
    private static final Highlighter.HighlightPainter HERE=new DefaultHighlighter.DefaultHighlightPainter(new Color(255,170,60));

    DesktopFindBar(JTextComponent page) {
        super(new FlowLayout(FlowLayout.LEFT,4,4));this.page=page;
        setOpaque(true);setBackground(DesktopUi.CARD);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DesktopUi.LINE),BorderFactory.createEmptyBorder(2,6,2,4)));
        words.getAccessibleContext().setAccessibleName(Say.t("Find in this note"));
        words.putClientProperty("JTextField.placeholderText",Say.t("Find in this note"));
        count.setFont(DesktopUi.BODY.deriveFont(12.5f));count.setForeground(DesktopUi.QUIET);
        JButton back=step("▲","Previous match",()->go(-1)),on=step("▼","Next match",()->go(1)),close=step("×","Close",this::close);
        add(words);add(count);add(back);add(on);add(close);
        words.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){look(true);} public void removeUpdate(DocumentEvent e){look(true);} public void changedUpdate(DocumentEvent e){}
        });
        words.addActionListener(e->go((e.getModifiers()&ActionEvent.SHIFT_MASK)!=0?-1:1));
        InputMap keys=getInputMap(WHEN_IN_FOCUSED_WINDOW);
        keys.put(KeyStroke.getKeyStroke("ESCAPE"),"close");keys.put(KeyStroke.getKeyStroke("F3"),"next");keys.put(KeyStroke.getKeyStroke("shift F3"),"previous");
        getActionMap().put("close",new AbstractAction(){public void actionPerformed(ActionEvent e){if(isVisible())close();}});
        getActionMap().put("next",new AbstractAction(){public void actionPerformed(ActionEvent e){if(isVisible())go(1);}});
        getActionMap().put("previous",new AbstractAction(){public void actionPerformed(ActionEvent e){if(isVisible())go(-1);}});
        // The note changed under it (typed in, or come from another device): found again, the one in hand kept near.
        page.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){again();} public void removeUpdate(DocumentEvent e){again();} public void changedUpdate(DocumentEvent e){}
        });
        page.addPropertyChangeListener("document",e->{close();page.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent ev){again();} public void removeUpdate(DocumentEvent ev){again();} public void changedUpdate(DocumentEvent ev){}});});
        setVisible(false);
    }

    private static JButton step(String mark,String name,Runnable action) {
        JButton b=new JButton(mark);b.setFocusPainted(false);b.setMargin(new Insets(1,6,1,6));
        b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        b.setToolTipText(Say.t(name));b.getAccessibleContext().setAccessibleName(Say.t(name));b.addActionListener(e->action.run());
        return b;
    }

    /** Shown with these words, the first place they are in chosen. */
    void show(String term) {
        setVisible(true);
        if(getParent()!=null){getParent().revalidate();getParent().repaint();}
        words.setText(term==null?"":term.trim());
        look(true);
        words.requestFocusInWindow();words.selectAll();
    }

    /** Gone, and its marks with it; the caret where the match in hand was. */
    void close() {
        clearMarks();found.clear();at=-1;
        setVisible(false);
        if(getParent()!=null){getParent().revalidate();getParent().repaint();}
        page.requestFocusInWindow();
    }

    private boolean refreshing;
    private void again(){if(isVisible()&&!refreshing)SwingUtilities.invokeLater(()->look(false));}

    /** Every place the words are, any case; from the first, or from near the one in hand. */
    private void look(boolean fromStart) {
        int near=at>=0&&at<found.size()?found.get(at)[0]:0;
        clearMarks();found.clear();
        String term=words.getText().trim().toLowerCase(Locale.ROOT);
        String text;
        try{text=page.getDocument().getText(0,page.getDocument().getLength()).toLowerCase(Locale.ROOT);}catch(Exception none){text="";}
        if(!term.isEmpty())for(int from=text.indexOf(term);from>=0;from=text.indexOf(term,from+term.length()))found.add(new int[]{from,from+term.length()});
        at=found.isEmpty()?-1:0;
        if(!fromStart&&!found.isEmpty())for(int i=0;i<found.size();i++)if(found.get(i)[0]>=near){at=i;break;}
        mark();
    }

    private void go(int by) {
        if(found.isEmpty()){look(true);return;}
        at=Math.floorMod(at+by,found.size());
        mark();
    }

    /** All marked, the one in hand more strongly, selected and scrolled to. */
    private void mark() {
        clearMarks();
        Highlighter lights=page.getHighlighter();
        for(int i=0;i<found.size();i++)try{marks.add(lights.addHighlight(found.get(i)[0],found.get(i)[1],i==at?HERE:ALL));}catch(Exception none){/* past the end */}
        count.setText(found.isEmpty()?(words.getText().isBlank()?" ":Say.t("Not found")):Say.t("{0} of {1}",String.valueOf(at+1),String.valueOf(found.size())));
        if(at>=0)try {
            refreshing=true;
            page.setCaretPosition(found.get(at)[0]);
            Rectangle r=page.modelToView2D(found.get(at)[0]).getBounds();
            r.height=Math.max(r.height,40);r.y=Math.max(0,r.y-80);r.height+=160;
            page.scrollRectToVisible(r);
        } catch(Exception none){/* not laid out yet */}
        finally{refreshing=false;}
        revalidate();repaint();
    }

    private void clearMarks(){for(Object one:marks)page.getHighlighter().removeHighlight(one);marks.clear();}

    /** How many places the words are in, and which is in hand (from 0), for tests. */
    int found(){return found.size();}
    int inHand(){return at;}
}
