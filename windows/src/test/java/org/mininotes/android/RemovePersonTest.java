package org.mininotes.android;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.mininotes.desktop.platform.content.Context;
import static org.junit.Assert.*;

/** Remove this person (decision 163): off everything shared with them that this device may change, then out of my people. Offline, made up. */
public class RemovePersonTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    @Test public void offEverythingSharedWithThemAndOutOfMyPeople() throws Exception {
        NoteStore store=new NoteStore(new Context(temp.newFolder("pad")));store.getWritableDatabase();
        try {
            String ana="MxAnaRemove@127.0.0.1:9911",ben="MxBenRemove@127.0.0.1:9912";
            for(String[] one:new String[][]{{ana,"Ana"},{ben,"Ben"}}) {
                Keys k=new Keys(new Context(temp.newFolder(one[1])));
                store.pairedWith(one[0],one[1],false,k.agreement().getPublic().getEncoded(),k.signing().getPublic().getEncoded());
            }
            NoteStore.Note plan=new NoteStore.Note();plan.title="Plan";plan.body="made up";plan.book=Things.HOME;store.save(plan);
            NoteStore.Note list=new NoteStore.Note();list.title="List";list.body="made up";list.book=Things.HOME;store.save(list);
            store.give(Sharing.Scope.PAGE,plan.id,ana,Sharing.Level.WRITE,"Ana");
            store.give(Sharing.Scope.PAGE,list.id,ana,Sharing.Level.READ,"Ana");
            store.give(Sharing.Scope.PAGE,list.id,ben,Sharing.Level.READ,"Ben");
            assertEquals(2,store.offEverything(ana).size());
            for(Sharing.Rule rule:store.shares())if(rule.address.equals(ana))assertEquals(Sharing.Level.GONE,rule.level);
            boolean benStays=false;for(Sharing.Rule rule:store.shares())if(rule.address.equals(ben)&&rule.level==Sharing.Level.READ)benStays=true;
            assertTrue("nobody else is touched",benStays);
            assertEquals("twice is nothing more",0,store.offEverything(ana).size());
        } finally{store.close();}
    }
}
