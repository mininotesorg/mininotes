// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A file or a note stamped on Minima through Integritas (the owner, 2026-10-18: "how could we integrate Integritas directly
 * through the app so that people could easily stamp a file from the app, could they link their Integritas account in
 * there?"; decision 199). Each person links their own account with the API key from their profile at integritas.technology.
 * Only the SHA-256 of what is stamped leaves the device, never the file or the note: Integritas records that fingerprint on
 * Minima with the time, and anybody holding the same bytes can later have it found there, with a PDF report. Holds no
 * Android or Swing types: both apps stamp and verify the same way. Blocking: a worker calls it.
 *
 * <p>The API (docs.integritas.technology, v2): POST timestamp/post {hash}, POST verify/file {hash, filename, filesize} with
 * x-report-required for the PDF; headers x-api-key and x-request-id; every answer in one envelope, {status, message, data}.
 *
 * <p>What a stamp is, and how far it is (decision 218): timestamp/post has Integritas' node send a Minima transaction that
 * carries the fingerprint (its answer: {data:{command:"send", params:{address, state:{"99":hash}}, status:true}}); it gives
 * no block and no transaction id. The fingerprint is on Minima once that transaction is in a block, a few minutes at most,
 * and verify/file then answers with blockchainData: the block, its date, the txpow and the transaction id. So a stamp is
 * waiting until then, and {@link Watch} looks for it, more and more seldom, until it is found or the time is up.
 */
final class Integritas {
    private Integritas(){}

    /** Where the API answers: the host its own examples use first, then the one its pages name. */
    static final String[] BASES={"https://integritas.minima.global/core/v2","https://integritas.technology/core/v2"};
    /** Where requests go: {@link #BASES}, or a stand-in in a test. */
    static String[] bases=BASES;
    /** Where a person gets their key. */
    static final String SITE="https://integritas.technology/";
    private static final int WAIT=30_000;
    /** Where the person's key is kept, among the notebook's secrets. */
    static final String KEY="integritas";

    /** The fingerprint Integritas records: the SHA-256 of the bytes, in lower-case hex. */
    static String hash(InputStream in) throws IOException {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] part=new byte[65536];int n;
            while((n=in.read(part))!=-1)digest.update(part,0,n);
            return hex(digest.digest());
        } catch(java.security.NoSuchAlgorithmException never){throw new IllegalStateException("SHA-256 is missing",never);}
    }
    static String hash(byte[] bytes) throws IOException {return hash(new java.io.ByteArrayInputStream(bytes));}

    /**
     * A SHA-256 fingerprint found in what was pasted (the owner, 2026-10-25: "when I receive a file, that I can compare it to
     * the hash that I have"; decision 250): 64 hexadecimal digits, with or without 0x, in capitals or not, with spaces or
     * colons between pairs as some tools write them, and words around it allowed. Lowercase, or null where there is none.
     */
    static String fingerprintIn(String pasted) {
        if(pasted==null)return null;
        String joined=pasted.replaceAll("(?<=[0-9A-Fa-f])[ :](?=[0-9A-Fa-f])","");
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<![0-9A-Fa-f])(?:0[xX])?([0-9A-Fa-f]{64})(?![0-9A-Fa-f])").matcher(joined);
        return m.find()?m.group(1).toLowerCase(Locale.ROOT):null;
    }
    /** Whether a fingerprint pasted is this one: true, false, or null while nothing pasted is a fingerprint yet. */
    static Boolean sameFingerprint(String mine,String pasted) {
        String theirs=fingerprintIn(pasted);
        return theirs==null||mine==null?null:theirs.equalsIgnoreCase(mine);
    }
    /** A note's fingerprint: its words, as Copy the text gives them, in UTF-8. */
    static String hashOfText(String text) throws IOException {return hash((text==null?"":text).getBytes(StandardCharsets.UTF_8));}

    /**
     * What a note's stamp covers from 0.4.018 (the owner, 2026-10-21: "Why integritas/stamping doesnt include the title of the
     * note? I think it should"; decision 230): its title, an empty line, then its text, as the PC's Copy the text gives them,
     * so anybody can copy the note and make the same SHA-256 without Mininotes; a note with no title, its text alone.
     */
    static String noteWords(String title,String body) {
        String t=title==null?"":title,b=body==null?"":body;
        // As Copy the note: a title of spaces is none, and a title is taken exactly as written.
        return t.trim().isEmpty()?b:t+"\n\n"+b;
    }
    static String hashOfNote(String title,String body) throws IOException {return hashOfText(noteWords(title,body));}
    /** Kinds of stamp: a note's title and text (from 0.4.018); "note", its text alone (stamps made before); a file in
     *  Mininotes; a file on this device's drive, never put in Mininotes (decision 230). */
    static final String TITLED="titled", NOTE="note", FILE="file", OUTSIDE="outside";
    /** A note as it is now, fingerprinted the way {@code row} was: each stamp keeps the form it was made in. */
    static String hashNow(NoteStore.StampRow row,String title,String body) throws IOException {
        return TITLED.equals(row.kind())?hashOfNote(title,body):hashOfText(body);
    }
    /** Of a note's stamps, newest first, the one that matches it as it is now, each by its own form; else null. */
    static NoteStore.StampRow matching(List<NoteStore.StampRow> all,String title,String body) {
        for(NoteStore.StampRow one:all)try{if(one.hash().equals(hashNow(one,title,body)))return one;}catch(IOException never){/* the next */}
        return null;
    }

    static String hex(byte[] bytes) {
        StringBuilder out=new StringBuilder();
        for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b));
        return out.toString();
    }

    /** Whether a key looks like one: not empty, no spaces, of a sensible length. Integritas says whether it is. */
    static boolean looksLikeAKey(String key){return key!=null&&key.trim().length()>=8&&!key.trim().contains(" ");}

    /**
     * What a stamp request said: when, the request's id, Integritas' own words, the Minima address its transaction went to,
     * and where the fingerprint already is on Minima, where it was there before (empty for a new one, the usual case).
     */
    record Stamped(String hash,long at,String request,String message,String address,List<Block> already) {}

    /** A fingerprint recorded on Minima now, or the record already there. */
    static Stamped stamp(String key,String hash) throws IOException {
        JSONObject body=new JSONObject();
        try{body.put("hash",hash);}catch(org.json.JSONException never){throw new IOException(never);}
        String request=UUID.randomUUID().toString();
        long at=System.currentTimeMillis();
        JSONObject said=post("timestamp/post",key,request,body,false);
        JSONObject data=said.optJSONObject("data"),params=data==null?null:data.optJSONObject("params");
        return new Stamped(hash,at,request,said.optString("message",""),params==null?"":params.optString("address",""),found(said).blocks());
    }

    /** One record of a fingerprint on Minima: the block, its date, the transaction (txpow) and the transaction's id. */
    record Block(long number,String date,String txpow,String tx) {}
    /**
     * What a verification found: whether the bytes match, where on Minima, the PDF report's link while it lasts, and, where a
     * report was asked for, the NFT Integritas minted for it: its coin id and the transaction that made it (decision 224).
     */
    record Found(boolean match,String result,List<Block> blocks,String report,String nft,String nftTx) {}

    /** The fingerprint looked for on Minima; with {@code report}, a PDF report made, its link good for about an hour. */
    static Found verify(String key,String hash,String name,long size,boolean report) throws IOException {
        JSONObject body=new JSONObject();
        try{body.put("hash",hash);if(name!=null&&!name.isEmpty())body.put("filename",name);if(size>=0)body.put("filesize",size);}
        catch(org.json.JSONException never){throw new IOException(never);}
        JSONObject said=post("verify/file",key,UUID.randomUUID().toString(),body,report);
        return found(said);
    }

    /** What verify/file answered, read: {data:{data:{result, blockchainData:[...]}}, file:{downloadUrl}}. */
    static Found found(JSONObject said) {
        JSONObject outer=said.optJSONObject("data"),inner=outer==null?null:outer.optJSONObject("data");
        String result=inner==null?"":inner.optString("result","");
        List<Block> blocks=new ArrayList<>();
        JSONArray chain=inner==null?null:inner.optJSONArray("blockchainData");
        if(chain!=null)for(int i=0;i<chain.length();i++){JSONObject one=chain.optJSONObject(i);if(one!=null)
            blocks.add(new Block(one.optLong("blockNumber",0),one.optString("blockDate",""),one.optString("txpowId",""),one.optString("transactionId","")));}
        JSONObject file=said.optJSONObject("file");
        if(file==null&&outer!=null)file=outer.optJSONObject("file");
        String report=file==null?null:file.optString("downloadUrl",null);
        boolean match=!blocks.isEmpty()||result.toLowerCase(Locale.ROOT).contains("full match");
        return new Found(match&&!result.toLowerCase(Locale.ROOT).startsWith("no"),result,blocks,report,
            inner==null?"":inner.optString("nftCoinId",""),inner==null?"":inner.optString("nftTxnId",""));
    }

    // ---- the Minima Explorer ------------------------------------------------------------------------------------------

    /**
     * Where a stamp can be seen on Minima, as the PDF report links it (the owner, 2026-10-19: "in the details we should link as
     * much as possible to the Minima explorer, like we have in the PDF report"; decision 224). The forms are the explorer's own,
     * read from its pages on 2026-10-19: a transaction at transactions/{txpow id}, and a block or an address found by its
     * search, search?q={block number or address}, which is how the explorer itself links an address. A transaction's id and a
     * coin's id are not found by it, so those are copied, not linked.
     */
    static final String EXPLORER="https://explorer.minima.global/";
    /** A block's page, by its number; null for none. */
    static String blockLink(long block){return block>0?EXPLORER+"search?q="+block:null;}
    /** A transaction's page, by its txpow id; null for what is not one. */
    static String txpowLink(String txpow){return hexId(txpow)?EXPLORER+"transactions/"+txpow:null;}
    /** An address, as the explorer finds it; null for none. */
    static String addressLink(String address) {
        if(address==null||!address.matches("[0-9A-Za-z#]{8,}"))return null;
        try{return EXPLORER+"search?q="+java.net.URLEncoder.encode(address,"UTF-8");}catch(java.io.UnsupportedEncodingException never){return null;}
    }
    private static boolean hexId(String id){return id!=null&&id.matches("0x[0-9A-Fa-f]{16,}");}

    /** Whether the fingerprint is on Minima yet: verify/file with no report asked, which changes nothing and costs nothing. */
    static Found check(String key,String hash) throws IOException {return verify(key,hash,null,-1,false);}

    /**
     * The record that stands for a stamp made at {@code at}: the first on Minima from a little before then (clocks differ), or,
     * where the fingerprint was there already, the first of all, the oldest proof. Null where there is none.
     */
    static Block chosen(List<Block> blocks,long at) {
        Block first=null,since=null;long firstAt=Long.MAX_VALUE,sinceAt=Long.MAX_VALUE;
        for(Block b:blocks) {
            if(b.number()<=0)continue;
            long when=millis(b.date());long key=when<0?Long.MAX_VALUE-1:when;
            if(first==null||key<firstAt||key==firstAt&&b.number()<first.number()){first=b;firstAt=key;}
            if(when>=at-15*60_000L&&(since==null||when<sinceAt)){since=b;sinceAt=when;}
        }
        return since!=null?since:first;
    }
    /** Integritas' ISO time in milliseconds, or -1 where it cannot be read. */
    static long millis(String iso){try{return java.time.Instant.parse(iso).toEpochMilli();}catch(Exception unreadable){return -1;}}

    /**
     * How long to wait before looking for a stamp on Minima again, after {@code tries} looks: soon at first, since a block
     * comes every minute or so and Integritas says a few minutes at most, then more and more seldom (seconds).
     */
    private static final int[] WAITS={15,20,30,30,45,60,60,90,120,180};
    static long waitSeconds(int tries){return tries<WAITS.length?WAITS[tries]:300;}
    /** How many looks before it stops on its own: about two hours. Verify again looks once more, and starts again. */
    static final int MOST=30;

    /** A date and time as this device says them. */
    static String when(long at){return java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT).format(new java.util.Date(at));}
    /** Integritas' ISO time as this device says it; as it came where it cannot be read. */
    static String when(String iso){long at=millis(iso);return at<0?(iso==null?"":iso):when(at);}
    /** A day as this device says it. */
    static String day(long at){return java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new java.util.Date(at));}

    /**
     * The line at the top of a stamped note (decision 218): stamped and where, waiting for Minima, not found yet, or, once its
     * text is not the text stamped, changed since. {@code stamp} is the one that stands: the stamp of what the note says now,
     * else its newest.
     */
    static String noteLine(NoteStore.StampRow stamp,boolean changed) {
        if(changed)return Say.t("Changed since it was stamped on {0}",day(stamp.at()));
        long recorded=millis(stamp.recorded());
        if(stamp.confirmed())return Say.t("Stamped on Minima, {0}, block {1}",day(recorded>0?recorded:stamp.at()),String.valueOf(stamp.block()));
        if(stamp.waiting())return Say.t("Stamping, waiting for Minima");
        return Say.t("Stamped {0}, not found on Minima yet",day(stamp.at()));
    }
    /** Of a note's stamps, newest first, the one whose fingerprint is that of {@code hashNow}; else the newest; null for none. */
    static NoteStore.StampRow standing(List<NoteStore.StampRow> all,String hashNow) {
        for(NoteStore.StampRow one:all)if(one.hash().equals(hashNow))return one;
        return all.isEmpty()?null:all.get(0);
    }

    /** What is fingerprinted, said plainly, for the box: a note's text (in characters) or a file (in bytes). */
    static String what(boolean note,long size) {
        if(note)return Say.t("The note's title and text as they were saved: {0} characters, in UTF-8. That exact text is kept with the stamp, in your notebook.",String.valueOf(size));
        return Say.t("The file exactly as it is kept here: {0}.",Attachment.size(size));
    }

    /**
     * What Integritas answered a stamp with, said for the box (the owner, 2026-10-25: "it says hash anchored successfully,
     * first this should be translated and then lower it says that it is still progressing, this is a bit confusing";
     * decision 249). Its "Hash anchored successfully." means it took the fingerprint and sent it to Minima, not that Minima
     * has it yet: said so, in the language chosen, so the step after (waiting for Minima) follows from it. Anything else
     * it says is quoted.
     */
    static String answered(String said) {
        if(said==null||said.isBlank())return "";
        if(said.toLowerCase(Locale.ROOT).contains("anchored successfully"))return Say.t("Integritas took it and sent it on to Minima.");
        return Say.t("Integritas said: {0}",said);
    }

    /** Everything kept of a stamp, to copy: plain lines anybody can check against Minima. */
    static String details(NoteStore.StampRow s) {
        StringBuilder all=new StringBuilder();
        all.append(s.name().isEmpty()?"Untitled":s.name()).append(" (").append(s.note()?"note":"file").append(")\n");
        all.append("SHA-256: ").append(s.hash()).append('\n');
        all.append("Stamped: ").append(java.time.Instant.ofEpochMilli(s.at())).append('\n');
        if(!s.request().isEmpty())all.append("Integritas request: ").append(s.request()).append('\n');
        if(!s.address().isEmpty())all.append("Minima address: ").append(s.address()).append('\n');
        if(s.confirmed()) {
            all.append("Block: ").append(s.block()).append('\n');
            if(!s.recorded().isEmpty())all.append("Recorded on Minima: ").append(s.recorded()).append('\n');
            if(!s.txpow().isEmpty())all.append("Transaction (txpow): ").append(s.txpow()).append('\n');
            if(!s.tx().isEmpty())all.append("Transaction id: ").append(s.tx()).append('\n');
            if(txpowLink(s.txpow())!=null)all.append("Minima Explorer: ").append(txpowLink(s.txpow())).append('\n');
            if(!s.nft().isEmpty())all.append("NFT coin id: ").append(s.nft()).append('\n');
            if(txpowLink(s.nftTx())!=null)all.append("NFT on the Minima Explorer: ").append(txpowLink(s.nftTx())).append('\n');
        } else all.append("State: ").append(s.waiting()?"waiting for Minima":"not found on Minima yet").append('\n');
        return all.toString().trim();
    }

    /**
     * Stamps waiting for Minima, looked for in the background until found (decision 218): closing the box does not stop it,
     * and a start of the app goes on with those still waiting. One thread of its own; each look is one verify/file with no
     * report. Every change is told to those listening, on this thread: each app takes it to its own screen.
     */
    static final class Watch {
        interface Listener{void heard(NoteStore.StampRow row,boolean landed,String trouble);}
        private final NoteStore store;private final long unit;
        private final java.util.concurrent.ScheduledExecutorService timer;
        private final List<Listener> listeners=new java.util.concurrent.CopyOnWriteArrayList<>();
        private final java.util.Map<Long,java.util.concurrent.ScheduledFuture<?>> due=new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.Map<Long,Long> next=new java.util.concurrent.ConcurrentHashMap<>();
        private final java.util.Map<Long,String> trouble=new java.util.concurrent.ConcurrentHashMap<>();
        /** Looks in a row that got no answer at all, for each stamp. */
        private final java.util.Map<Long,Integer> unanswered=new java.util.concurrent.ConcurrentHashMap<>();
        /** Looked for because somebody asked (Verify again, Check now): what went wrong is said at once. */
        private final java.util.Set<Long> asked=java.util.concurrent.ConcurrentHashMap.newKeySet();
        /** How many of those before the screen is told Integritas cannot be reached. */
        static final int QUIET_LOOKS=3;

        /** @param unit milliseconds in a second of waiting: 1000, or fewer in a test */
        Watch(NoteStore store,long unit) {
            this.store=store;this.unit=unit;
            timer=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Minima stamps");t.setDaemon(true);return t;});
        }
        NoteStore store(){return store;}
        void listen(Listener l){listeners.add(l);}
        void unlisten(Listener l){listeners.remove(l);}
        /** Those still waiting, looked for again from where they were. */
        void resume(){if(!timer.isShutdown())timer.execute(()->{try{for(NoteStore.StampRow one:store.stampsWaiting())if(!due.containsKey(one.n()))watch(one.n(),one.tries());}catch(RuntimeException closed){/* the notebook is gone */}});}
        /** One stamp looked for after the wait its looks so far call for. */
        void watch(long n,int tries) {
            if(timer.isShutdown())return;
            long wait=waitSeconds(tries)*unit;next.put(n,System.currentTimeMillis()+wait);
            java.util.concurrent.ScheduledFuture<?> was=due.put(n,timer.schedule(()->look(n),wait,java.util.concurrent.TimeUnit.MILLISECONDS));
            if(was!=null)was.cancel(false);
        }
        /** Looked for now (Verify again, Check now): one that had stopped is looked for again from the start. */
        void now(long n) {
            if(timer.isShutdown())return;
            java.util.concurrent.ScheduledFuture<?> was=due.remove(n);if(was!=null)was.cancel(false);
            timer.execute(()->{try{NoteStore.StampRow row=store.stampRow(n);
                if(row!=null&&!row.confirmed()&&!row.waiting())store.stampChecked(n,0,row.checked(),NoteStore.STAMP_WAITING);}catch(RuntimeException closed){return;}
                asked.add(n);look(n);});
        }
        /**
         * The stamps of this fingerprint told as they are now, after Verify on Minima or a report found or kept something of
         * them: every box and list showing them draws them again (decision 224). On the caller's thread.
         */
        void told(String hash) {
            try{for(NoteStore.StampRow one:store.stamps())if(one.hash().equals(hash))tell(one,false);}catch(RuntimeException closed){/* the notebook is gone */}
        }
        /** When it is next looked for, or 0 where it is not. */
        long nextLook(long n){return due.containsKey(n)?next.getOrDefault(n,0L):0;}
        /** Why the last look did not get an answer, or null. */
        String trouble(long n){return trouble.get(n);}
        void stop(){timer.shutdownNow();}

        private void look(long n) {
            due.remove(n);
            boolean wanted=asked.remove(n);
            NoteStore.StampRow row;
            try{row=store.stampRow(n);}catch(RuntimeException closed){return;}
            if(row==null||row.confirmed())return;
            int tries=row.tries()+1;long now=System.currentTimeMillis();
            try {
                Found f=check(store.secret(KEY),row.hash());trouble.remove(n);unanswered.remove(n);
                Block b=f.match()?chosen(f.blocks(),row.at()):null;
                if(b!=null){store.stampLanded(n,b.number(),b.txpow(),b.tx(),b.date(),tries,now);tell(store.stampRow(n),true);return;}
            } catch(Unreachable away) {
                // A look nobody asked for says nothing the first times Integritas does not answer: a phone waking, a
                // network changing or the server's moment of trouble passes, and the next look is soon (decision 246).
                int times=unanswered.merge(n,1,Integer::sum);
                if(wanted||times>=QUIET_LOOKS)trouble.put(n,away.getMessage());
            } catch(IOException failed){trouble.put(n,failed.getMessage()==null?"Integritas could not be reached.":failed.getMessage());}
            catch(RuntimeException closed){return;}
            boolean more=tries<MOST;
            try{store.stampChecked(n,tries,now,more?NoteStore.STAMP_WAITING:NoteStore.STAMP_UNSEEN);}catch(RuntimeException closed){return;}
            if(more)watch(n,tries);
            tell(store.stampRow(n),false);
        }
        private void tell(NoteStore.StampRow row,boolean landed) {
            if(row==null)return;
            for(Listener l:listeners)try{l.heard(row,landed,trouble.get(row.n()));}catch(RuntimeException ignored){/* one screen's trouble is not the others' */}
        }
    }

    /** One request to the API, tried at each host until one answers; an answer saying error is an error. */
    private static JSONObject post(String path,String key,String request,JSONObject body,boolean report) throws IOException {
        if(!looksLikeAKey(key))throw new IOException("Link your Integritas account first: paste the API key from your profile at integritas.technology.");
        IOException last=null;
        long from=System.currentTimeMillis();
        // Both names are one server today (147.93.126.192, read 2026-10-25), so the second host is no help when that
        // server, or the phone's way out, has a moment's trouble. A round that failed quickly (a network not up yet, a
        // refusal, no name found) is tried once more after a pause; one that waited out the time is not made to wait again.
        for(int round=0;round<2;round++) {
            for(String base:bases) {
                try{return once(base+"/"+path,key.trim(),request,body,report);}
                catch(Unreachable gone){last=gone;}
            }
            if(round>0||System.currentTimeMillis()-from>QUICK)break;
            try{Thread.sleep(againAfter);}catch(InterruptedException stop){Thread.currentThread().interrupt();break;}
        }
        // Why, in the log: a code or the kind of failure, never the key or what was stamped.
        said.accept("no answer from "+bases.length+" host(s) for "+path+": "+(last==null?"none tried":why(last)));
        throw last==null?new Unreachable("Integritas could not be reached."):new Unreachable("Integritas could not be reached. Try again in a moment.");
    }
    /** Where a line for the log goes: each app's own log, set by NoteStore (this class holds no Android types). */
    static volatile java.util.function.Consumer<String> said=why->{};
    /** A round that failed in less than this was quick, and is tried again (see {@link #post}). */
    private static final long QUICK=10_000;
    /** The pause before that second round; a test makes it short. */
    static long againAfter=3_000;
    private static String why(IOException e){return e.getMessage()==null?e.getClass().getSimpleName():e.getClass().getSimpleName()+" "+e.getMessage();}

    /** A host that did not answer at all, or does not have the API: the next is tried. Thrown on when none answers. */
    static final class Unreachable extends IOException{Unreachable(String why){super(why);}}

    private static JSONObject once(String url,String key,String request,JSONObject body,boolean report) throws IOException {
        HttpURLConnection http;
        try{http=(HttpURLConnection)new URL(url).openConnection();}catch(IOException bad){throw new Unreachable(bad.getMessage());}
        try {
            http.setConnectTimeout(WAIT);http.setReadTimeout(WAIT);http.setRequestMethod("POST");http.setDoOutput(true);
            http.setRequestProperty("Content-Type","application/json");http.setRequestProperty("Accept","application/json");
            http.setRequestProperty("x-api-key",key);http.setRequestProperty("x-request-id",request);
            if(report)http.setRequestProperty("x-report-required","true");
            byte[] out=body.toString().getBytes(StandardCharsets.UTF_8);
            try(OutputStream to=http.getOutputStream()){to.write(out);}
            catch(IOException noRoute){throw new Unreachable(noRoute.getMessage());}
            int code;
            try{code=http.getResponseCode();}catch(IOException noAnswer){throw new Unreachable(noAnswer.getMessage());}
            if(code==404||code==502||code==503)throw new Unreachable("HTTP "+code);
            String text=read(code>=400?http.getErrorStream():http.getInputStream());
            JSONObject said;
            try{said=new JSONObject(text.isEmpty()?"{}":text);}catch(org.json.JSONException notJson){said=new JSONObject();}
            if(code==401||code==403)throw new IOException("Integritas did not accept this API key. Check it in your profile at integritas.technology.");
            if(code==402)throw new IOException("Your Integritas account has no stamps left. Add some at integritas.technology.");
            if(code>=400||"error".equalsIgnoreCase(said.optString("status",""))) {
                String why=said.optString("message","");
                throw new IOException(why.isEmpty()?"Integritas said no (HTTP "+code+").":"Integritas said: "+why);
            }
            return said;
        } finally {http.disconnect();}
    }

    /**
     * The PDF report a verification made, fetched while its link lasts (about an hour) so it can be kept with what it is about
     * (the owner, 2026-10-25: "making sure I can atached the integritas pdf report"; decision 253). Only over https, or to this
     * machine for a test; at most {@link #REPORT_MOST} bytes, and only what starts as a PDF does.
     */
    static byte[] download(String link) throws IOException {
        URL url;
        try{url=new URL(link);}catch(java.net.MalformedURLException bad){throw new IOException("Integritas gave no report this time. Try again in a moment.");}
        boolean here="127.0.0.1".equals(url.getHost())||"localhost".equals(url.getHost());
        if(!"https".equals(url.getProtocol())&&!(here&&"http".equals(url.getProtocol())))throw new IOException("Integritas gave no report this time. Try again in a moment.");
        HttpURLConnection http=(HttpURLConnection)url.openConnection();
        try {
            http.setConnectTimeout(WAIT);http.setReadTimeout(WAIT);http.setInstanceFollowRedirects(true);
            if(http.getResponseCode()!=200)throw new IOException("The report could not be fetched. Try again in a moment.");
            try(InputStream in=http.getInputStream()) {
                ByteArrayOutputStream all=new ByteArrayOutputStream();byte[] part=new byte[8192];int n;
                while((n=in.read(part))!=-1){all.write(part,0,n);if(all.size()>REPORT_MOST)throw new IOException("The report could not be fetched. Try again in a moment.");}
                byte[] pdf=all.toByteArray();
                if(pdf.length<5||pdf[0]!='%'||pdf[1]!='P'||pdf[2]!='D'||pdf[3]!='F')throw new IOException("The report could not be fetched. Try again in a moment.");
                return pdf;
            }
        } catch(java.net.SocketTimeoutException slow){throw new IOException("The report could not be fetched. Try again in a moment.");}
        finally{http.disconnect();}
    }
    /** The most a report may weigh: far above any Integritas makes. */
    static final int REPORT_MOST=20*1024*1024;
    /** What a report kept with a thing is called: its name, then what it is. */
    static String reportName(String of){String base=of==null||of.isBlank()?Say.t("Untitled"):of.replaceAll("\\.[A-Za-z0-9]{1,8}$","");return base+", Integritas report.pdf";}

    private static String read(InputStream in) throws IOException {
        if(in==null)return "";
        try(InputStream from=in){ByteArrayOutputStream all=new ByteArrayOutputStream();byte[] part=new byte[8192];int n;
            while((n=from.read(part))!=-1)all.write(part,0,n);return all.toString(StandardCharsets.UTF_8.name());}
    }
}
