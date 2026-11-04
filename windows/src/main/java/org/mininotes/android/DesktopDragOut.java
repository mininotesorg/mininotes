package org.mininotes.android;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Files carried out of the window onto Explorer or the desktop (the owner, 2026-10-30: "a drag and drop directly into the
 * windows explorer should be able, both way, for a copy and for and move, so that it will delete it from the app"; decision 292).
 *
 * <p>Home's own carry (DesktopHome.Carry) moves icons between cells, pages, folders and the dock, drawn by the app; Windows'
 * drag and drop is a different thing, run by Windows, that the app cannot draw or take back. So the carry stays as it is and
 * hands over only when the pointer leaves the window: inside it nothing changes. Not a TransferHandler on the icon: setting one
 * puts a drop target on it too, which would take over the files Explorer lets go on an icon (DesktopDrops.aim). A recogniser of
 * our own, listening to nothing, starts Windows' drag from the carry's last mouse event instead.
 *
 * <p>What goes are plain copies, written first while the icon is carried (the notebook may be locked, so read as a save reads
 * them), into a folder of this PC's own beside the notebook, never inside it, so no backup takes them. A copy is what a drop
 * does; Shift held makes it a move, and only once Windows says it was moved are they put in Mininotes' bin, never erased, so a
 * slip is put back from there.
 */
final class DesktopDragOut {
    private DesktopDragOut(){}

    /** While one of our drags is out: the window does not take back, as new files, what it is itself giving. */
    static volatile boolean going;

    /** Where the copies are written: %LOCALAPPDATA%\Mininotes-dragged, beside the notebook's own folder, not in it. */
    static Path rooms(){return Path.of(System.getenv().getOrDefault("LOCALAPPDATA",System.getProperty("java.io.tmpdir")),"Mininotes-dragged");}

    /** A name for this file in that folder: its own, made fit for Windows, with a number where one is there already. */
    static Path free(Path in,String name) {
        String plain=DesktopFiles.onDisk(name);int dot=plain.lastIndexOf('.');
        String stem=dot>0?plain.substring(0,dot):plain,ext=dot>0?plain.substring(dot):"";
        Path target=in.resolve(plain);for(int n=2;Files.exists(target);n++)target=in.resolve(stem+" ("+n+")"+ext);
        return target;
    }

    /** These files as Explorer takes files, from the clipboard or a drag. */
    static Transferable fileList(List<File> files) {
        List<File> kept=List.copyOf(files);
        return new Transferable(){
            public DataFlavor[] getTransferDataFlavors(){return new DataFlavor[]{DataFlavor.javaFileListFlavor};}
            public boolean isDataFlavorSupported(DataFlavor f){return DataFlavor.javaFileListFlavor.equals(f);}
            public Object getTransferData(DataFlavor f)throws UnsupportedFlavorException{if(!isDataFlavorSupported(f))throw new UnsupportedFlavorException(f);return kept;}
        };
    }

    /**
     * One drag out, made ready as the carry starts: the copies written on the disk's thread, so they are there by the time the
     * pointer reaches the window's edge. Until they are, the carry goes on inside the window as before.
     */
    static final class Going {
        private final Desktop pad;private final List<NoteStore.Branch> things;
        private List<File> copies;
        Going(Desktop pad,List<NoteStore.Branch> things) {
            this.pad=pad;this.things=List.copyOf(things);
            pad.disk.submit(()->{
                Path all=rooms();Files.createDirectories(all);sweep(all);
                Path room=Files.createTempDirectory(all,"drag-");List<File> made=new ArrayList<>();
                for(NoteStore.Branch one:this.things) {
                    NoteStore.Held held=pad.store.file(one.id);if(held==null)continue;
                    Path copy=free(room,held.name);
                    try(OutputStream out=Files.newOutputStream(copy)){DesktopFiles.copyOut(pad.context,pad.store.fileFor(held.id).toPath(),out);}
                    made.add(copy.toFile());
                }
                return made;
            },made->copies=made.isEmpty()?null:made,e->{/* not ready: the carry goes on inside the window */});
        }
        boolean ready(){return copies!=null;}

        /** Windows' drag started from this icon, with the carry's last mouse event as what began it. */
        void drag(DesktopHome.Tile from,MouseEvent last) {
            List<File> files=copies;
            MouseEvent on=SwingUtilities.convertMouseEvent((Component)last.getSource(),last,from);
            Gesture gesture=new Gesture(from,dge->{
                try {
                    going=true;
                    // No cursor of our own: Windows shows a copy or a move as the place under the pointer and Shift say.
                    dge.startDrag(null,fileList(files),new DragSourceAdapter(){
                        @Override public void dragDropEnd(DragSourceDropEvent end){going=false;ended(files,end.getDropSuccess(),end.getDropAction());}
                    });
                } catch(InvalidDnDOperationException busy){going=false;pad.status.setText(Say.t("That did not finish. Please try again."));}
            });
            gesture.go(on);
        }

        /**
         * Let go outside: a copy is said; a move puts the files in Mininotes' bin. Explorer may move the copies itself and
         * answer with no action at all (Windows' "optimised move"), so a drop that says nothing is looked at again a moment
         * later: the copies gone from our own folder can only have been taken by a move.
         */
        private void ended(List<File> files,boolean done,int action) {
            String said=things.size()==1?"“"+things.get(0).name+"”":Say.t("{0} files",String.valueOf(things.size()));
            if(done&&action==DnDConstants.ACTION_MOVE){moved(said);return;}
            if(done){pad.status.setToolTipText(null);pad.status.setText(Say.t("{0} copied out of Mininotes",said));return;}
            javax.swing.Timer look=new javax.swing.Timer(1500,e->{boolean gone=!files.isEmpty();for(File one:files)if(one.exists())gone=false;if(gone)moved(said);});
            look.setRepeats(false);look.start();
        }
        private void moved(String said){if(pad.home!=null)pad.home.binAll(things,Say.t("{0} moved out of Mininotes and put in the bin",said));}
    }

    /**
     * Copies left by earlier drags, gone once they are an hour old: Explorer has long since read them, and the folder is ours.
     * Today's are left, since Explorer may still be copying one out.
     */
    private static void sweep(Path all) {
        long before=System.currentTimeMillis()-3_600_000L;
        try(DirectoryStream<Path> rooms=Files.newDirectoryStream(all,"drag-*")) {
            for(Path room:rooms) {
                if(Files.getLastModifiedTime(room).toMillis()>before)continue;
                try(DirectoryStream<Path> in=Files.newDirectoryStream(room)){for(Path one:in)Files.deleteIfExists(one);}
                Files.deleteIfExists(room);
            }
        } catch(IOException | RuntimeException left){/* left for the next drag */}
    }

    /** A recogniser that recognises nothing by itself: the carry says when the drag out begins. */
    private static final class Gesture extends DragGestureRecognizer {
        Gesture(Component on,DragGestureListener listener){super(DragSource.getDefaultDragSource(),on,DnDConstants.ACTION_COPY_OR_MOVE,listener);}
        @Override protected void registerListeners(){}
        @Override protected void unregisterListeners(){}
        /**
         * Begun as a copy: Windows' drag asks for one of copy, move or link to start, and the actions offered, copy or move,
         * let the place it is let go on, or Shift, make it a move.
         */
        void go(MouseEvent e){appendEvent(e);fireDragGestureRecognized(DnDConstants.ACTION_COPY,e.getPoint());}
    }
}
