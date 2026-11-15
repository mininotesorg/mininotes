package org.mininotes.desktop.platform.content;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import org.json.*;
import org.mininotes.desktop.platform.AtomicFile;

/**
 * The backup server's settings (decision 330): what the PC seals with Windows' DPAPI is kept here as a file only the server's own
 * user can read, in a folder only it can enter (install.sh makes both so). There is no person at a server to type a password at
 * each start; its keys only pair it and open nothing of the owner's: the backups it keeps are sealed by their devices.
 */
public final class SharedPreferences {
    private final Path file;
    private JSONObject values;
    SharedPreferences(Path file) {
        this.file=file;
        try {
            values=Files.exists(file)?new JSONObject(Files.readString(file,StandardCharsets.UTF_8)):new JSONObject();
        } catch(Exception failure){throw new IllegalStateException("Could not open this server's settings",failure);}
    }
    public synchronized String getString(String key,String fallback){return values.optString(key,fallback);}
    public synchronized long getLong(String key,long fallback){return values.optLong(key,fallback);}
    public synchronized int getInt(String key,int fallback){return values.optInt(key,fallback);}
    public synchronized boolean getBoolean(String key,boolean fallback){return values.optBoolean(key,fallback);}
    public synchronized Set<String> getStringSet(String key,Set<String> fallback) {
        JSONArray array=values.optJSONArray(key);
        if(array==null)return new HashSet<>(fallback);
        Set<String> result=new HashSet<>();
        for(int i=0;i<array.length();i++)result.add(array.getString(i));
        return result;
    }
    public Editor edit(){return new Editor();}
    private static final Object REMOVED=new Object();
    public final class Editor {
        private final Map<String,Object> changes=new LinkedHashMap<>();
        public Editor putString(String key,String value){changes.put(key,value);return this;}
        public Editor putLong(String key,long value){changes.put(key,value);return this;}
        public Editor putInt(String key,int value){changes.put(key,value);return this;}
        public Editor putBoolean(String key,boolean value){changes.put(key,value);return this;}
        public Editor putStringSet(String key,Set<String> value){changes.put(key,new JSONArray(value));return this;}
        public Editor remove(String key){changes.put(key,REMOVED);return this;}
        public void apply() {
            synchronized(SharedPreferences.this) {
                JSONObject next=new JSONObject(values.toString());
                changes.forEach((key,value)->{if(value==REMOVED)next.remove(key);else next.put(key,value);});
                try {
                    AtomicFile.write(file,next.toString().getBytes(StandardCharsets.UTF_8));
                    ownOnly(file);
                    values=next;
                } catch(Exception failure){throw new IllegalStateException("Could not save this server's settings",failure);}
            }
        }
        public boolean commit(){apply();return true;}
    }
    /** Readable by this user alone, where the file system says who reads (not on Windows, where the tests run). */
    public static void ownOnly(Path file) {
        try{Files.setPosixFilePermissions(file,PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException|java.io.IOException notHere){/* not POSIX */}
    }
}
