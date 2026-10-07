// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * A cry for help, sent the moment a note or folder set to send one is opened (the owner, 2026-10-06: "could we even make it
 * that if one note is open, it sends a help request to a contact with the phone location, all this of course fully
 * secretly?"; docs/HOME.md, decision 113).
 *
 * <p>It rides as a card of its own, like a {@link Parlons} address: four bytes that say what it is, read before a note is,
 * so it is never taken for somebody's words. It is sealed and signed like everything else, so on the network it looks like
 * any other sealed message; who it is from is the key the envelope is signed with, which the far end turns into a name from
 * its own contacts. It is never sealed for a device that has not said it reads one ({@link Receipt#HELP}): a build from
 * before would write it over a note, silently, which is the one thing this must not do.
 *
 * <p>One opening is one incident, named by sixteen random bytes, and every update of it (the first with the last known
 * place, then the precise one when the GPS answers, then one every five minutes for an hour) carries the same incident and
 * a number that climbs, so the far end shows one alert and fills in its place and time as the updates come. The PC has no
 * GPS, so it sends one update with no place and says so.
 *
 * <p>Holds no Android types: the format, the map links and the words are unit tested.
 */
final class Help {
    /** The format, and the first four bytes. Not a {@link Parlons} card, a {@link Groups} card, a {@link Parcel} or anything else. */
    static final byte[] MAGIC={'M','N','H','1'};
    /** One byte after the magic, so a later build can say more without being read as this one. */
    static final int FORMAT=1;

    /** How long the words can be: a short message the owner wrote, not a letter. */
    static final int MOST_WORDS=280;
    /** Sixteen bytes name one opening, so every update of it is shown as one alert. */
    static final int INCIDENT=16;

    /** The switch's words, the same on both apps, and the button that turns it on after the box has said what it does. */
    static final String SWITCH="Send a help request when opened", TURN_ON="Turn it on";
    /** The box that asks before it is turned on (the owner, 2026-10-06: "By default they are not, and request confirmation before being set."). */
    static final String ASK_TITLE="Turn on the help request?";
    /** What will happen, the same words on both apps: sent at once when it opens, with nothing on screen. */
    static final String WHAT="The moment this opens, a help request is sent to the people you choose, with your message. Nothing shows on screen.";
    /** What a phone sends: the place, then precise, then every five minutes for an hour while the app is open. */
    static final String PHONE_PLACE="Your location is sent: the last known at once, then the precise one when the GPS answers, then once every 5 minutes for an hour while Mininotes is open.";
    /** What a computer sends: no place, since it has none. */
    static final String PC_PLACE="This computer has no location, so the request is sent without one.";
    /** The warning for an ordinary note or folder: the setting is in the notebook, where a forensic look finds it (decision 113). */
    static final String ORDINARY_TRACE="This setting is kept in the notebook. Someone who examines this device's data could find it.";
    /** And for a private one: it is inside the private space, with no trace outside (decision 111). */
    static final String PRIVATE_TRACE="This setting is kept inside the private space. Nothing outside it shows that it is set.";
    /** Said when nobody was chosen, and the hint in the message field. */
    static final String NEED_SOMEONE="Choose at least one person to send it to.";
    static final String MESSAGE_HINT="A short message: where you are, or what is wrong";
    /** The heading over the people to choose, and why the phone asks for the location. */
    static final String TO="Send it to", WHY_LOCATION="Mininotes needs your location only to send it with a help request you have asked it to send.";

    private Help(){}

    /** One help request, as a card says it: an opening named by its incident, a climbing number, the words, and a place or none. */
    static final class Request {
        final byte[] incident; final int number; final String message;
        final boolean located; final double lat,lon; final float metres; final long placeAt,at;
        Request(byte[] incident,int number,String message,boolean located,double lat,double lon,float metres,long placeAt,long at) {
            this.incident=incident; this.number=number; this.message=message==null?"":message;
            this.located=located; this.lat=lat; this.lon=lon; this.metres=metres; this.placeAt=placeAt; this.at=at;
        }
    }

    /** The inside of a card. */
    static byte[] wrap(Request r) {
        if(r.incident==null||r.incident.length!=INCIDENT)throw new IllegalArgumentException("A help request needs a sixteen-byte incident.");
        String said=r.message.trim();
        if(said.length()>MOST_WORDS)said=said.substring(0,MOST_WORDS);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)) {
            out.write(MAGIC);out.writeByte(FORMAT);
            out.write(r.incident);out.writeInt(r.number);
            byte[] text=said.getBytes(StandardCharsets.UTF_8);out.writeShort(text.length);out.write(text);
            out.writeBoolean(r.located);
            out.writeDouble(r.located?r.lat:0);out.writeDouble(r.located?r.lon:0);
            out.writeFloat(r.located?r.metres:0);out.writeLong(r.located?r.placeAt:0);
            out.writeLong(r.at);
        } catch(IOException e){throw new IllegalArgumentException(e.getMessage(),e);}
        return bytes.toByteArray();
    }

    /** Whether these bytes say they are a help request, before anything else is asked of them. */
    static boolean isCard(byte[] said) {
        if(said==null||said.length<MAGIC.length)return false;
        for(int at=0;at<MAGIC.length;at++)if(said[at]!=MAGIC[at])return false;
        return true;
    }

    /** What a card says, or null where these bytes are not one, or are but broken, out of bounds, or with anything after it. */
    static Request open(byte[] said) {
        if(!isCard(said)||said.length>Envelope.MAX_TEXT)return null;
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(said,MAGIC.length,said.length-MAGIC.length))) {
            if(in.readUnsignedByte()!=FORMAT)return null;
            byte[] incident=new byte[INCIDENT];in.readFully(incident);
            int number=in.readInt();
            int length=in.readUnsignedShort();if(length>MOST_WORDS*4)return null;
            byte[] text=new byte[length];in.readFully(text);
            String message=new String(text,StandardCharsets.UTF_8);
            boolean located=in.readBoolean();
            double lat=in.readDouble(),lon=in.readDouble();float metres=in.readFloat();long placeAt=in.readLong();
            long at=in.readLong();
            if(number<0||at<=0||in.available()!=0)return null;
            if(located&&(lat<-90||lat>90||lon<-180||lon>180))return null;
            if(message.length()>MOST_WORDS)message=message.substring(0,MOST_WORDS);
            return new Request(incident,number,message,located,lat,lon,metres,placeAt,at);
        } catch(IOException broken){return null;}
    }

    // ---- the map links and the words, the same on both apps ----------------------------------------------------------

    /** A geo: link a phone's map app opens, with a marker; lat and lon with a full stop, whatever the phone's language. */
    static String geo(double lat,double lon) {
        return "geo:"+num(lat)+","+num(lon)+"?q="+num(lat)+","+num(lon);
    }

    /** An https map anybody can open, OpenStreetMap, which needs no account and keeps nothing. */
    static String web(double lat,double lon) {
        return "https://www.openstreetmap.org/?mlat="+num(lat)+"&mlon="+num(lon)+"#map=17/"+num(lat)+"/"+num(lon);
    }

    /** A number for a link: a full stop for the point, never a comma, and not in scientific form. */
    private static String num(double d){return new java.math.BigDecimal(d).setScale(6,java.math.RoundingMode.HALF_UP).toPlainString();}

    /** The heading of the alert: loud, and whose it is. */
    static String title(String name){return "HELP from "+(name==null||name.trim().isEmpty()?"a contact":name.trim());}

    /**
     * The body of the alert, the same words on both apps: the message, then the place and when it was fixed, or that there
     * is no place (a PC sent it, or the GPS has not answered yet), then the map links. {@code when} is already in words, so
     * each app says the time its own way.
     */
    static String body(String message,boolean located,double lat,double lon,float metres,String placeWhen) {
        StringBuilder b=new StringBuilder();
        String said=message==null?"":message.trim();
        if(!said.isEmpty())b.append(said).append("\n\n");
        if(located) {
            b.append("Near ").append(num(lat)).append(", ").append(num(lon));
            if(metres>0)b.append(" (about ").append(Math.round(metres)).append(" m)");
            if(placeWhen!=null&&!placeWhen.isEmpty())b.append(", ").append(placeWhen);
            b.append("\n").append(web(lat,lon));
        } else {
            b.append("No location was sent.");
        }
        return b.toString();
    }
}
