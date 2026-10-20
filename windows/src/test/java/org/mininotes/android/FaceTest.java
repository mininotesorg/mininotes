package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.util.*;
import org.mininotes.desktop.platform.content.Context;

/**
 * A person's picture (decision 158): your own, set once and sent to everybody paired, kept on your card there; one picked
 * for a contact on this device, worn over theirs and never sent. Real notebooks and {@code Post.arrived}, no network.
 */
public class FaceTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private final List<NoteStore> open=new ArrayList<>();
    @After public void close(){for(NoteStore one:open)one.close();}

    private static final String A_AT="MxFaceOwner@127.0.0.1:9861",A2_AT="MxFaceOwnersPhone@127.0.0.1:9862",ANA_AT="MxFaceAna@127.0.0.1:9863",BEN_AT="MxFaceBen@127.0.0.1:9864";
    private static byte[] picture(int n){byte[] b=new byte[64];b[0]=(byte)0x89;b[1]='P';b[2]='N';b[3]='G';b[4]=13;b[5]=10;b[6]=26;b[7]=10;b[20]=(byte)n;return b;}

    private final class Device {
        final Context context;final NoteStore store;final Keys keys;final String at;
        Device(String name,String at) throws Exception {
            this.at=at;
            context=new Context(temp.newFolder(name));store=new NoteStore(context);store.getWritableDatabase();open.add(store);
            keys=new Keys(context);
            store.mySigningKey=Base64.getEncoder().encodeToString(keys.signing().getPublic().getEncoded());
            store.myAgreement=Point.shorten(keys.agreement().getPublic());store.myName=name;store.myAddress=at;
        }
        void pair(Device other,String name,boolean mine) throws Exception {
            store.pairedWith(other.at,name,mine,other.keys.agreement().getPublic().getEncoded(),other.keys.signing().getPublic().getEncoded());
        }
        Post.Landed hear(byte[] sealed){return Post.arrived(context,store,keys,sealed);}
        byte[] card(Device to,Face.Card card) throws Exception {
            return Envelope.seal(new byte[16],card.decided,System.currentTimeMillis(),Face.wrap(card),keys.signing(),to.keys.agreement().getPublic());
        }
    }

    private Device a,a2,ana,ben;
    @Before public void four() throws Exception {
        a=new Device("Test PC",A_AT);a2=new Device("Test phone",A2_AT);ana=new Device("Ana",ANA_AT);ben=new Device("Ben",BEN_AT);
        a.pair(a2,"Test phone",true);a2.pair(a,"Test PC",true);
        a.pair(ana,"Ana",false);ana.pair(a,"Test PC",false);ana.pair(ben,"Ben",false);
    }

    /** Its first four bytes are its own: MNF1 was a file's sleeve, and every file sent on its own was read as a picture. */
    @Test public void itsFirstBytesAreNoOtherCardsOwn() throws Exception {
        java.nio.file.Path root=java.nio.file.Path.of("").toAbsolutePath();
        while(root!=null&&!java.nio.file.Files.isDirectory(root.resolve("android")))root=root.getParent();
        java.util.regex.Pattern magic=java.util.regex.Pattern.compile("MAGIC=[{]([^}]*)[}]");
        String face=new String(Face.MAGIC,java.nio.charset.StandardCharsets.US_ASCII);
        try(var files=java.nio.file.Files.newDirectoryStream(root.resolve("android/app/src/main/java/org/mininotes/android"),"*.java")) {
            for(var p:files) {
                if(p.getFileName().toString().equals("Face.java"))continue;
                var m=magic.matcher(java.nio.file.Files.readString(p));
                while(m.find())assertNotEquals(p.getFileName()+" uses the same first bytes",face,m.group(1).replaceAll("[' ,]",""));
            }
        }
    }

    @Test public void aCardSaysWhatItCarriesAndNothingElse() throws Exception {
        Face.Card card=new Face.Card("key",picture(1),1234);
        byte[] wrapped=Face.wrap(new Face.Card(Base64.getEncoder().encodeToString(a.keys.signing().getPublic().getEncoded()),picture(1),1234));
        assertTrue(Face.isCard(wrapped));
        Face.Card back=Face.open(wrapped);
        assertNotNull(back);assertArrayEquals(picture(1),back.picture);assertEquals(1234,back.decided);
        assertNull("broken",Face.open(Arrays.copyOf(wrapped,wrapped.length-3)));
        assertThrows(IllegalArgumentException.class,()->Face.wrap(new Face.Card("k",new byte[40],1)));
        assertFalse(card.removed());
    }

    @Test public void yourPictureArrivesOnYourRoundThereAndAnOlderOneChangesNothing() throws Exception {
        a.store.setMyFace(picture(1),5000);
        assertArrayEquals(picture(1),a.store.faceOf(null));
        assertTrue(ana.hear(a.card(ana,a.store.myFaceCard())).contacts);
        assertArrayEquals("on Ana's phone, the PC's round wears it",picture(1),ana.store.faceOf(A_AT));
        a.store.setMyFace(picture(2),4000);
        ana.hear(a.card(ana,a.store.myFaceCard()));
        assertArrayEquals("older: nothing changes",picture(1),ana.store.faceOf(A_AT));
        a.store.setMyFace(null,9000);
        ana.hear(a.card(ana,a.store.myFaceCard()));
        assertNull("taken off later: gone there too",ana.store.faceOf(A_AT));
    }

    @Test public void fromYourOtherDeviceItIsYoursToo() throws Exception {
        a2.store.setMyFace(picture(3),6000);
        a.hear(a2.card(a,a2.store.myFaceCard()));
        assertArrayEquals(picture(3),a.store.myFace());
    }

    @Test public void aCardAboutSomebodyElseIsNotTaken() throws Exception {
        a.pair(ben,"Ben",false);
        Face.Card aboutBen=new Face.Card(Base64.getEncoder().encodeToString(ben.keys.signing().getPublic().getEncoded()),picture(4),7000);
        assertFalse(a.hear(ana.card(a,aboutBen)).contacts);
        assertNull(a.store.faceOf(BEN_AT));
    }

    @Test public void onePickedHereIsWornOverTheirsAndTakenOffGivesTheirsBack() throws Exception {
        a.store.setMyFace(picture(1),5000);
        ana.hear(a.card(ana,a.store.myFaceCard()));
        ana.store.setFaceHere(A_AT,picture(5));
        assertArrayEquals(picture(5),ana.store.faceOf(A_AT));assertTrue(ana.store.faceIsPickedHere(A_AT));
        ana.store.setFaceHere(A_AT,null);
        assertArrayEquals(picture(1),ana.store.faceOf(A_AT));assertFalse(ana.store.faceIsPickedHere(A_AT));
    }
}
