package org.mininotes.android;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import org.mininotes.desktop.platform.AtomicFile;
import org.mininotes.desktop.platform.content.Context;
import org.mininotes.desktop.platform.content.SharedPreferences;

/**
 * The same P-256 wire keys as the phone's and the PC's, kept on the backup server (decision 330) as files only its own user can
 * read: they let it pair and take what the owner's devices push, and open nothing, since every backup is sealed by its device.
 */
final class Keys {
    private final Context context;
    private KeyPair signing,agreement;
    Keys(Context context){this.context=context;}
    synchronized KeyPair signing() throws Exception {if(signing==null)signing=kept("signing");return signing;}
    synchronized KeyPair agreement() throws Exception {if(agreement==null)agreement=kept("agreement");return agreement;}
    static PublicKey publicKey(byte[] encoded) throws GeneralSecurityException{return Point.read(encoded);}
    String line(String name,String address,String offer,boolean writes,String scope,String target) throws Exception {
        return Pairing.write(name,address,Point.shorten(agreement().getPublic()),Point.shorten(signing().getPublic()),offer,writes,scope,target);
    }
    String line(String name,String address,String offer,Sharing.Level level,String scope,String target) throws Exception {
        return Pairing.write(name,address,Point.shorten(agreement().getPublic()),Point.shorten(signing().getPublic()),offer,level,scope,target);
    }
    private KeyPair kept(String name) throws Exception {
        Path file=context.getFilesDir().toPath().resolve("keys").resolve(name+".key");
        if(Files.exists(file)) {
            byte[] raw=Files.readAllBytes(file);
            try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw))) {
                int size=in.readInt();if(size<1||size>512)throw new IOException("This server's key is damaged");
                byte[] secret=in.readNBytes(size),pub=in.readAllBytes();
                return new KeyPair(publicKey(pub),KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(secret)));
            } finally {java.util.Arrays.fill(raw,(byte)0);}
        }
        KeyPair made=Envelope.keys();
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            byte[] secret=made.getPrivate().getEncoded();out.writeInt(secret.length);out.write(secret);out.write(made.getPublic().getEncoded());
            java.util.Arrays.fill(secret,(byte)0);
        }
        byte[] raw=bytes.toByteArray();
        try{AtomicFile.write(file,raw);SharedPreferences.ownOnly(file);}finally{java.util.Arrays.fill(raw,(byte)0);}
        return made;
    }
}
