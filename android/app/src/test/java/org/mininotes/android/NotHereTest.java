package org.mininotes.android;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * "Not here" (decision 293; the owner agreed 2026-10-11 and 2026-10-30): a rule on one thing that, for its device, stands in
 * place of what a folder above or everything gives it, so somebody can be taken away below a folder or given less there.
 * Who reaches what, given rules, "not here" and bonds; when a choice is written as one; and how it travels in a parcel and a
 * carton, said only where it is, and read back whole or not at all.
 */
public class NotHereTest {
    private static final String TRIP="folder-trip", DAYS="folder-days", NOTE="note-1", OTHER="note-2";
    private static final String ANA="MxA..ana", BEN="MxB..ben", PHONE="MxC..phone", KEY="key-of-ana";
    /** A note in Days, in Trip, on Home: Home down, the note last, as a path is. */
    private static final List<String> AT_NOTE=List.of(TRIP,DAYS,NOTE), AT_OTHER=List.of(TRIP,DAYS,OTHER), AT_DAYS=List.of(TRIP,DAYS);

    private static Sharing.Rule rule(Sharing.Scope scope,String target,String address,Sharing.Level level) {
        return new Sharing.Rule(scope,target,address,level,10L,"");
    }
    private static Sharing.Rule notHere(Sharing.Scope scope,String target,String address,Sharing.Level level) {
        return new Sharing.Rule(scope,target,address,level,20L,"",true);
    }

    @Test public void takenAwayBelowAFolderReachesNobodyThereAndEverybodyElseAsBefore() {
        List<Sharing.Rule> rules=Arrays.asList(rule(Sharing.Scope.THING,TRIP,ANA,Sharing.Level.WRITE),rule(Sharing.Scope.THING,TRIP,BEN,Sharing.Level.READ),
            notHere(Sharing.Scope.PAGE,NOTE,ANA,Sharing.Level.GONE));
        Map<String,Boolean> there=Sharing.audience(rules,AT_NOTE);
        assertFalse("Ana is taken away from this note",there.containsKey(ANA));
        assertTrue("Ben still has it",there.containsKey(BEN));
        assertTrue("the next note in the same folder still goes to Ana",Sharing.audience(rules,AT_OTHER).containsKey(ANA));
        assertTrue("and the folder itself",Sharing.audience(rules,AT_DAYS).containsKey(ANA));
        Sharing.Rule stands=Sharing.standing(rules,AT_NOTE,List.of(ANA),"");
        assertEquals("what she sends there is weighed as taken off",Sharing.Level.GONE,stands.level);
        assertTrue(stands.here);
    }

    @Test public void withoutNotHereARuleBelowOnlyAddsAsItAlwaysDid() {
        // A row at GONE on the note, written by a build from before or by Remove where nothing above gave it: the folder wins.
        List<Sharing.Rule> rules=Arrays.asList(rule(Sharing.Scope.THING,TRIP,ANA,Sharing.Level.WRITE),rule(Sharing.Scope.PAGE,NOTE,ANA,Sharing.Level.GONE));
        assertTrue(Sharing.audience(rules,AT_NOTE).containsKey(ANA));
        assertEquals(Sharing.Level.WRITE,Sharing.standing(rules,AT_NOTE,List.of(ANA),"").level);
        assertEquals("and the rules stand as they are",rules,Sharing.standingOn(rules,AT_NOTE));
    }

    @Test public void lessHereIsWhatStandsHereAndOnlyHere() {
        List<Sharing.Rule> rules=Arrays.asList(rule(Sharing.Scope.THING,TRIP,ANA,Sharing.Level.ADMIN),notHere(Sharing.Scope.PAGE,NOTE,ANA,Sharing.Level.READ));
        assertEquals(Sharing.Level.READ,Sharing.standing(rules,AT_NOTE,List.of(ANA),"").level);
        assertFalse("a reader is sent it, and does not write it back",Sharing.audience(rules,AT_NOTE).get(ANA));
        assertEquals(Sharing.Level.ADMIN,Sharing.standing(rules,AT_OTHER,List.of(ANA),"").level);
    }

    @Test public void aFolderTakenAwayTakesEverythingInsideItAndARuleDeeperStillGives() {
        List<Sharing.Rule> rules=new ArrayList<>(Arrays.asList(rule(Sharing.Scope.THING,TRIP,ANA,Sharing.Level.WRITE),
            notHere(Sharing.Scope.THING,DAYS,ANA,Sharing.Level.GONE)));
        assertFalse(Sharing.audience(rules,AT_DAYS).containsKey(ANA));
        assertFalse("everything inside it",Sharing.audience(rules,AT_NOTE).containsKey(ANA));
        assertTrue("Trip itself still goes to her",Sharing.audience(rules,List.of(TRIP)).containsKey(ANA));
        // Given one note inside it on its own afterwards: below "not here", so it counts.
        rules.add(rule(Sharing.Scope.PAGE,NOTE,ANA,Sharing.Level.READ));
        assertTrue(Sharing.audience(rules,AT_NOTE).containsKey(ANA));
        assertFalse(Sharing.audience(rules,AT_OTHER).containsKey(ANA));
    }

    @Test public void aDeviceOfMineWithEverythingIsTakenAwayFromOneThingAndKeepsItsBond() {
        Sharing.Rule bond=rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,PHONE,Sharing.Level.WRITE);
        List<Sharing.Rule> rules=Arrays.asList(bond,notHere(Sharing.Scope.PAGE,NOTE,PHONE,Sharing.Level.GONE));
        assertFalse(Sharing.audience(rules,AT_NOTE).containsKey(PHONE));
        assertTrue("everything else still goes to it",Sharing.audience(rules,AT_OTHER).get(PHONE));
        assertTrue("nothing is above everything to set aside",new Sharing.Rule(Sharing.Scope.LIBRARY,Sharing.EVERYTHING,PHONE,Sharing.Level.READ,1L,"",true).here==false);
    }

    @Test public void theSameDeviceAtAnotherAddressIsKnownByItsKey() {
        // The folder's rule holds the address she was at; "not here" was decided at her new one.
        List<Sharing.Rule> rules=Arrays.asList(new Sharing.Rule(Sharing.Scope.THING,TRIP,"MxA..ana-then",Sharing.Level.WRITE,10L,KEY),
            new Sharing.Rule(Sharing.Scope.PAGE,NOTE,ANA,Sharing.Level.GONE,20L,KEY,true));
        assertFalse(Sharing.audience(rules,AT_NOTE).containsKey("MxA..ana-then"));
        assertEquals(Sharing.Level.GONE,Sharing.standing(rules,AT_NOTE,List.of("MxA..ana-then"),KEY).level);
    }

    @Test public void whatHoldsAThingIsAskedWithoutTheThingItself() {
        List<Sharing.Rule> rules=Arrays.asList(rule(Sharing.Scope.THING,TRIP,ANA,Sharing.Level.WRITE),notHere(Sharing.Scope.PAGE,NOTE,ANA,Sharing.Level.GONE),
            rule(Sharing.Scope.PAGE,NOTE,BEN,Sharing.Level.READ));
        assertEquals(Sharing.Level.WRITE,Sharing.above(rules,AT_NOTE,List.of(ANA),"").level);
        assertNull("Ben has it on the note alone",Sharing.above(rules,AT_NOTE,List.of(BEN),""));
        assertNull(Sharing.above(rules,List.of(),List.of(ANA),""));
    }

    @Test public void aChoiceIsNotHereOnlyWhereWhatHoldsItGivesMore() {
        assertTrue(Sharing.here(Sharing.Level.GONE,Sharing.Level.READ));
        assertTrue(Sharing.here(Sharing.Level.READ,Sharing.Level.WRITE));
        assertFalse("as much as the folder: a rule like any other, which lifts it",Sharing.here(Sharing.Level.WRITE,Sharing.Level.WRITE));
        assertFalse("more: a rule like any other",Sharing.here(Sharing.Level.ADMIN,Sharing.Level.WRITE));
        assertFalse("nothing above",Sharing.here(Sharing.Level.GONE,null));
    }

    @Test public void movingSomethingTakesItsNotHereWithIt() {
        // Taken away from the note in Days; moved to Home, where Trip gives her nothing: nobody is gained or lost by it.
        List<Sharing.Rule> rules=Arrays.asList(rule(Sharing.Scope.THING,TRIP,ANA,Sharing.Level.WRITE),notHere(Sharing.Scope.PAGE,NOTE,ANA,Sharing.Level.GONE));
        Sharing.Change change=Sharing.moving(rules,AT_NOTE,List.of(NOTE));
        assertFalse(change.any());
    }

    // ---- how it travels ---------------------------------------------------------------------------------------------------

    private static Parcel.Sent parcel(List<Parcel.Member> members) {
        Parcel.Sent sent=new Parcel.Sent("c","Trip","b","Days","Title","Body",true,members,"PAGE",NOTE,true,3L,true,
            new ArrayList<>(),5L,new ArrayList<>(),new ArrayList<>(),"",null);
        sent.colour=2;sent.colourDecided=7L;
        return sent;
    }
    private static List<Parcel.Member> members(boolean anaNotHere) {
        List<Parcel.Member> all=new ArrayList<>();
        all.add(new Parcel.Member("key-ana",ANA,"Ana",Sharing.Level.GONE.said(),20L,null,anaNotHere));
        all.add(new Parcel.Member("key-ben",BEN,"Ben",Sharing.Level.READ.said(),10L,null,false));
        return all;
    }

    @Test public void aParcelSaysWhichEntriesAreNotHereAndReadsThemBack() throws Exception {
        Parcel.Sent read=Parcel.open(Parcel.wrap(parcel(members(true))));
        assertNotNull(read);
        assertTrue(read.members.get(0).here);assertFalse(read.members.get(1).here);
        assertEquals("what follows is read as before",7L,read.colourDecided);
    }

    @Test public void aListWithNoneIsSaidByteForByteAsBefore() throws Exception {
        byte[] none=Parcel.wrap(parcel(members(false)));
        // The same list as a build from before made it: no flag on any entry.
        List<Parcel.Member> before=new ArrayList<>();
        for(Parcel.Member one:members(false))before.add(new Parcel.Member(one.key,one.address,one.name,one.level,one.changed,null));
        assertArrayEquals(Parcel.wrap(parcel(before)),none);
        assertFalse(new String(none,java.nio.charset.StandardCharsets.ISO_8859_1).contains("MNX1"));
    }

    @Test public void notHereThatDoesNotReadWholeMarksNobody() throws Exception {
        byte[] whole=Parcel.wrap(parcel(members(true)));
        String text=new String(whole,java.nio.charset.StandardCharsets.ISO_8859_1);
        int at=text.indexOf("MNX1");
        assertTrue(at>0);
        // The count changed to one that is not the list's: the parcel still arrives, and nobody is marked.
        byte[] wrong=whole.clone();wrong[at+7]=9;
        Parcel.Sent read=Parcel.open(wrong);
        assertNotNull(read);
        for(Parcel.Member one:read.members)assertFalse(one.here);
    }

    @Test public void aCartonSaysItToo() throws Exception {
        Carton.Sent going=new Carton.Sent(DAYS,"Days","",null,0,0L,new ArrayList<>(),true,"THING",DAYS,members(true),true,null,0L);
        Carton.Sent read=Carton.open(Carton.wrap(going));
        assertNotNull(read);
        assertTrue(read.members.get(0).here);assertFalse(read.members.get(1).here);
        Carton.Sent none=new Carton.Sent(DAYS,"Days","",null,0,0L,new ArrayList<>(),true,"THING",DAYS,members(false),true,null,0L);
        assertFalse(new String(Carton.wrap(none),java.nio.charset.StandardCharsets.ISO_8859_1).contains("MNX1"));
    }

    @Test public void theWordsSayWhatItGets() {
        assertEquals("One of your devices. It gets only Temp and what you share with it. Choose here what it may do with everything else.",Sharing.NOTHING_YET_DOES);
        assertTrue(Sharing.everythingDoes(Sharing.Level.ADMIN).contains("share it"));
        assertTrue(Sharing.REMOVE_EVERYTHING_SAYS.contains("{0}"));
    }
}
