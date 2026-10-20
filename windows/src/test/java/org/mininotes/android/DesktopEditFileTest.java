package org.mininotes.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;

/** Edit a file… writes back only what was typed: the file's own line ends and mark kept, no header added (docs/FILES.md). */
public class DesktopEditFileTest {
    @Test public void windowsLineEndsAndTheMarkComeBackAsTheyWere() {
        byte[] file={(byte)0xEF,(byte)0xBB,(byte)0xBF,'a','\r','\n','b'};
        DesktopEditFile.Read read=DesktopEditFile.Read.of(file);
        assertEquals("a\nb",read.text());assertTrue(read.crlf());assertTrue(read.bom());
        assertArrayEquals(file,read.bytes("a\nb"));
        byte[] more=read.bytes("a\nb\nc");
        assertEquals("﻿a\r\nb\r\nc",new String(more,StandardCharsets.UTF_8));
    }
    @Test public void aPlainFileStaysPlainAndGetsNoHeader() {
        byte[] file="# Notes\nline".getBytes(StandardCharsets.UTF_8);
        DesktopEditFile.Read read=DesktopEditFile.Read.of(file);
        assertFalse(read.crlf());assertFalse(read.bom());
        String back=new String(read.bytes("# Notes\nline\nmore"),StandardCharsets.UTF_8);
        assertEquals("# Notes\nline\nmore",back);
        assertFalse("no Mininotes header is written into it",back.contains("mininotes:"));
    }
}
