package org.mininotes.android;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.Test;

/** The Backup page's rules (decision 329): when each is due, the names given, which old ones go, what is kept. */
public class BackupsTest {
    private static final ZoneId PARIS=ZoneId.of("Europe/Paris");
    private static long at(int y,int mo,int d,int h,int mi){return Backups.at(LocalDateTime.of(y,mo,d,h,mi),PARIS);}

    @Test public void eachDayIsDueAtItsHourAndOneMissedIsDueAtOnce() {
        Backups.Place p=new Backups.Place();p.often=Backups.Often.DAILY;p.hour=20;p.set=at(2026,11,5,10,0);p.last=0;
        assertEquals(at(2026,11,5,20,0),Backups.next(p,PARIS));
        assertFalse(Backups.due(p,at(2026,11,5,19,59),PARIS));
        assertTrue(Backups.due(p,at(2026,11,5,20,0),PARIS));
        assertTrue("the PC was off at 20:00: due when it starts",Backups.due(p,at(2026,11,7,9,0),PARIS));
        p.last=at(2026,11,5,20,1);
        assertEquals(at(2026,11,6,20,0),Backups.next(p,PARIS));
    }
    @Test public void eachWeekAndEachMonth() {
        Backups.Place p=new Backups.Place();p.often=Backups.Often.WEEKLY;p.weekday=1;p.hour=8;p.set=at(2026,11,5,10,0);// a Thursday
        assertEquals(at(2026,11,9,8,0),Backups.next(p,PARIS));
        p.last=at(2026,11,9,8,0);assertEquals(at(2026,11,16,8,0),Backups.next(p,PARIS));
        p.often=Backups.Often.MONTHLY;p.monthday=1;p.last=0;
        assertEquals(at(2026,12,1,8,0),Backups.next(p,PARIS));
        p.monthday=28;assertEquals(at(2026,11,28,8,0),Backups.next(p,PARIS));
    }
    @Test public void askedAndOnCloseAreNeverDueByTheClockAndOnCloseNotTooOften() {
        Backups.Place p=new Backups.Place();p.often=Backups.Often.ASKED;
        assertEquals(-1,Backups.next(p,PARIS));assertFalse(Backups.due(p,System.currentTimeMillis()+10L*86400000,PARIS));
        p.often=Backups.Often.ON_CLOSE;p.tried=0;
        assertTrue(Backups.dueOnClose(p,at(2026,11,5,10,0)));
        p.tried=at(2026,11,5,10,0);
        assertFalse(Backups.dueOnClose(p,at(2026,11,5,10,5)));
        assertTrue(Backups.dueOnClose(p,at(2026,11,5,10,11)));
    }
    /** The switch on each place (the owner, 2026-11-07): off, nothing by itself. Kept as set: DesktopBackupsTest (org.json). */
    @Test public void aPlaceSwitchedOffIsNeverMadeOrPushedByItself() {
        Backups.Place p=new Backups.Place();p.often=Backups.Often.DAILY;p.hour=20;p.set=at(2026,11,5,10,0);
        p.on=false;
        assertFalse(Backups.due(p,at(2026,11,7,9,0),PARIS));
        assertEquals("no Next: line while off","",Backups.nextSaid(p,PARIS));
        assertFalse(Backups.timed(p));
        p.often=Backups.Often.ON_CLOSE;assertFalse(Backups.dueOnClose(p,at(2026,11,5,10,0)));
        p.device="Mx-device";assertFalse(Backups.pushAgain(p,at(2026,11,7,9,0)));
        p.on=true;
        assertTrue(Backups.dueOnClose(p,at(2026,11,5,10,0)));
        assertTrue(Backups.pushAgain(p,at(2026,11,7,9,0)));
        assertTrue(Backups.timed(p));
    }
    @Test public void aFailureIsTriedAgainAfterHalfAnHour() {
        Backups.Place p=new Backups.Place();p.often=Backups.Often.DAILY;p.hour=20;p.set=at(2026,11,5,10,0);
        p.tried=at(2026,11,5,20,0);p.said=Backups.Said.NO_FOLDER;
        assertFalse(Backups.due(p,at(2026,11,5,20,20),PARIS));
        assertTrue(Backups.due(p,at(2026,11,5,20,31),PARIS));
    }
    @Test public void namesAndWhichOldOnesGo() {
        String name=Backups.fileName("Pixel 7 Pro",at(2026,11,5,20,40),PARIS);
        assertEquals("mininotes-backup-Pixel-7-Pro-20261105-204000.mnbackup",name);
        assertEquals("device",Backups.safe("  "));assertEquals("Ecran",Backups.safe("Ecran"));
        List<String> folder=List.of(
            "mininotes-backup-PC-20261101-200000.mnbackup","mininotes-backup-PC-20261102-200000.mnbackup",
            "mininotes-backup-PC-20261103-200000.mnbackup","mininotes-backup-PC-20261104-200000.mnbackup",
            "mininotes-backup-Phone-20261101-200000.mnbackup","holiday.jpg","mininotes-backup-locked.mnbackup",
            "mininotes-backup-PC-20261101-200000.mnbackup.tmp");
        assertEquals(List.of("mininotes-backup-PC-20261102-200000.mnbackup","mininotes-backup-PC-20261101-200000.mnbackup"),Backups.beyond(folder,"PC",2));
        assertTrue("never more than there are",Backups.beyond(folder,"PC",10).isEmpty());
    }
    /** Pushed to a device (decision 330): only a backup's own name is taken, and the sender says how many to keep. */
    @Test public void onlyABackupsNameIsTakenAndTheKindSaysHowManyToKeep() {
        String name=Backups.fileName("Pixel 7 Pro",at(2026,11,5,20,40),PARIS);
        assertTrue(Backups.isBackupName(name));
        assertEquals("Pixel-7-Pro",Backups.deviceIn(name));
        for(String not:new String[]{"../mininotes-backup-x-20261105-204000.mnbackup","mininotes-backup-a/b-20261105-204000.mnbackup",
                "mininotes-backup--20261105-204000.mnbackup","mininotes-backup-x-2026110-204000.mnbackup","holiday.jpg",
                "mininotes-backup-x-20261105-204000.mnbackup.exe","mininotes-backup-"+"y".repeat(41)+"-20261105-204000.mnbackup",null})
            assertFalse(String.valueOf(not),Backups.isBackupName(not));
        assertEquals(10,Backups.keepIn(Backups.kindKeeping(10)));
        assertEquals(Backups.KEEP_MOST,Backups.keepIn(Backups.KIND+";keep=999"));
        assertEquals(Backups.KEEP,Backups.keepIn(Backups.KIND));assertEquals(Backups.KEEP,Backups.keepIn("application/x-mininotes-backup;keep=x"));
    }

    @Test public void aFingerprintChangesWithTheNotesAndWithTheFilesOnlyWhereTheyGo() {
        String a=Backups.fingerprint("notes",List.of("f1"),Backups.What.EVERYTHING);
        assertNotEquals(a,Backups.fingerprint("notes!",List.of("f1"),Backups.What.EVERYTHING));
        assertNotEquals(a,Backups.fingerprint("notes",List.of("f1","f2"),Backups.What.EVERYTHING));
        assertEquals(Backups.fingerprint("notes",List.of("f1"),Backups.What.NO_FILES),Backups.fingerprint("notes",List.of("f2"),Backups.What.NO_FILES));
    }

    // Step 3 of decision 329: a key file, and this device only.
    @Test public void aKeyFileHoldsTheSecretThatOpensItsBackupsAndNothingElseIsOne() throws Exception {
        String secret=Backups.newSecret();
        assertNotEquals(secret,Backups.newSecret());
        byte[] file=Backups.keyFile(secret).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(secret,Backups.secretIn(file));
        assertEquals(secret,Backups.secretIn(Backups.keyFile(secret).replace("\n","\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertNull(Backups.secretIn("a note\nnot a key".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertNull(Backups.secretIn(null));
        assertEquals("mininotes-backup-key-Pixel-7.mnkey",Backups.keyFileName("Pixel 7"));
        // The lock is made with the secret as a password is, so the secret, and only it, opens the backups.
        Vault.Made made=Vault.make(secret.toCharArray(),10_000);
        assertArrayEquals(made.key,Backups.openWith(made.kept,List.of("wrong-one-wrong-one-wrong",secret)));
        assertNull(Backups.openWith(made.kept,List.of(Backups.newSecret())));
        assertEquals(2,Backups.withHere(Backups.withHere(java.util.Set.of(),secret),Backups.newSecret()).size());
    }

    @Test public void theNewWaysToOpenComeAfterTheOthersSoKeptPlacesReadTheSame() {
        assertEquals(5,Backups.opensNames(true).length);assertEquals(4,Backups.opensNames(false).length);
        assertEquals(Backups.Opens.KEY_FILE,Backups.opensAt(3,true));assertEquals(Backups.Opens.DEVICE,Backups.opensAt(3,false));
        assertEquals(4,Backups.indexOf(Backups.Opens.DEVICE,true));
        // Kept by name: those kept before read as they were.
        assertEquals(List.of("NOTEBOOK","PASSWORD","WORDS","KEY_FILE","DEVICE"),java.util.Arrays.stream(Backups.Opens.values()).map(Enum::name).toList());
    }
}
