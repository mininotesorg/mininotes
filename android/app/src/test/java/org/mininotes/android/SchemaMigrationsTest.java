package org.mininotes.android;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class SchemaMigrationsTest {
    private static final Pattern INDEX=Pattern.compile("CREATE INDEX (?:IF NOT EXISTS )?([A-Za-z0-9_]+)");
    private static Set<String> indexes(List<String> statements) {
        Set<String> names=new TreeSet<>();
        for(String statement:statements){Matcher m=INDEX.matcher(statement);while(m.find())names.add(m.group(1));}
        return names;
    }

    @Test public void everyReleasedVersionCanStepForward() {
        for(int version=1;version<SchemaMigrations.VERSION;version++)
            assertFalse("Version "+version+" has no upgrade step",SchemaMigrations.upgrade(version,version+1).isEmpty());
    }
    @Test public void anOldInstallUpgradesThroughEveryStepInOrder() {
        List<String> stepByStep=new ArrayList<>();
        for(int version=1;version<SchemaMigrations.VERSION;version++)stepByStep.addAll(SchemaMigrations.upgrade(version,version+1));
        assertEquals(stepByStep,SchemaMigrations.upgrade(1,SchemaMigrations.VERSION));
    }
    @Test public void aCurrentInstallHasNothingToApply() {
        assertTrue(SchemaMigrations.upgrade(SchemaMigrations.VERSION,SchemaMigrations.VERSION).isEmpty());
    }
    @Test public void aFreshInstallEndsWithTheIndexesAnUpgradedOneHas() {
        assertEquals(indexes(SchemaMigrations.create()),indexes(SchemaMigrations.upgrade(1,SchemaMigrations.VERSION)));
    }
    @Test public void freshInstallCreatesTheNotesTable() {
        assertTrue(SchemaMigrations.create().get(0).startsWith("CREATE TABLE notes("));
    }
    @Test public void filesKeptWithANoteExistBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String upgraded=String.join("\n",SchemaMigrations.upgrade(1,SchemaMigrations.VERSION));
        for(String sql:new String[]{fresh,upgraded}) {
            assertTrue("no files table",sql.contains("CREATE TABLE IF NOT EXISTS files("));
            for(String column:new String[]{"note TEXT NOT NULL","name TEXT NOT NULL","kind TEXT NOT NULL",
                                           "bytes INTEGER NOT NULL","added INTEGER NOT NULL"})
                assertTrue(column,sql.contains(column));
        }
    }

    @Test public void whatWasHandedOverIsKeptApartFromWhatArrivedBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(17,18));
        for(String sql:new String[]{fresh,step}) {
            assertTrue("no handed table",sql.contains("CREATE TABLE IF NOT EXISTS handed("));
            for(String column:new String[]{"address TEXT NOT NULL","page TEXT NOT NULL","revision INTEGER NOT NULL",
                                           "at INTEGER NOT NULL","tries INTEGER NOT NULL","PRIMARY KEY(address,page)"})
                assertTrue(column,sql.contains(column));
        }
        // What was already recorded as delivered is left alone: most of it is true.
        assertFalse(step.toUpperCase(java.util.Locale.ROOT).contains("SENT"));
    }

    @Test public void whatWasAgreedIsKeptApartFromWhatWasDeliveredBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(18,19));
        for(String sql:new String[]{fresh,step})
            assertTrue(sql.contains("ALTER TABLE sent ADD COLUMN agreed INTEGER NOT NULL DEFAULT 0"));
    }

    @Test public void thisPhonesOwnStandingHasSomewhereToLiveBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(19,20));
        for(String sql:new String[]{fresh,step}) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS standing("));
            for(String column:new String[]{"scope TEXT NOT NULL","target TEXT NOT NULL","level INTEGER NOT NULL",
                                           "changed INTEGER NOT NULL","PRIMARY KEY(scope,target)"})
                assertTrue(column,sql.contains(column));
        }
    }

    @Test public void leavingIsToldApartFromStoppingForNowBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(20,21));
        for(String sql:new String[]{fresh,step})
            assertTrue(sql.contains("ALTER TABLE refused ADD COLUMN gone INTEGER NOT NULL DEFAULT 0"));
        // Everything refused before this was stopped for now, and still is.
        assertFalse(step.contains("UPDATE"));
    }

    @Test public void whatWasGivenAtPairingTimeIsWhatTheOfferSaid() {
        String step=String.join("\n",SchemaMigrations.upgrade(21,22));
        // Only the rows that say one thing in `mine` and another in `level`: a reader somebody decided on
        // says read in both, and is left as a reader.
        assertTrue(step.contains("UPDATE shares SET level=2 WHERE level=1 AND mine=1"));
        assertTrue(step.contains("UPDATE shares SET changed=added WHERE changed=0"));
        assertFalse("nothing is taken off anybody by a repair",step.contains("level=0"));
        // And a fresh notebook, which has no such rows, is not harmed by the same words.
        assertTrue(String.join("\n",SchemaMigrations.create()).contains("UPDATE shares SET level=2 WHERE level=1 AND mine=1"));
    }

    @Test public void filesThatTravelHaveSomewhereToLiveBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(23,24));
        for(String sql:new String[]{fresh,step}) {
            // What every file already kept says: it was added here, so no list from anybody can take it out.
            assertTrue(sql.contains("ALTER TABLE files ADD COLUMN origin TEXT NOT NULL DEFAULT ''"));
            for(String table:new String[]{"published(","incoming(","reached(","listed("})
                assertTrue(table,sql.contains("CREATE TABLE IF NOT EXISTS "+table));
            assertTrue(sql.contains("PRIMARY KEY(id,address)"));assertTrue(sql.contains("PRIMARY KEY(note,origin)"));
        }
        // Additive: nothing already in the notebook is touched but by a column added to it.
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }

    @Test public void whatThePcHoldsForItsPhonesHasSomewhereToLiveBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(24,25));
        for(String sql:new String[]{fresh,step}) {
            assertTrue("no held table",sql.contains("CREATE TABLE IF NOT EXISTS held("));
            for(String column:new String[]{"id TEXT PRIMARY KEY","sender TEXT NOT NULL","recipient TEXT NOT NULL","page TEXT NOT NULL",
                                           "revision INTEGER NOT NULL","sealed INTEGER NOT NULL","bytes TEXT NOT NULL","size INTEGER NOT NULL",
                                           "kept INTEGER NOT NULL"})
                assertTrue(column,sql.contains(column));
            assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS held_recipient ON held(recipient,kept)"));
        }
        // Additive: a table of its own, and nothing already in the notebook touched.
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP ")||upper.contains("ALTER "));
    }

    @Test public void filesSentOnTheirOwnHaveSomewhereToLiveBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(25,26));
        for(String sql:new String[]{fresh,step}) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS transfers("));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS transferred("));
            for(String column:new String[]{"wire TEXT NOT NULL","way TEXT NOT NULL","address TEXT NOT NULL","state INTEGER NOT NULL",
                                           "revision INTEGER NOT NULL DEFAULT 1","seen INTEGER NOT NULL DEFAULT 0","batch TEXT NOT NULL",
                                           "theirs TEXT NOT NULL DEFAULT ''","here INTEGER NOT NULL DEFAULT 0","relay INTEGER NOT NULL DEFAULT 0"})
                assertTrue(column,sql.contains(column));
            // One device's id for a sending can never be taken for another's, or for one going the other way.
            assertTrue(sql.contains("CREATE UNIQUE INDEX IF NOT EXISTS transfers_wire ON transfers(wire,address,way)"));
            assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS transferred_batch ON transferred(batch)"));
        }
        // Additive: tables of their own, and nothing already in the notebook touched.
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP ")||upper.contains("ALTER "));
    }

    @Test public void onePersonOnEveryDeviceHasSomewhereToLiveBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(26,27));
        for(String sql:new String[]{fresh,step}) {
            for(String column:new String[]{"person TEXT NOT NULL DEFAULT ''","via TEXT NOT NULL DEFAULT ''",
                                           "gone INTEGER NOT NULL DEFAULT 0","named INTEGER NOT NULL DEFAULT 0"})
                assertTrue(column,sql.contains("ALTER TABLE addresses ADD COLUMN "+column));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS persons(id TEXT PRIMARY KEY"));
            for(String column:new String[]{"name TEXT","changed INTEGER","aliases TEXT","mine INTEGER"})assertTrue(column,sql.contains(column));
            assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS addresses_person ON addresses(person)"));
        }
        // Additive: columns and a table of their own, and nothing already in the notebook rewritten.
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }

    @Test public void aNoteHasASizeOfItsOwnToKeepBothWaysRound() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(27,28));
        // Every note there already has none of its own, so it is read at the size it always was.
        for(String sql:new String[]{fresh,step})
            assertTrue(sql.contains("ALTER TABLE notes ADD COLUMN rung INTEGER NOT NULL DEFAULT "+Reading.NONE));
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }

    @Test public void whoWroteWhatIsKeptBesideTheNotesAndNothingThatWasThereChanges() {
        String fresh=String.join("\n",SchemaMigrations.create());
        String step=String.join("\n",SchemaMigrations.upgrade(28,29));
        for(String sql:new String[]{fresh,step}) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS writers(note TEXT PRIMARY KEY"));
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS inks(writer TEXT PRIMARY KEY"));
        }
        // New tables only: no note row is touched, so every note already here is drawn exactly as it was.
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP ")||upper.contains("ALTER TABLE NOTES"));
    }

    @Test public void collectionsAndBooksBecomeThingsAndNothingIsTakenAway() {
        assertTrue(SchemaMigrations.VERSION>=30);
        assertEquals(29,SchemaMigrations.BEFORE_THINGS);
        String fresh=String.join(" ",SchemaMigrations.create());
        String step=String.join(" ",SchemaMigrations.upgrade(29,30));
        for(String sql:new String[]{fresh,step}) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS things(id TEXT PRIMARY KEY,parent TEXT NOT NULL"));
            for(String column:new String[]{"icon TEXT","image TEXT","tint INTEGER","ordinal INTEGER","favourite INTEGER",
                    "dock INTEGER","archived INTEGER","binned INTEGER","theirs INTEGER","origin TEXT","pause INTEGER",
                    "revision INTEGER","made INTEGER","updated INTEGER","gone INTEGER"})
                assertTrue(column,sql.contains(column));
            // Collections under Home, books under the collection they were in, each under its own id.
            assertTrue(sql.contains("SELECT id,'"+Things.HOME+"','collection',name,colour,place,pinned,archived,deleted,theirs,origin,pause,updated,updated FROM collections"));
            assertTrue(sql.contains("SELECT id,collection,'collection',name,colour,place,pinned,archived,deleted,theirs,origin,pause,updated,updated FROM books"));
            for(String column:new String[]{"icon TEXT NOT NULL DEFAULT ''","image TEXT NOT NULL DEFAULT ''","dock INTEGER NOT NULL DEFAULT 0"})
                assertTrue(column,sql.contains("ALTER TABLE notes ADD COLUMN "+column));
            assertTrue(sql.contains("UPDATE files SET held='collection' WHERE held='book'"));
        }
        // A fresh notebook makes its first collection and book before they become things.
        List<String> made=SchemaMigrations.create();
        int book=-1,things=-1;
        for(int at=0;at<made.size();at++){if(made.get(at).startsWith("INSERT OR IGNORE INTO books"))book=at;if(made.get(at).contains("INTO things("))things=Math.max(things,at);}
        assertTrue(book>=0&&things>book);
        // Nothing is dropped, deleted or moved out of the old tables.
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("DELETE ")||upper.contains("DROP ")||upper.contains("UPDATE COLLECTIONS")||upper.contains("UPDATE BOOKS"));
        assertFalse("no note is moved",upper.contains("SET BOOK"));
    }

    @Test public void theCountsAroundTheStepAreTheSameThingsInTheSameOrder() {
        assertEquals(SchemaMigrations.COUNTED.length,SchemaMigrations.COUNT_BEFORE.length);
        assertEquals(SchemaMigrations.COUNTED.length,SchemaMigrations.COUNT_AFTER.length);
        long[] same={3,9,15,11,1,0,2};
        assertNull(SchemaMigrations.differs(same,same.clone()));
        long[] lost=same.clone();lost[2]=14;
        assertEquals("files 15 became 14",SchemaMigrations.differs(same,lost));
        long[] two=same.clone();two[0]=2;two[4]=0;
        assertEquals("collections and books 3 became 2, favourites 1 became 0",SchemaMigrations.differs(same,two));
        assertNotNull(SchemaMigrations.differs(same,new long[]{1}));
        assertNotNull(SchemaMigrations.differs(null,same));
        assertEquals("collections and books 3, notes 9, files 15, shares 11, favourites 1, binned 0, archived 2",SchemaMigrations.counted(same));
    }

    @Test public void receivedFilesAreKeptOnHomeAndTheirSendingsAreMarkedRatherThanDeleted() {
        assertTrue(SchemaMigrations.VERSION>=31);
        assertEquals(30,SchemaMigrations.BEFORE_HOME_FILES);
        String fresh=String.join(" ",SchemaMigrations.create());
        String step=String.join(" ",SchemaMigrations.upgrade(30,31));
        for(String sql:new String[]{fresh,step}) {
            assertTrue(sql.contains("ALTER TABLE files ADD COLUMN fresh INTEGER NOT NULL DEFAULT 0"));
            assertTrue(sql.contains("ALTER TABLE transferred ADD COLUMN moved INTEGER NOT NULL DEFAULT 0"));
            // Each received file that is here, on Home under its own id - the bytes already in the shed under it - with where
            // it came from, when, and whether its sending was looked at.
            assertTrue(sql.contains("INSERT OR IGNORE INTO files(id,note,name,kind,bytes,added,place,held,origin,fresh) SELECT f.id,'"+Things.HOME
                +"',f.name,f.kind,f.bytes,t.at,-t.at,'collection',t.address,CASE WHEN t.seen=0 THEN 1 ELSE 0 END"));
            assertTrue(sql.contains("WHERE t.way='in' AND f.here=1 AND f.moved=0"));
            assertTrue(sql.contains("UPDATE transferred SET moved=1 WHERE here=1 AND moved=0"));
        }
        // After the move to things, so a fresh notebook takes it as an upgraded one does.
        List<String> made=SchemaMigrations.create();
        int things=-1,home=-1;
        for(int at=0;at<made.size();at++){if(made.get(at).contains("INTO things("))things=Math.max(things,at);if(made.get(at).contains("ADD COLUMN moved"))home=at;}
        assertTrue(things>=0&&home>things);
        // Nothing is deleted or dropped: a sending stays behind its files, marked.
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("DELETE ")||upper.contains("DROP "));
        assertFalse("no note is touched",upper.contains("NOTES"));
    }

    @Test public void theCountsAroundEachCheckedStepAreTheSameThingsInTheSameOrder() {
        assertEquals(2,SchemaMigrations.CHECKED.size());
        assertEquals(SchemaMigrations.BEFORE_THINGS,SchemaMigrations.CHECKED.get(0).from);
        assertEquals(SchemaMigrations.BEFORE_HOME_FILES,SchemaMigrations.CHECKED.get(1).from);
        for(SchemaMigrations.Checked step:SchemaMigrations.CHECKED) {
            assertEquals(step.counted.length,step.before.length);
            assertEquals(step.counted.length,step.after.length);
            assertTrue(step.from<SchemaMigrations.VERSION);
        }
        long[] same={4,19,3};
        assertNull(SchemaMigrations.differs(SchemaMigrations.COUNTED_HOME,same,same.clone()));
        long[] ignored=same.clone();ignored[0]=3;ignored[1]=18;
        assertEquals("received files 4 became 3, files 19 became 18",SchemaMigrations.differs(SchemaMigrations.COUNTED_HOME,same,ignored));
        assertEquals("received files 4, files 19, new 3",SchemaMigrations.counted(SchemaMigrations.COUNTED_HOME,same));
        // Refused, the notebook is left where it was, and that is said by which build opens it.
        assertEquals("Mininotes 0.1.041",SchemaMigrations.stillOpens(29));
        assertEquals("Mininotes 0.1.041",SchemaMigrations.stillOpens(12));
        assertEquals("Mininotes 0.2.001",SchemaMigrations.stillOpens(30));
    }

    @Test public void upgradingNeverDiscardsStoredNotes() {
        for(String statement:SchemaMigrations.upgrade(1,SchemaMigrations.VERSION)) {
            String sql=statement.toUpperCase(java.util.Locale.ROOT);
            assertFalse(statement,sql.contains("DROP TABLE")||sql.contains("DELETE FROM")||sql.contains("DROP COLUMN"));
        }
    }
    @Test public void unknownVersionsAreRejected() {
        for(int[] range:new int[][]{{0,1},{-1,2},{1,SchemaMigrations.VERSION+1},{SchemaMigrations.VERSION+1,SchemaMigrations.VERSION+1}})
            try{SchemaMigrations.upgrade(range[0],range[1]);fail("Accepted "+range[0]+" to "+range[1]);}catch(IllegalArgumentException expected){}
    }
    @Test public void everyIconStandsOnAPageAndNoneHasOneYet() {
        assertTrue(SchemaMigrations.VERSION>=33);
        String fresh=String.join(" ",SchemaMigrations.create());
        String step=String.join(" ",SchemaMigrations.upgrade(32,33));
        for(String sql:new String[]{fresh,step})
            for(String table:new String[]{"things","notes","files"})
                assertTrue(table,sql.contains("ALTER TABLE "+table+" ADD COLUMN page INTEGER NOT NULL DEFAULT "+Layout.NO_PAGE));
        // Columns only: a cell kept before pages is read as the long grid it was (Layout.pages).
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void writingLinesAreOnUntilSwitchedOffAndWhatIsOnItsWayHasANameToWaitUnder() {
        assertTrue(SchemaMigrations.VERSION>=34);
        String fresh=String.join(" ",SchemaMigrations.create());
        String step=String.join(" ",SchemaMigrations.upgrade(33,34));
        for(String sql:new String[]{fresh,step}) {
            assertTrue(sql.contains("ALTER TABLE notes ADD COLUMN lines INTEGER NOT NULL DEFAULT 1"));
            assertTrue(sql.contains("ALTER TABLE accepting ADD COLUMN what TEXT NOT NULL DEFAULT ''"));
            assertTrue(sql.contains("ALTER TABLE accepting ADD COLUMN kind TEXT NOT NULL DEFAULT ''"));
        }
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void whereAThingIsShownAndALookOfItsOwnAreThisDevicesAndEmptyUntilChosen() {
        assertTrue(SchemaMigrations.VERSION>=35);
        String fresh=String.join(" ",SchemaMigrations.create());
        String step=String.join(" ",SchemaMigrations.upgrade(34,35));
        for(String sql:new String[]{fresh,step})
            for(String table:new String[]{"notes","things"})
                for(String column:new String[]{"shown","myicon","myimage"})
                    assertTrue(table+"."+column,sql.contains("ALTER TABLE "+table+" ADD COLUMN "+column+" TEXT NOT NULL DEFAULT ''"));
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void theColourEachPersonChoseIsATableOfItsOwn() {
        assertTrue(SchemaMigrations.VERSION>=36);
        String fresh=String.join(" ",SchemaMigrations.create()),step=String.join(" ",SchemaMigrations.upgrade(35,36));
        for(String sql:new String[]{fresh,step})
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS said_inks(writer TEXT PRIMARY KEY,colour INTEGER NOT NULL,at INTEGER NOT NULL)"));
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void whenAThingIsToBeGoneAndWhenItWasOpenedStartAtNothing() {
        assertTrue(SchemaMigrations.VERSION>=37);
        String fresh=String.join(" ",SchemaMigrations.create()),step=String.join(" ",SchemaMigrations.upgrade(36,37));
        for(String sql:new String[]{fresh,step})
            for(String table:new String[]{"notes","things"})
                for(String column:new String[]{"until","touched"})
                    assertTrue(table+"."+column,sql.contains("ALTER TABLE "+table+" ADD COLUMN "+column+" INTEGER NOT NULL DEFAULT 0"));
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void aFileIsInNoPlaceUntilItIsPutInOne() {
        // Decision 87: a file archived or binned, temporary, starred - none of them, for every file there is.
        assertTrue(SchemaMigrations.VERSION>=38);
        String fresh=String.join(" ",SchemaMigrations.create()),step=String.join(" ",SchemaMigrations.upgrade(37,38));
        for(String sql:new String[]{fresh,step})
            for(String column:new String[]{"away","until","starred"})
                assertTrue("files."+column,sql.contains("ALTER TABLE files ADD COLUMN "+column+" INTEGER NOT NULL DEFAULT 0"));
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void aFileIsNobodysAndShownWhereItIsUntilItIsSharedOnItsOwn() {
        // Decision 92: a file shared like a note (theirs, its revision, gone for everybody, where it is shown, and one waited
        // for on its own): nothing of it for every file and every wait there is.
        assertTrue(SchemaMigrations.VERSION>=39);
        String fresh=String.join(" ",SchemaMigrations.create()),step=String.join(" ",SchemaMigrations.upgrade(38,39));
        for(String sql:new String[]{fresh,step}) {
            for(String column:new String[]{"theirs","revision","gone"})
                assertTrue("files."+column,sql.contains("ALTER TABLE files ADD COLUMN "+column+" INTEGER NOT NULL DEFAULT 0"));
            assertTrue("files.shown",sql.contains("ALTER TABLE files ADD COLUMN shown TEXT NOT NULL DEFAULT ''"));
            assertTrue("incoming.alone",sql.contains("ALTER TABLE incoming ADD COLUMN alone INTEGER NOT NULL DEFAULT 0"));
        }
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void noFileIsOnItsWayToTempYet() {
        // Decision 95: a file on its way from Temp on another of my devices carries when it is to be gone; none is yet.
        assertTrue(SchemaMigrations.VERSION>=41);
        String fresh=String.join(" ",SchemaMigrations.create()),step=String.join(" ",SchemaMigrations.upgrade(40,41));
        for(String sql:new String[]{fresh,step})
            assertTrue("incoming.until",sql.contains("ALTER TABLE incoming ADD COLUMN until INTEGER NOT NULL DEFAULT 0"));
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void nobodyHasAParlonsAddressYet() {
        // Decision 101: a person's Parlons! address and when it was decided, on their card: empty and never decided for
        // everybody already here, so the first address chosen on any device of the owner's stands.
        assertTrue(SchemaMigrations.VERSION>=43);
        String fresh=String.join(" ",SchemaMigrations.create()),step=String.join(" ",SchemaMigrations.upgrade(42,43));
        for(String sql:new String[]{fresh,step}) {
            assertTrue("addresses.parlons",sql.contains("ALTER TABLE addresses ADD COLUMN parlons TEXT NOT NULL DEFAULT ''"));
            assertTrue("addresses.parlonsDecided",sql.contains("ALTER TABLE addresses ADD COLUMN parlonsDecided INTEGER NOT NULL DEFAULT 0"));
        }
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void nobodyIsInAGroupYetAndEveryRuleWasGivenDirectly() {
        // Decision 100: groups, who is in each, what each was given, and the group a rule came from: none of it for any
        // rule there is, so every rule already here stays one given to its person on their own.
        assertTrue(SchemaMigrations.VERSION>=42);
        String fresh=String.join(" ",SchemaMigrations.create()),step=String.join(" ",SchemaMigrations.upgrade(41,42));
        for(String sql:new String[]{fresh,step}) {
            assertTrue("groups",sql.contains("CREATE TABLE IF NOT EXISTS groups(id TEXT PRIMARY KEY,name TEXT NOT NULL,decided INTEGER NOT NULL,gone INTEGER NOT NULL DEFAULT 0)"));
            assertTrue("members",sql.contains("CREATE TABLE IF NOT EXISTS members(grp TEXT NOT NULL,signing TEXT NOT NULL,decided INTEGER NOT NULL,gone INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(grp,signing))"));
            assertTrue("group_shares",sql.contains("CREATE TABLE IF NOT EXISTS group_shares(grp TEXT NOT NULL,scope TEXT NOT NULL,target TEXT NOT NULL,level TEXT NOT NULL,decided INTEGER NOT NULL,PRIMARY KEY(grp,scope,target))"));
            assertTrue("shares.grp",sql.contains("ALTER TABLE shares ADD COLUMN grp TEXT NOT NULL DEFAULT ''"));
        }
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void noFileHasBeenRenamedOrReplacedByAnybodyYet() {
        // Decision 93: when a file's name was chosen and its bytes replaced, what this device changed, and a wait's owner and
        // whether it is a new version: nothing of it for every file and every wait there is.
        assertTrue(SchemaMigrations.VERSION>=40);
        String fresh=String.join(" ",SchemaMigrations.create()),step=String.join(" ",SchemaMigrations.upgrade(39,40));
        for(String sql:new String[]{fresh,step}) {
            for(String column:new String[]{"named","replaced","changed"})
                assertTrue("files."+column,sql.contains("ALTER TABLE files ADD COLUMN "+column+" INTEGER NOT NULL DEFAULT 0"));
            assertTrue("incoming.owner",sql.contains("ALTER TABLE incoming ADD COLUMN owner TEXT NOT NULL DEFAULT ''"));
            assertTrue("incoming.replaces",sql.contains("ALTER TABLE incoming ADD COLUMN replaces INTEGER NOT NULL DEFAULT 0"));
        }
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }
    @Test public void everyIconHasACellToStayInAndNoneHasOneYet() {
        assertTrue(SchemaMigrations.VERSION>=32);
        String fresh=String.join(" ",SchemaMigrations.create());
        String step=String.join(" ",SchemaMigrations.upgrade(31,32));
        for(String sql:new String[]{fresh,step})
            for(String table:new String[]{"things","notes","files"})
                assertTrue(table,sql.contains("ALTER TABLE "+table+" ADD COLUMN cell INTEGER NOT NULL DEFAULT "+Layout.NONE));
        // Columns only: every grid is drawn as it was until something on it is moved.
        String upper=step.toUpperCase(java.util.Locale.ROOT);
        assertFalse(upper.contains("UPDATE ")||upper.contains("DELETE ")||upper.contains("DROP "));
    }

    @Test public void downgradeIsRefusedRatherThanRewritingTheNotebook() {
        try{SchemaMigrations.upgrade(SchemaMigrations.VERSION,1);fail("Accepted a downgrade");}
        catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("newer version"));}
    }
}
