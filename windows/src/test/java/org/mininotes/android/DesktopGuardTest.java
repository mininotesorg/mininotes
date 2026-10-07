package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.mininotes.desktop.platform.content.Context;

/**
 * The guard at the door (the owner, 2026-10-06: "We have to make sure that if we type privatenotes or private//: or anything
 * related to the private elements in a shared element, this is not shared, how can we do that?"; docs/HOME.md, decision
 * 114), on a PC with a note shared with a phone, every way a code comes in: typed a key at a time, handed over as a whole
 * word at every letter, pasted in one go, short and long, a save that cannot wait in the middle of one, in the title, and
 * arriving in a parcel from a build before the guard. Then the stored note, its versions, what is owed, every parcel sealed
 * for the phone, what the phone keeps, what goes to the clipboard and every file of the notebook: never a code nor its
 * password. And Read me still teaches it after a save, and nothing the app itself writes holds a run the guard takes.
 */
public class DesktopGuardTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private static final String PASSWORD="velvet kettle 4417",PHONE="MxGuardPhoneFixture@127.0.0.1:9441",PC="MxGuardPcFixture@127.0.0.1:9442";
    private static final String[] NEVER={"privatespace//","privatenote//","privatefolder//","private//:","private space//","private note//",PASSWORD,"velvet"};

    private static void clean(String what,String text) {
        String low=text==null?"":text.toLowerCase(Locale.ROOT);
        for(String word:NEVER)assertFalse(what+" holds "+word+": "+text,low.contains(word.toLowerCase(Locale.ROOT)));
    }

    @Test public void everyWayInAndNothingGoesOut() throws Exception {
        com.eurobuddha.maxima.core.crypto.Hashes.setSha3(Sha3::of);
        Path folder=temp.newFolder("pc").toPath();
        Desktop pad=open(folder);
        Context phoneContext=new Context(temp.newFolder("phone"));NoteStore phone=new NoteStore(phoneContext);phone.getWritableDatabase();Keys phoneKeys=new Keys(phoneContext);
        List<byte[]> sealed=new ArrayList<>();
        try {
            phone.mySigningKey=Base64.getEncoder().encodeToString(phoneKeys.signing().getPublic().getEncoded());
            pad.store.mySigningKey=Base64.getEncoder().encodeToString(pad.keys.signing().getPublic().getEncoded());
            pad.store.pairedWith(PHONE,"Test phone",false,phoneKeys.agreement().getPublic().getEncoded(),phoneKeys.signing().getPublic().getEncoded());
            phone.pairedWith(PC,"Test PC",false,pad.keys.agreement().getPublic().getEncoded(),pad.keys.signing().getPublic().getEncoded());
            NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title="Plan";n.body="Meet at noon";n.revision=1;pad.store.save(n);
            pad.store.setLevel(Sharing.Scope.PAGE,n.id,PHONE,Sharing.Level.WRITE,null);
            phone.addShare(new Sharing.Rule(Sharing.Scope.PAGE,n.id,PC,true));
            SwingUtilities.invokeAndWait(()->pad.open(n.id));settle(pad);
            Runnable send=()->{try{sealed.add(seal(pad,n.id,phoneKeys));}catch(Exception e){throw new RuntimeException(e);}};

            // 1. Typed a key at a time: caught at its colon, the password into its own field.
            end(pad,pad.page);
            for(char c:"\nprivate//:".toCharArray())type(pad,pad.page,String.valueOf(c));
            password(pad,PASSWORD);
            saveNow(pad);send.run();
            assertEquals("Meet at noon\n",onEdt(()->pad.page.getText()));

            // 2. Handed over as a whole word at every letter, as a phone's keyboard does, the password's first letters too,
            // before the field is up: only what follows the code reaches the field.
            end(pad,pad.page);
            String word="privatespace//:velvet";
            SwingUtilities.invokeAndWait(()->{
                int from=pad.page.getCaretPosition();
                for(int i=1;i<=word.length();i++){
                    int to=Math.min(pad.page.getDocument().getLength(),from+i-1);
                    try{((javax.swing.text.AbstractDocument)pad.page.getDocument()).replace(from,to-from,word.substring(0,i),null);}catch(Exception e){throw new RuntimeException(e);}
                }
            });
            await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
            assertEquals("only the password's first letters wait in its field","velvet",onEdt(()->new String(pad.privately.asking.getPassword())));
            SwingUtilities.invokeAndWait(()->pad.privately.asking.postActionEvent());
            dismissAgain(pad);
            assertEquals("Meet at noon\n",onEdt(()->pad.page.getText()));
            saveNow(pad);send.run();

            // 3. Pasted in one go, short: the code and its password out of the page at once, the password in its field.
            end(pad,pad.page);
            SwingUtilities.invokeAndWait(()->pad.page.replaceSelection("private//:"+PASSWORD));
            await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
            assertEquals(PASSWORD,onEdt(()->new String(pad.privately.asking.getPassword())));
            assertEquals("Meet at noon\n",onEdt(()->pad.page.getText()));
            SwingUtilities.invokeAndWait(()->pad.privately.asking.postActionEvent());
            saveNow(pad);send.run();

            // 4. Pasted long, which the catching leaves to the guard: the page shows at once what is kept, the caret sensible.
            end(pad,pad.page);
            SwingUtilities.invokeAndWait(()->pad.page.replaceSelection("Notes pasted from a long message from somebody, with privatespace//:"+PASSWORD+" in it\nBring water"));
            saveNow(pad);send.run();
            assertEquals("Meet at noon\nNotes pasted from a long message from somebody, with \nBring water",onEdt(()->pad.page.getText()));
            assertEquals(onEdt(()->pad.page.getDocument().getLength()),onEdt(()->pad.page.getCaretPosition()));

            // 5. A save that cannot wait in the middle of a code typed a key at a time: the half code goes, never written.
            for(char c:" privatespace//".toCharArray())type(pad,pad.page,String.valueOf(c));
            SwingUtilities.invokeAndWait(()->pad.save(()->{}));settle(pad);send.run();
            assertFalse(pad.store.get(n.id).body.contains("privatespace"));

            // 6. In the title, pasted.
            SwingUtilities.invokeAndWait(()->{pad.title.requestFocusInWindow();pad.title.setCaretPosition(pad.title.getText().length());});
            SwingUtilities.invokeAndWait(()->pad.title.replaceSelection(" and privatespace//:"+PASSWORD+" also, which is long enough"));
            saveNow(pad);send.run();
            assertEquals("Plan and ",onEdt(()->pad.title.getText()));

            // 7. Arriving from the phone, written by a build before the guard: the code in its title and body.
            NoteStore.Note now=pad.store.get(n.id);
            ByteArrayOutputStream raw=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(raw);
            out.write(Parcel.MAGIC);out.writeBoolean(true);
            for(String one:new String[]{"","","","","Plan privatefolder//:"+PASSWORD,now.body+"\nFrom the phone private note//:"+PASSWORD+"\nThe end"})Parcel.put(out,one,Parcel.TEXT_MOST);
            byte[] old=Envelope.seal(page(n.id),now.revision+1,1,raw.toByteArray(),phoneKeys.signing(),pad.keys.agreement().getPublic());
            Post.arrived(pad.context,pad.store,pad.keys,old);settle(pad);
            NoteStore.Note arrived=pad.store.get(n.id);
            assertTrue("it arrived",arrived.body.endsWith("From the phone \nThe end"));
            send.run();

            // Everything kept, owed and sealed, and what the phone makes of it.
            NoteStore.Note kept=pad.store.get(n.id);
            clean("the title",kept.title);clean("the note",kept.body);
            for(NoteStore.Version v:pad.store.versions(n.id)){clean("a version",v.title);clean("a version",v.body);}
            for(Outbox.Wait w:pad.store.owed(NoteStore.Branch.Kind.PAGE,n.id))assertEquals(PHONE,w.address);
            assertTrue(sealed.size()>=7);
            for(byte[] one:sealed)Post.arrived(phoneContext,phone,phoneKeys,one);
            NoteStore.Note there=phone.get(n.id);
            assertNotNull("it reached the phone",there);
            clean("the phone's note",there.title);clean("the phone's note",there.body);
            for(NoteStore.Version v:phone.versions(n.id)){clean("the phone's versions",v.title);clean("the phone's versions",v.body);}
            // And the clipboard: Copy the text. Whatever was on it is put back after.
            java.awt.datatransfer.Clipboard board=java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            java.awt.datatransfer.Transferable was=board.getContents(null);
            try {
                SwingUtilities.invokeAndWait(()->{pad.page.setText(pad.page.getText()+"\nprivatespace//:"+PASSWORD);pad.copyNote();});
                clean("the clipboard",(String)board.getData(java.awt.datatransfer.DataFlavor.stringFlavor));
            } finally{if(was!=null)try{board.setContents(was,null);}catch(IllegalStateException busy){/* as it is */}}
            saveNow(pad);
        } finally{close(pad);phone.close();}
        for(Path file:all(folder)){String found=DesktopPrivateTest.find(file,NEVER);assertNull(found+" in "+folder.relativize(file),found);}
    }

    /** Read me teaches the codes in words the guard leaves whole, and still does after it is written in and saved. */
    @Test public void readMeStillTeachesItAfterASave() throws Exception {
        Path folder=temp.newFolder("readme").toPath();
        Desktop pad=open(folder);
        try {
            assertSame("the guard leaves Read me whole",NoteStore.READ_ME_SAYS,PrivateCode.scrub(NoteStore.READ_ME_SAYS));
            assertTrue(NoteStore.READ_ME_SAYS.contains("type the word privatespace, then //: and your password, all together"));
            assertTrue(NoteStore.READ_ME_SAYS.contains("type the word private, then //: and your password, all together"));
            assertTrue("the old one spelled them whole",PrivateCode.holds(NoteStore.READ_ME_0_3_008));
            NoteStore.Note n=new NoteStore.Note();n.book=Things.HOME;n.title=NoteStore.READ_ME;n.body=NoteStore.READ_ME_SAYS;n.revision=1;pad.store.save(n);
            SwingUtilities.invokeAndWait(()->pad.open(n.id));settle(pad);
            end(pad,pad.page);type(pad,pad.page,"\nMy own line");
            saveNow(pad);
            assertEquals(NoteStore.READ_ME_SAYS+"\nMy own line",pad.store.get(n.id).body);
            // An old Read me, as 0.3.008 wrote it and as the guard leaves it once saved again: both said again.
            for(String was:new String[]{NoteStore.READ_ME_0_3_008,PrivateCode.scrub(NoteStore.READ_ME_0_3_008)}) {
                NoteStore.Note old=new NoteStore.Note();old.book=Things.HOME;old.title=NoteStore.READ_ME;old.body="";old.revision=1;
                pad.store.save(old);
                // As a build before the guard wrote it: straight into the notebook, which save would not do.
                pad.store.getWritableDatabase().execSQL("UPDATE notes SET body=? WHERE id=?",new Object[]{was,old.id});
                assertEquals(was,pad.store.get(old.id).body);
                assertTrue(pad.store.readMeAgain());
                assertEquals(NoteStore.READ_ME_SAYS,pad.store.get(old.id).body);
                for(NoteStore.Version v:pad.store.versions(old.id))assertFalse(PrivateCode.holds(v.body));
            }
        } finally{close(pad);}
    }

    /**
     * Nothing the app writes, in either app, is a run the guard would take out: every string in the sources, comments aside,
     * looked at, but the codes' own list in PrivateCode.
     */
    @Test public void nothingTheAppWritesHoldsACode() throws Exception {
        int looked=0;
        for(Path root:new Path[]{Paths.get("src","main","java"),Paths.get("..","android","app","src","main","java")}) {
            for(Path file:all(root)) {
                if(!file.toString().endsWith(".java")||file.getFileName().toString().equals("PrivateCode.java"))continue;
                // The 0.3.008 Read me is kept word for word only to know it again and say the new one over it, never written.
                boolean notes=file.getFileName().toString().equals("NoteStore.java");
                for(String said:strings(Files.readString(file,StandardCharsets.UTF_8))){
                    if(notes&&NoteStore.READ_ME_0_3_008.contains(said.replace("\\u2026","\u2026")))continue;
                    looked++;assertFalse(file.getFileName()+": "+said,PrivateCode.holds(said));
                }
            }
        }
        assertTrue("the sources were read: "+looked,looked>5000);
        for(String one:new String[]{NoteStore.READ_ME_SAYS,NoteStore.FIRST_NOTE_SAYS,PrivateSpace.saying(false,3),PrivateSpace.saying(false,1)})assertFalse(PrivateCode.holds(one));
    }

    /** The string literals of a Java source, its comments left out, its escapes as they are written. */
    static List<String> strings(String source) {
        List<String> out=new ArrayList<>();
        for(int i=0;i<source.length();i++) {
            char c=source.charAt(i);
            if(c=='/'&&i+1<source.length()&&source.charAt(i+1)=='/'){while(i<source.length()&&source.charAt(i)!='\n')i++;continue;}
            if(c=='/'&&i+1<source.length()&&source.charAt(i+1)=='*'){int end=source.indexOf("*/",i+2);i=end<0?source.length():end+1;continue;}
            if(c=='\''){i++;while(i<source.length()&&source.charAt(i)!='\''){if(source.charAt(i)=='\\')i++;i++;}continue;}
            if(c=='"') {
                if(source.startsWith("\"\"\"",i)){int end=source.indexOf("\"\"\"",i+3);out.add(source.substring(i+3,end<0?source.length():end));i=end<0?source.length():end+2;continue;}
                StringBuilder s=new StringBuilder();i++;
                while(i<source.length()&&source.charAt(i)!='"'){if(source.charAt(i)=='\\'){s.append(source,i,i+2);i+=2;continue;}s.append(source.charAt(i));i++;}
                out.add(s.toString());
            }
        }
        return out;
    }

    // ---- helpers --------------------------------------------------------------------------------------------------------

    private static byte[] page(String id){UUID u=UUID.fromString(id);return java.nio.ByteBuffer.allocate(16).putLong(u.getMostSignificantBits()).putLong(u.getLeastSignificantBits()).array();}
    /** The note as this PC seals it for the phone now. */
    private static byte[] seal(Desktop pad,String id,Keys to) throws Exception {
        NoteStore.Note kept=pad.store.get(id);
        Parcel.Sent going=new Parcel.Sent("","","","",kept.title,kept.body,true,Collections.<Parcel.Member>emptyList(),"PAGE",id,false,-1L,false,
            List.of(),1L,List.of(),pad.store.steps(pad.store.above(id)),"",null);
        byte[] plain=Parcel.wrap(going,Envelope.MAX_TEXT);
        for(String word:NEVER)assertFalse("a sealed parcel holds "+word,DesktopPrivateTest.contains(plain,word));
        return Envelope.seal(page(id),kept.revision,1,plain,pad.keys.signing(),to.agreement().getPublic());
    }
    private static void password(Desktop pad,String said) throws Exception {
        await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
        SwingUtilities.invokeAndWait(()->{pad.privately.asking.setText(said);pad.privately.asking.postActionEvent();});
        pad.privately.settle();SwingUtilities.invokeAndWait(()->{});
    }
    /** privatespace//: with a password nothing answers to asks it once more: that second field is let go. */
    private static void dismissAgain(Desktop pad) throws Exception {
        pad.privately.settle();
        await(()->onEdt(()->pad.privately.asking!=null&&pad.privately.asking.isEditable()));
        SwingUtilities.invokeAndWait(()->{pad.privately.asking.setText("not the same");pad.privately.asking.postActionEvent();});
        pad.privately.settle();SwingUtilities.invokeAndWait(()->{});
        assertFalse(onEdt(()->pad.privately.showing()));
    }
    private static void end(Desktop pad,JTextComponent field) throws Exception {
        SwingUtilities.invokeAndWait(()->{field.requestFocusInWindow();field.setCaretPosition(field.getDocument().getLength());});
    }
    private static void saveNow(Desktop pad) throws Exception {SwingUtilities.invokeAndWait(()->pad.save(null));settle(pad);}
    private static void type(Desktop pad,JTextComponent field,String text) throws Exception {
        for(char c:text.toCharArray())SwingUtilities.invokeAndWait(()->field.replaceSelection(String.valueOf(c)));
    }
    private static Desktop open(Path folder) throws Exception {
        Desktop[] app=new Desktop[1];
        SwingUtilities.invokeAndWait(()->{try{app[0]=new Desktop(folder,true);app[0].show();}catch(Exception e){throw new RuntimeException(e);}});
        Desktop pad=app[0];
        await(()->pad.page.isEditable()&&pad.store.latest()!=null&&pad.privately.slots()!=null);
        settle(pad);
        return pad;
    }
    private static void close(Desktop pad) throws Exception {SwingUtilities.invokeAndWait(()->pad.shutdown(false));await(()->!pad.frame.isDisplayable());}
    private static void settle(Desktop pad) throws Exception {
        for(int i=0;i<3;i++){pad.disk.flush(10000);pad.privately.settle();SwingUtilities.invokeAndWait(()->{});}
        Thread.sleep(150);
    }
    private static List<Path> all(Path root) throws Exception {try(var walk=Files.walk(root)){return walk.filter(Files::isRegularFile).toList();}}
    private static <T> T onEdt(Callable<T> read) throws Exception {
        Object[] got={null};Exception[] failed={null};
        SwingUtilities.invokeAndWait(()->{try{got[0]=read.call();}catch(Exception e){failed[0]=e;}});
        if(failed[0]!=null)throw failed[0];
        @SuppressWarnings("unchecked") T value=(T)got[0];return value;
    }
    private static void await(Callable<Boolean> condition) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
        while(System.nanoTime()<until){if(condition.call())return;Thread.sleep(40);}fail("Desktop did not finish in time");
    }
}
