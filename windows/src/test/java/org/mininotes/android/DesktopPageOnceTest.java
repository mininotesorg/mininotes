package org.mininotes.android;

import org.junit.*;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.swing.*;

/**
 * The page's words drawn once, where the page laid them out (the owner, 2026-10-22, a picture of the French README on the PC
 * with lines drawn over each other: "Vérifier" again over its own line, "L'horodatage" over "La différence", bold labels
 * cut; decision 234). Bold, italic and addresses are drawn again over the plain letters, from the page's own line view:
 * given a box one caret wider than the one Java lays the page out in, the view laid every line out again at that width, so
 * where a line wrapped one letter sooner the lines under it moved a row, and the words drawn into a bold label's place were
 * another line's. Made-up words only, in a picture beside the gallery's.
 */
public class DesktopPageOnceTest {
    private static final Path SHOTS=Path.of("build","verification","gallery");

    @Test public void stylesAreDrawnOverTheirOwnWordsAtTheWidthThePageIsLaidOutIn() throws Exception {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        Files.createDirectories(SHOTS);
        SwingUtilities.invokeAndWait(()->{
            Desktop.Paper page=new Desktop.Paper();
            page.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,18));page.setLineWrap(true);page.setWrapStyleWord(true);
            page.setBorder(BorderFactory.createEmptyBorder(12,42,40,38));
            // A first line that fits the page's width to the letter, then bold labels under it, as the README's items are.
            String first="- Horodater sur Minima inscrit maintenant que le fichier existe exactement tel quel",
                rest="\n- **Vérifier sur Minima** compare plus tard un fichier\n- **La différence :** horodater écrit la preuve une fois\n- **L'horodatage d'une note** couvre son titre";
            page.setText(first+rest);
            // On no window: the whole page is in view, so every style on it is drawn.
            try {
                // The width where the first line wraps, and one letter wider it would not: found by measuring, not guessed.
                int wraps=-1;
                for(int w=300;w<1400&&wraps<0;w++) {
                    page.setSize(w,400);page.doLayout();
                    double second=page.modelToView2D(first.length()+1).getY();
                    page.setSize(w+1,400);page.doLayout();
                    if(page.modelToView2D(first.length()+1).getY()<second)wraps=w;
                }
                assertTrue("a width where the first line just wraps",wraps>0);
                page.setSize(wraps,400);page.doLayout();
                int label=page.getText().indexOf("La diff");
                javax.swing.text.BoxView lines=(javax.swing.text.BoxView)page.getUI().getRootView(page).getView(0);
                java.awt.geom.Rectangle2D row=page.modelToView2D(label);float width=lines.getWidth();
                BufferedImage image=new BufferedImage(wraps,400,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();page.paint(g);g.dispose();
                javax.imageio.ImageIO.write(image,"png",SHOTS.resolve("234-drawn-once.png").toFile());
                // The page's letters alone, from its line view at the width Java laid it out in: no style drawn again.
                Insets in=page.getInsets();
                BufferedImage plain=new BufferedImage(wraps,400,BufferedImage.TYPE_INT_RGB);
                Graphics2D p=plain.createGraphics();p.setClip(0,0,wraps,400);p.setColor(Color.WHITE);p.fillRect(0,0,wraps,400);p.setColor(Color.BLACK);p.setFont(page.getFont());
                page.getUI().getRootView(page).paint(p,new Rectangle(in.left,in.top,Math.round(width),400-in.top-in.bottom));p.dispose();
                // Every dark pixel the page drew is a letter that is there: bold struck a hair to the right, italic slanted
                // a little, never another line's words in a label's place.
                int strays=0;
                for(int y=1;y<399;y++)for(int x=0;x<wraps;x++) {
                    if(!dark(image.getRGB(x,y)))continue;
                    boolean near=false;
                    for(int dy=-1;dy<=1&&!near;dy++)for(int dx=-3;dx<=2&&!near;dx++){int px=x+dx;if(px>=0&&px<wraps&&inked(plain.getRGB(px,y+dy)))near=true;}
                    if(!near)strays++;
                }
                assertEquals("letters drawn where the page has none",0,strays);
                assertEquals("the page's lines, after the page is drawn",width,lines.getWidth(),0.01);
                assertEquals("the bold label's row, after the page is drawn",row.getY(),page.modelToView2D(label).getY(),0.01);
            } catch(Exception e){throw new AssertionError(e);}
        });
    }

    /** Any ink at all, a letter's soft edge included; and the dark middle of a letter. */
    private static boolean inked(int rgb){int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;return r*299+g*587+b*114<200000;}
    private static boolean dark(int rgb){int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;return r*299+g*587+b*114<100000;}
}
