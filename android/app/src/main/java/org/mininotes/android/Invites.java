// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Connecting is an invitation, accepted or ignored, as a shared thing is (decision 163; docs/CONNECT-REVIEW.md, the owner's
 * answer 1). Somebody who scanned this device's plain code, offering nothing, is no longer added back by itself: "Ben wants
 * to add you" waits here, with Accept and Ignore, until the owner says. Accept adds them and answers their hello; Ignore
 * tells nobody, and their hello, which they say again at each opening, is let fall from then on. Nothing of it is in the
 * notebook: the hello is kept in the settings, as what waits to be answered on a locked device is.
 *
 * <p>A code that offers a thing is not this: showing it was the decision, and it stays one step (decision 109's 15 minutes).
 * Nor is a device already added, whose hello is answered as before.
 */
final class Invites {
    private Invites(){}

    private static SharedPreferences kept(Context c){return c.getSharedPreferences("invites",0);}
    private static String b64(byte[] b){return Base64.getEncoder().encodeToString(b==null?new byte[0]:b);}
    private static byte[] bytes(String s){try{return Base64.getDecoder().decode(s);}catch(IllegalArgumentException broken){return new byte[0];}}
    private static String key(byte[] signing){return "ignored:"+b64(signing);}

    /** Whether this device already has them, by the key they sign with: their hello is answered as before, not asked about. */
    static boolean known(NoteStore store,Hello.Said said) {
        for(NoteStore.Contact one:store.addresses())if(one.paired()&&java.util.Arrays.equals(one.signing,said.signing))return true;
        return false;
    }

    /** Whether the owner ignored them: their hello falls, said to nobody. */
    static boolean ignored(Context c,Hello.Said said){return kept(c).getString(key(said.signing),"").equals("1");}

    /** Kept to be asked about, once each, by their address. Whether it is new here (and so to be said). */
    static boolean keep(Context c,Hello.Said said) {
        String line=said.name.replace('\t',' ')+"\t"+b64(said.agreement)+"\t"+b64(said.signing)+"\t"+(said.files?"1":"0")+"\t"+System.currentTimeMillis();
        boolean fresh=kept(c).getString("from:"+said.address,"").isEmpty();
        kept(c).edit().putString("from:"+said.address,line).apply();
        return fresh;
    }

    /** What waits to be accepted or ignored, the oldest first. */
    static List<Hello.Said> waiting(Context c) {
        List<Object[]> all=new ArrayList<>();
        SharedPreferences p=kept(c);
        for(String address:p.getString("addresses","").split("\n")) {
            if(address.isEmpty())continue;
            String[] f=p.getString("from:"+address,"").split("\t",-1);
            if(f.length<5)continue;
            long at;try{at=Long.parseLong(f[4]);}catch(NumberFormatException broken){continue;}
            all.add(new Object[]{at,new Hello.Said(f[0],address,bytes(f[1]),bytes(f[2]),"","",false,"1".equals(f[3]))});
        }
        all.sort((a,b)->Long.compare((Long)a[0],(Long)b[0]));
        List<Hello.Said> out=new ArrayList<>();for(Object[] one:all)out.add((Hello.Said)one[1]);
        return out;
    }

    /** Kept and listed: the settings on both apps keep strings, so the addresses that wait are one string, a line each. */
    static boolean add(Context c,Hello.Said said) {
        if(ignored(c,said))return false;
        boolean fresh=keep(c,said);
        String all=kept(c).getString("addresses","");
        if(!("\n"+all+"\n").contains("\n"+said.address+"\n"))kept(c).edit().putString("addresses",all.isEmpty()?said.address:all+"\n"+said.address).apply();
        return fresh;
    }

    /** No longer waiting: accepted, or ignored, which is remembered by their key so their hello falls from now on. */
    static void done(Context c,Hello.Said said,boolean ignore) {
        StringBuilder rest=new StringBuilder();
        for(String address:kept(c).getString("addresses","").split("\n"))
            if(!address.isEmpty()&&!address.equals(said.address))rest.append(rest.length()==0?"":"\n").append(address);
        SharedPreferences.Editor e=kept(c).edit().putString("addresses",rest.toString()).putString("from:"+said.address,"");
        if(ignore)e.putString(key(said.signing),"1");
        e.apply();
    }

    /** Asked to be added after all, by scanning their code here: ignoring them is forgotten. */
    static void unignore(Context c,byte[] signing){kept(c).edit().putString(key(signing),"").apply();}
}
