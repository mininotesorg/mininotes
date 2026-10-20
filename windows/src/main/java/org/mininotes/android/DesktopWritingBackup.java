package org.mininotes.android;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Separate desktop recovery backup, always sealed; no private-space or portable-note format change. */
final class DesktopWritingBackup {
    static void write(NoteStore store,Path target,byte[] key,byte[] lock) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream head=new DataOutputStream(bytes);
        head.write(DesktopBackup.LOCKED);head.writeShort(lock.length);head.write(lock);
        try(InputStream in=new ByteArrayInputStream(DesktopRecovery.backup(store).getBytes(StandardCharsets.UTF_8))){Sealed.seal(key,in,bytes);}
        org.mininotes.desktop.platform.AtomicFile.write(target,bytes.toByteArray());
    }
    static byte[] lock(Path source) throws Exception {
        try(DataInputStream in=new DataInputStream(Files.newInputStream(source))) {
            byte[] magic=new byte[4];in.readFully(magic);if(!java.util.Arrays.equals(magic,DesktopBackup.LOCKED))throw new IOException("This is not a locked writing backup");
            byte[] lock=new byte[in.readUnsignedShort()];in.readFully(lock);return lock;
        }
    }
    static int restore(NoteStore store,Path source,byte[] key) throws Exception {
        ByteArrayOutputStream plain=new ByteArrayOutputStream();
        try(DataInputStream in=new DataInputStream(Files.newInputStream(source))){in.skipNBytes(4);int n=in.readUnsignedShort();in.skipNBytes(n);Sealed.open(key,in,plain);}
        return DesktopRecovery.restore(store,plain.toString(StandardCharsets.UTF_8));
    }
}
