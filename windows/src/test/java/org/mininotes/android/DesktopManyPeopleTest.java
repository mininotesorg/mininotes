package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * A note that reaches many (the owner, 2026-10-15: "I can imagine having 10 or more"): a few rounds, those that need a look
 * first, then one +N that lists everybody. Made-up names, drawn from the components themselves, never from the screen.
 */
public class DesktopManyPeopleTest {
    private static final Path SHOTS=Path.of("build","verification","gallery");
    private static final String[] NAMES={"Ana","Ben","Chloe","Dev","Emma","Farid","Gwen","Hugo","Ines","Jun","Kofi","Lea"};

    private static List<SyncStatus.Person> people(int n) {
        List<SyncStatus.Person> all=new ArrayList<>();
        for(int i=0;i<n;i++)all.add(new SyncStatus.Person("Mx"+NAMES[i]+"@127.0.0.1:"+(9000+i),NAMES[i],i==5?SyncMark.SENT:i==8?SyncMark.STUCK:SyncMark.GONE));
        return all;
    }

    @Test public void fewAreAllDrawnAndNeverAPlusOne() {
        assertEquals(4,SyncStatus.roundsShown(people(4),SyncStatus.PC_ROUNDS).size());
        assertEquals(5,SyncStatus.roundsShown(people(5),SyncStatus.PC_ROUNDS).size());
        assertEquals(3,SyncStatus.roundsShown(people(6),SyncStatus.PHONE_ROUNDS).size());
    }

    @Test public void thoseThatNeedALookComeFirst() {
        List<SyncStatus.Person> shown=SyncStatus.roundsShown(people(12),SyncStatus.PC_ROUNDS);
        assertEquals(List.of("Farid","Ines","Ana","Ben"),shown.stream().map(SyncStatus.Person::name).toList());
    }

    @Test public void manyAreAFewRoundsAndAPlusThatListsEverybody() throws Exception {
        Files.createDirectories(SHOTS);
        SwingUtilities.invokeAndWait(()->{
            try {
                JPanel line=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));line.setBackground(DesktopUi.PAPER);
                List<SyncStatus.Person> who=people(12);
                DesktopMark.people(line,who,24,null,null,p->p.name().equals("Ana")?Tint.NONE:Tint.NONE);
                assertEquals("four rounds and the +8",5,line.getComponentCount());
                JLabel more=(JLabel)line.getComponent(4);
                assertEquals(Say.t("Everybody it reaches: {0}","12"),more.getAccessibleContext().getAccessibleName());
                JPopupMenu box=DesktopMark.everybody(who,24,null,null,null,"Everybody it reaches: 12");
                JFrame frame=new JFrame();frame.setUndecorated(true);
                JPanel both=new JPanel(new BorderLayout(0,8));both.setBackground(DesktopUi.PAPER);both.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
                both.add(line,BorderLayout.NORTH);
                JPanel list=(JPanel)((JScrollPane)box.getComponent(0)).getViewport().getView();
                assertEquals("the heading and twelve lines",13,list.getComponentCount());
                both.add(box.getComponent(0),BorderLayout.CENTER);
                frame.setContentPane(both);frame.pack();frame.setSize(460,Math.min(frame.getHeight(),520));frame.validate();
                var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                var g=image.createGraphics();both.printAll(g);g.dispose();
                javax.imageio.ImageIO.write(image,"png",SHOTS.resolve("many-people.png").toFile());
                frame.dispose();
            } catch(Exception e){throw new RuntimeException(e);}
        });
    }
}
