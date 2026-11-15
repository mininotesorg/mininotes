package org.mininotes.android;

import java.io.*;
import java.nio.file.*;
import javax.swing.*;

/**
 * Big files on the PC (see {@link Big}, decision 327): offered from where they are, never copied into the pad; one offered here
 * asked about in a box, Save or Refuse, and saved straight into Downloads, the bar saying how far it is. From somebody whose
 * files are taken without asking, saved at once.
 */
final class DesktopBig {
    private DesktopBig(){}

    /** Heard from now on: offers that come are asked about, or saved. Said once, when the window is built. */
    static void listen(Desktop pad) {
        Big.asking=one->SwingUtilities.invokeLater(()->ask(pad,one));
        Big.saving=one->SwingUtilities.invokeLater(()->save(pad,one));
    }

    // ---- sending -----------------------------------------------------------------------------------------------

    /** This file offered to that device, read from where it is when they save it. The bar says where it stands. */
    static void offer(Desktop pad,Path file,Drop.Device to){offer(pad,file,to,System.currentTimeMillis(),true);}
    /**
     * @param since when the first try was: tried again every two minutes for a day while it is not reached (Big.AGAIN_EVERY)
     * @param asked whether the person is looking: the box with the way to send it shows then, and only once
     */
    private static void offer(Desktop pad,Path file,Drop.Device to,long since,boolean asked) {
        String name=Attachment.named(file.getFileName().toString());
        if(asked){pad.status.setToolTipText(null);pad.status.setText(Say.t("Offering “{0}” to {1}…",name,to.name));}
        pad.network.submit(()->{
            long bytes=Files.size(file);
            Big.Heard heard=new Big.Heard(){
                @Override public void saved(){SwingUtilities.invokeLater(()->pad.notice(Say.t("{1} saved “{0}”",name,to.name)));}
                @Override public void refused(){SwingUtilities.invokeLater(()->pad.notice(Say.t("{1} refused “{0}”",name,to.name)));}
            };
            Big.Source source=at->{InputStream in=Files.newInputStream(file);try{in.skipNBytes(at);}catch(IOException e){in.close();throw e;}return in;};
            String said=Post.offerBig(pad.context,pad.store,pad.keys,to.address,name,Files.probeContentType(file),bytes,source,heard);
            return said==null?"":said;
        },said->{
            Retry was=WAITING.remove(file);if(was!=null)was.timer.stop();
            if(said.isEmpty()){pad.status.setText(Say.t("“{0}” offered to {1}. It goes when they press Save.",name,to.name));if(!asked)pad.notice(Say.t("“{0}” offered to {1}. It goes when they press Save.",name,to.name));return;}
            if(!Big.notReached(said)){pad.status.setText(said);DesktopUi.tell(pad.frame,Say.t("“{0}” was not sent",name),DesktopUi.note(said,380,DesktopUi.INK,DesktopUi.BODY));return;}
            // Not reached: the way to send it, and tried again by itself while Mininotes is open (the owner, 2026-11-07).
            long now=System.currentTimeMillis();
            if(pad.closing||!Big.tryAgain(since,now)){pad.status.setText(Big.gaveUp(name,to.name));pad.notice(Big.gaveUp(name,to.name));return;}
            pad.status.setText(Big.waiting(name,to.name));
            Retry next=new Retry(new Timer((int)Big.AGAIN_EVERY,e->{Retry me=WAITING.remove(file);if(me!=null){me.timer.stop();offer(pad,file,to,since,false);}}));
            next.timer.setRepeats(false);WAITING.put(file,next);next.timer.start();
            if(asked)steps(pad,file,to,name,since);
        },pad::failed);
    }

    /** Big files not reached yet, each with the timer that offers it again. */
    private static final java.util.Map<Path,Retry> WAITING=new java.util.concurrent.ConcurrentHashMap<>();
    private record Retry(Timer timer){}
    /** Whether that file waits to be offered again: for tests. */
    static boolean waits(Path file){return WAITING.containsKey(file);}

    /** The box that says how to send it: the steps, Try again now (the one primary), and Stop, which stops the tries. */
    private static void steps(Desktop pad,Path file,Drop.Device to,String name,long since) {
        JDialog[] box={null};
        JButton stop=DesktopUi.button("Stop trying",()->{box[0].dispose();Retry was=WAITING.remove(file);if(was!=null)was.timer.stop();pad.status.setText(Say.t("“{0}” was not sent",name));});
        JButton again=DesktopUi.primary("Try again",()->{box[0].dispose();Retry was=WAITING.remove(file);if(was!=null)was.timer.stop();offer(pad,file,to,since,true);});
        JButton later=DesktopUi.button("Close",()->box[0].dispose());
        box[0]=DesktopUi.sheet(pad.frame,Big.sendTitle(name,to.name),DesktopUi.note(Big.steps(to.name),400,DesktopUi.INK,DesktopUi.BODY),DesktopUi.footer(stop,later,again),false);
        box[0].getRootPane().setDefaultButton(again);
        DesktopUi.show(box[0],460,420);
    }

    // ---- receiving ---------------------------------------------------------------------------------------------

    /** Somebody offers a big file: what it is, from whom, and Save or Refuse. Said over the tray too while the window is away. */
    static void ask(Desktop pad,Big.Waiting one) {
        String size=Attachment.size(one.offer.bytes);
        String said=Say.t("{0} wants to send you “{1}” ({2}).",one.name,one.offer.name,size);
        pad.notice(said);
        JPanel body=DesktopUi.column();
        DesktopUi.add(body,DesktopUi.note(said,380,DesktopUi.INK,DesktopUi.BODY));
        DesktopUi.gap(body,DesktopUi.S);
        DesktopUi.add(body,DesktopUi.note(Say.t("Saved straight into Downloads, not kept in Mininotes."),380,DesktopUi.QUIET,DesktopUi.BODY.deriveFont(13f)));
        JDialog[] box={null};
        JButton refuse=DesktopUi.button("Refuse",()->{box[0].dispose();refuse(pad,one);});
        JButton save=DesktopUi.primary("Save",()->{box[0].dispose();save(pad,one);});
        box[0]=DesktopUi.sheet(pad.frame,Say.t("A file from {0}",one.name),body,DesktopUi.footer(refuse,save),false);
        box[0].getRootPane().setDefaultButton(save);
        DesktopUi.show(box[0],440,360);
    }

    static void refuse(Desktop pad,Big.Waiting one) {
        pad.network.submit(()->{Post.refuseBig(pad.context,pad.store,one);return null;},done->pad.status.setText(Say.t("“{0}” refused",one.offer.name)),pad::failed);
    }

    /** Saved into Downloads under its own name, a number added where that is taken; what came thrown away where it failed. */
    static void save(Desktop pad,Big.Waiting one) {
        Path folder=Path.of(System.getProperty("user.home"),"Downloads");
        String name=DesktopFiles.onDisk(one.offer.name);int dot=name.lastIndexOf('.');
        String stem=dot>0?name.substring(0,dot):name,ext=dot>0?name.substring(dot):"";
        pad.status.setToolTipText(null);pad.status.setText(Say.t("Receiving “{0}”…",one.offer.name));
        long[] said={0};
        pad.network.submit(()->{
            Files.createDirectories(folder);
            Path target=folder.resolve(name);for(int n=2;Files.exists(target);n++)target=folder.resolve(stem+" ("+n+")"+ext);
            Path part=target.resolveSibling(target.getFileName()+".part");
            try(OutputStream out=new BufferedOutputStream(Files.newOutputStream(part),1<<16)) {
                Post.saveBig(pad.context,pad.store,one,out,here->{
                    long now=System.currentTimeMillis();if(now-said[0]<500)return;said[0]=now;
                    int percent=(int)(here*100/Math.max(1,one.offer.bytes));
                    SwingUtilities.invokeLater(()->pad.status.setText(Say.t("Receiving “{0}”… {1}%",one.offer.name,String.valueOf(percent))));
                });
            } catch(IOException|RuntimeException failed){Files.deleteIfExists(part);throw failed;}
            Files.move(part,target,StandardCopyOption.ATOMIC_MOVE);
            return target;
        },target->pad.notice(Say.t("{0} is in Downloads",target.getFileName().toString())),failed->{
            org.mininotes.desktop.platform.util.Log.w("Mininotes/Big","a big file not saved: "+failed.getClass().getSimpleName());
            // The offer still stands (Big.answered runs only once it is whole): the way to get it, and Save again (2026-11-07).
            pad.status.setText(Big.notWholeTitle(one.offer.name));
            JDialog[] box={null};
            JButton again=DesktopUi.primary("Save again",()->{box[0].dispose();if(Big.waitingFor(one.offer.token)!=null)save(pad,one);else pad.status.setText(Say.t("That file is no longer offered."));});
            JButton close=DesktopUi.button("Close",()->box[0].dispose());
            box[0]=DesktopUi.sheet(pad.frame,Big.notWholeTitle(one.offer.name),DesktopUi.note(Big.notWhole(one.name),380,DesktopUi.INK,DesktopUi.BODY),DesktopUi.footer(close,again),false);
            box[0].getRootPane().setDefaultButton(again);
            DesktopUi.show(box[0],420,320);
        });
    }
}
