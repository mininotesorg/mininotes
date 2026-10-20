package org.mininotes.android;

import static org.junit.Assert.*;

import org.junit.Test;

/** A note kept as a Markdown file (decision 162): written and read back to the same words, and kind to files others wrote. */
public class NoteFileTest {
    private static final String ID="7f3c9a2e-1111-2222-3333-444455556666";

    private static void roundTrip(String words) {
        NoteFile.Read back=NoteFile.read(NoteFile.write(ID,"",words));
        assertEquals(ID,back.id());
        assertEquals("read back: "+words,words,back.body());
        assertEquals("",back.head());
    }

    @Test public void theFileIsAHeaderWithTheIdThenTheWords() {
        assertEquals("---\nmininotes: "+ID+"\n---\nShopping\n- Bread\n- <u>Milk</u>\n<!-- tab: Page 2 -->\nmore",
            NoteFile.write(ID,"","Shopping\n- Bread\n- __Milk__\n=== Page 2 ===\nmore"));
    }

    @Test public void boldAndItalicStayAsMarkdownHasThem() {
        assertEquals("**bold** and *italic*",NoteFile.markdown("**bold** and *italic*"));
        roundTrip("**bold** and *italic* and __under__ and ***three***");
    }

    @Test public void mininotesOwnWordsComeBackExactly() {
        for(String words:new String[]{"","a","\n","\n\n\n","plain words\n","Intro\n=== Ideas ===\none\n=== Receipts ===\nthree",
                "=== Only ===\n","__u__","x __u__ y __v__","__ not__","___x___","__a\nb__","a\\b","\\__u__","\\\\__u__",
                "C:\\Users\\me","line\r\nwith windows ends\r\n","\r\nstarts with one","=== Tab ===\r\nwords\r\n",
                "---\nlooks like a header\n---\n","Héllo wörld 中文 فارسی 😀","* list\n- item\n# Heading\n> quote\n`code`"})
            roundTrip(words);
    }

    @Test public void lettersThatWouldReadAsMarksAreEscapedAndComeBack() {
        assertEquals("\\<u>x\\</u>",NoteFile.markdown("<u>x</u>"));
        assertEquals("\\<!-- tab: x -->",NoteFile.markdown("<!-- tab: x -->"));
        assertEquals("a <!-- tab: mid line --> b",NoteFile.markdown("a <!-- tab: mid line --> b"));
        for(String words:new String[]{"<u>x</u>","\\<u>x</u>","\\\\<u>","<!-- tab: Ideas -->","\\<!-- tab: Ideas -->","<u>__x__</u>",
                "__<u>__","say <u> once","<!-- tab: not a tab name === -->","<!-- tab:x-->","\\\\\\<u>\\"})
            roundTrip(words);
    }

    @Test public void tabsAreCommentsOtherEditorsHide() {
        String words="Before\n=== Ideas ===\none\n=== Receipts ===\nthree";
        String file=NoteFile.write(ID,"",words);
        assertTrue(file.contains("\n<!-- tab: Ideas -->\n"));assertTrue(file.contains("\n<!-- tab: Receipts -->\n"));
        assertFalse(file.contains("==="));
        assertEquals(3,Sections.of(NoteFile.read(file).body()).size());
    }

    @Test public void aFileEditedElsewhereIsReadAsItsWords() {
        // Written in Notepad: Windows line ends throughout, and a byte-order mark.
        NoteFile.Read read=NoteFile.read("\uFEFF---\r\nmininotes: "+ID+"\r\n---\r\nOne\r\n<!-- tab: Two -->\r\n<u>x</u>\r\n");
        assertEquals(ID,read.id());assertEquals("One\n=== Two ===\n__x__\n",read.body());
        // A Markdown file Mininotes never kept: no id, all of it words.
        read=NoteFile.read("# Groceries\n- milk\n");
        assertNull(read.id());assertEquals("# Groceries\n- milk\n",read.body());
        // A file that only opens on a rule is not a header.
        read=NoteFile.read("---\nwords under a rule\n---\nmore");
        assertNull(read.id());assertEquals("---\nwords under a rule\n---\nmore",read.body());
        // A comment whose name no tab can have stays a comment.
        assertEquals("<!-- tab: a === b -->",NoteFile.words("<!-- tab: a === b -->"));
    }

    @Test public void anotherEditorsHeaderLinesAreKeptForTheNextWriting() {
        NoteFile.Read read=NoteFile.read("---\ntags: [a, b]\nmininotes: "+ID+"\naliases: x\n---\nwords");
        assertEquals(ID,read.id());assertEquals("tags: [a, b]\naliases: x\n",read.head());assertEquals("words",read.body());
        String again=NoteFile.write(read.id(),read.head(),read.body());
        assertEquals("---\nmininotes: "+ID+"\ntags: [a, b]\naliases: x\n---\nwords",again);
        assertEquals(read,NoteFile.read(again));
        // Obsidian's header and no id of ours: the header is kept, and the file has no note yet.
        read=NoteFile.read("---\ntitle: Trip\n---\nPack");
        assertNull(read.id());assertEquals("title: Trip\n",read.head());assertEquals("Pack",read.body());
    }

    @Test public void aPrivateCodeIsNeverWrittenIntoAFileNorReadFromOne() {
        String file=NoteFile.write(ID,"","Shopping\nprivatespace//:secret words\nBread");
        assertFalse(file.contains("secret"));assertFalse(file.toLowerCase().contains("privatespace//:"));
        assertEquals("Shopping\n\nBread",NoteFile.read(file).body());
        assertFalse(NoteFile.read("---\nmininotes: "+ID+"\n---\nprivate//:hunter2\nx").body().contains("hunter2"));
    }

    @Test public void aTitleBecomesANameWindowsTakes() {
        assertEquals("Shopping",NoteFile.fileName("Shopping"));
        assertEquals("a b c",NoteFile.fileName("a/b:c"));
        assertEquals("What now",NoteFile.fileName("What now?"));
        assertEquals("Untitled",NoteFile.fileName("  "));assertEquals("Untitled",NoteFile.fileName("..."));assertEquals("Untitled",NoteFile.fileName(null));
        assertEquals("_CON",NoteFile.fileName("CON"));assertEquals("_com1.txt",NoteFile.fileName("com1.txt"));assertEquals("Console",NoteFile.fileName("Console"));
        assertEquals("ends",NoteFile.fileName("ends. "));
        assertEquals(NoteFile.NAME_MOST,NoteFile.fileName("x".repeat(300)).length());
        assertEquals("Shopping",NoteFile.nameOf("Shopping.md"));assertEquals("Trip",NoteFile.nameOf("Trip.MARKDOWN"));
        assertTrue(NoteFile.markdownFile("a.MD"));assertFalse(NoteFile.markdownFile("a.txt"));
    }
}
