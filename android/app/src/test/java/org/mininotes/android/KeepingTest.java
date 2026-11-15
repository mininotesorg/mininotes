package org.mininotes.android;

import static org.junit.Assert.*;

import com.eurobuddha.maxima.core.net.Frame;
import com.eurobuddha.maxima.core.net.PrivateStreams;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.List;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Backups pushed to a device that keeps them (decision 330): each sender's in its own folder, written whole or not at all, only
 * a backup's own name, the newest the sender asked for kept and nothing else in the folder touched. Made-up bytes only.
 */
public class KeepingTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    private static byte[] made(int n,int seed){byte[] b=new byte[n];new Random(seed).nextBytes(b);return b;}

    private void push(Path root,String from,String name,byte[] file,int keep) throws Exception {
        Big.Offer offer=Big.Offer.pushing(name,Backups.kindKeeping(keep),file.length);
        assertNull(Keeping.take(root,new Big.Waiting("Mx-address",from,Big.Offer.read(offer.said()),System.currentTimeMillis()),null,null));
        try(ServerSocket door=new ServerSocket(0,4,InetAddress.getLoopbackAddress())) {
            Thread answering=new Thread(()->{
                try(Socket one=door.accept()) {
                    DataInputStream in=new DataInputStream(new BufferedInputStream(one.getInputStream()));
                    byte[] frame=Frame.readOrSkip(in,1024);
                    if(frame!=null&&Frame.typeOf(frame)==PrivateStreams.TYPE)PrivateStreams.serve(frame,one,in,new DataOutputStream(one.getOutputStream()));
                } catch(Exception broken){throw new RuntimeException(broken);}
            });
            answering.start();
            Big.push(List.of(new InetSocketAddress(InetAddress.getLoopbackAddress(),door.getLocalPort())),offer,at->{
                ByteArrayInputStream in=new ByteArrayInputStream(file);in.skip(at);return in;},5000);
            answering.join(5000);
        }
    }

    @Test public void eachSendersBackupsAreKeptWholeInItsOwnFolderAndTheOldestGo() throws Exception {
        Path root=temp.newFolder("backups").toPath();
        Files.createDirectories(root.resolve("Pixel-7"));
        Files.writeString(root.resolve("Pixel-7").resolve("notes.txt"),"keep me");
        byte[][] files=new byte[4][];
        for(int i=0;i<4;i++) {
            files[i]=made(Big.PIECE+1000*i,i);
            push(root,"Pixel 7","mininotes-backup-Pixel-7-2026110"+(i+1)+"-204000.mnbackup",files[i],2);
        }
        push(root,"Pro (renamed)","mininotes-backup-Pro-20261105-204000.mnbackup",made(10,9),2);
        Path mine=root.resolve("Pixel-7");
        assertFalse("the oldest gone",Files.exists(mine.resolve("mininotes-backup-Pixel-7-20261101-204000.mnbackup")));
        assertFalse(Files.exists(mine.resolve("mininotes-backup-Pixel-7-20261102-204000.mnbackup")));
        assertArrayEquals(files[3],Files.readAllBytes(mine.resolve("mininotes-backup-Pixel-7-20261104-204000.mnbackup")));
        assertArrayEquals(files[2],Files.readAllBytes(mine.resolve("mininotes-backup-Pixel-7-20261103-204000.mnbackup")));
        assertEquals("nothing else touched","keep me",Files.readString(mine.resolve("notes.txt")));
        assertTrue(Files.exists(root.resolve("Pro").resolve("mininotes-backup-Pro-20261105-204000.mnbackup")));
        assertEquals(3,Keeping.all(root).size());
        try(var left=Files.list(mine)){assertTrue("no part left",left.noneMatch(p->p.toString().endsWith(Keeping.PART)));}
    }

    @Test public void onlyABackupIsTaken() throws Exception {
        Path root=temp.newFolder("backups").toPath();
        Big.Offer notOne=Big.Offer.pushing("holiday.jpg",Backups.KIND,10);
        assertNotNull(Keeping.take(root,new Big.Waiting("Mx","Pro",notOne,0),null,null));
        Big.Offer notKind=Big.Offer.pushing("mininotes-backup-Pro-20261105-204000.mnbackup","image/jpeg",10);
        assertNotNull(Keeping.take(root,new Big.Waiting("Mx","Pro",notKind,0),null,null));
        // Kept in the folder its file's name says, whatever the sender is called in a list: never a path out.
        Big.Offer fine=Big.Offer.pushing("mininotes-backup-Pro-20261105-204000.mnbackup",Backups.KIND,10);
        assertNull(Keeping.take(root,new Big.Waiting("Mx","../../etc",fine,0),null,null));
        assertTrue(Files.isDirectory(root.resolve("Pro")));
        try(var all=Files.list(root)){assertEquals(1,all.count());}
        Big.stopTaking(fine.token);
    }
}
