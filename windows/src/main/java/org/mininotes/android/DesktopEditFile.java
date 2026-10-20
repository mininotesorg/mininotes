package org.mininotes.android;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.List;
import javax.swing.*;
import org.mininotes.desktop.platform.AtomicFile;

/**
 * Edit a file…: a file opened, worked in and closed, as Notepad does it, and never part of Mininotes (the owner, 2026-10-14:
 * "for an external file I just want to open, work in and close, how should we present it when I don't want it in the app?";
 * closed, "I prefer removed completely"; docs/FILES.md). No note is made, nothing is synced, no header is written into the
 * file: its words are read, saved into it a moment after typing stops, on Ctrl+S and on closing, and nothing stays here once
 * the window is closed. Changed outside while open, it asks which words to keep, as a note kept as a file does (decision 171).
 */
final class DesktopEditFile {
    private DesktopEditFile(){}

    /** The most a file opened this way may be: a text, not a library. */
    static final long MOST=5L<<20;

    /** Edit a file…: the files picked, each in a window of its own. */
    static void open(Desktop d) {
        List<Path> files;
        try{files=DesktopFileDialog.openAny(d.frame,"Edit a file",folder(d),true);}
        catch(DesktopFileDialog.Unavailable old) {
            JFileChooser pick=new JFileChooser(folder(d).toFile());pick.setMultiSelectionEnabled(true);pick.setDialogTitle(Say.t("Edit a file"));
            if(pick.showOpenDialog(d.frame)!=JFileChooser.APPROVE_OPTION)return;
            java.io.File[] chosen=pick.getSelectedFiles();if(chosen.length==0&&pick.getSelectedFile()!=null)chosen=new java.io.File[]{pick.getSelectedFile()};
            files=new java.util.ArrayList<>();for(java.io.File f:chosen)files.add(f.toPath());
        }
        if(files==null||files.isEmpty())return;
        d.context.getSharedPreferences("settings",0).edit().putString("onDiskFolder",files.get(0).toAbsolutePath().getParent().toString()).apply();
        for(Path f:files)open(d,f);
    }

    private static Path folder(Desktop d) {
        String kept=d.context.getSharedPreferences("settings",0).getString("onDiskFolder","");
        if(!kept.isEmpty()&&Files.isDirectory(Path.of(kept)))return Path.of(kept);
        Path documents=Path.of(System.getProperty("user.home"),"Documents");
        return Files.isDirectory(documents)?documents:Path.of(System.getProperty("user.home"));
    }

    /** One file in its window; said on the status line where it cannot be opened. */
    static void open(Desktop d,Path file) {
        try {
            if(Files.size(file)>MOST){d.status.setText(Say.t("{0} is too long to open here",file.getFileName().toString()));return;}
            new Window(d,file).show();
        } catch(IOException|RuntimeException e) {
            d.status.setText(Say.t("{0} could not be opened",file.getFileName().toString()));
        }
    }

    /** A file's words as they are kept, its line ends taken as Mininotes' own, and how to write them back the same way. */
    record Read(String text,boolean crlf,boolean bom) {
        static Read of(byte[] bytes) {
            boolean bom=bytes.length>=3&&(bytes[0]&0xff)==0xEF&&(bytes[1]&0xff)==0xBB&&(bytes[2]&0xff)==0xBF;
            String text=new String(bytes,bom?3:0,bytes.length-(bom?3:0),StandardCharsets.UTF_8);
            boolean crlf=text.contains("\r\n");
            return new Read(text.replace("\r\n","\n"),crlf,bom);
        }
        /** The words written back with the file's own line ends and mark, so nothing changes but what was typed. */
        byte[] bytes(String words) {
            byte[] body=(crlf?words.replace("\n","\r\n"):words).getBytes(StandardCharsets.UTF_8);
            if(!bom)return body;
            byte[] out=new byte[body.length+3];out[0]=(byte)0xEF;out[1]=(byte)0xBB;out[2]=(byte)0xBF;System.arraycopy(body,0,out,3,body.length);return out;
        }
    }

    private static final class Window {
        private final Desktop d;private final Path file;private final JFrame frame;private final JTextArea page=new JTextArea();
        private final JLabel said=new JLabel(" ");private final Timer soon;
        private Read read;private String saved;private FileTime at;private boolean closing;

        Window(Desktop d,Path file) throws IOException {
            this.d=d;this.file=file;
            byte[] bytes=Files.readAllBytes(file);read=Read.of(bytes);saved=read.text();at=Files.getLastModifiedTime(file);
            frame=new JFrame(file.getFileName()+"  ·  Mininotes");
            frame.setIconImages(d.frame.getIconImages());
            page.setText(saved);page.setCaretPosition(0);page.setLineWrap(true);page.setWrapStyleWord(true);
            page.setFont(DesktopUi.BODY.deriveFont(15f));page.setForeground(DesktopUi.INK);page.setBackground(DesktopUi.PAPER);
            page.setBorder(BorderFactory.createEmptyBorder(16,22,16,22));
            JScrollPane scroll=new JScrollPane(page);scroll.setBorder(BorderFactory.createEmptyBorder());
            JPanel foot=new JPanel(new BorderLayout());foot.setBackground(DesktopUi.SHELF);foot.setBorder(BorderFactory.createEmptyBorder(6,16,6,16));
            said.setFont(DesktopUi.BODY.deriveFont(13f));said.setForeground(DesktopUi.QUIET);
            JLabel where=new JLabel(String.valueOf(file.toAbsolutePath().getParent()));where.setFont(DesktopUi.BODY.deriveFont(12f));where.setForeground(DesktopUi.QUIET);
            foot.add(said,BorderLayout.WEST);foot.add(where,BorderLayout.EAST);
            frame.getContentPane().setLayout(new BorderLayout());frame.getContentPane().add(scroll,BorderLayout.CENTER);frame.getContentPane().add(foot,BorderLayout.SOUTH);
            soon=new Timer(1500,e->save(false));soon.setRepeats(false);
            page.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
                public void insertUpdate(javax.swing.event.DocumentEvent e){typed();}
                public void removeUpdate(javax.swing.event.DocumentEvent e){typed();}
                public void changedUpdate(javax.swing.event.DocumentEvent e){}
            });
            page.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_DOWN_MASK),"saveNow");
            page.getActionMap().put("saveNow",new AbstractAction(){public void actionPerformed(ActionEvent e){save(false);}});
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){close();}});
            said.setText(Say.t("Saved into {0} as you write. Nothing of it is kept in Mininotes.",file.getFileName().toString()));
            frame.setSize(820,760);frame.setLocationRelativeTo(d.frame);
        }
        void show(){frame.setVisible(true);page.requestFocusInWindow();}

        private void typed(){if(closing)return;said.setText(Say.t("Saving…"));soon.restart();}

        /** The words into the file, unless it changed outside since: then asked which words to keep. Said either way. */
        private boolean save(boolean asClosing) {
            String now=page.getText();
            try {
                if(Files.exists(file)&&!Files.getLastModifiedTime(file).equals(at)) {
                    Read there=Read.of(Files.readAllBytes(file));
                    if(!there.text().equals(saved)&&!there.text().equals(now)) {
                        // Changed outside while open: neither is written over until the owner says which.
                        Object[] ways={Say.t("Keep the file's words"),Say.t("Keep my words")};
                        int picked=JOptionPane.showOptionDialog(frame,Say.t("{0} changed outside while it was open here. Which words should it keep?",file.getFileName().toString()),
                            Say.t("Changed in two places"),JOptionPane.DEFAULT_OPTION,JOptionPane.QUESTION_MESSAGE,null,ways,ways[0]);
                        if(picked==0){read=there;saved=there.text();at=Files.getLastModifiedTime(file);page.setText(saved);said.setText(Say.t("The file's words are kept"));return true;}
                        if(picked!=1){said.setText(Say.t("Not saved: the file changed outside"));return false;}
                    }
                }
                if(now.equals(saved)&&Files.exists(file)){said.setText(Say.t("Saved"));return true;}
                AtomicFile.write(file,read.bytes(now));
                saved=now;at=Files.getLastModifiedTime(file);
                said.setText(Say.t(asClosing?"Saved":"Saved into {0}",file.getFileName().toString()));
                return true;
            } catch(IOException|RuntimeException e) {
                said.setText(Say.t("Not saved: {0}",e.getMessage()==null?Say.t("the file could not be written"):e.getMessage()));
                return false;
            }
        }

        /** Saved, then gone: nothing of it stays. A save that fails keeps the window, so no word is lost. */
        private void close() {
            soon.stop();
            if(!save(true)) {
                int stay=JOptionPane.showConfirmDialog(frame,Say.t("{0} could not be saved. Close it anyway? What was typed since the last save is lost.",file.getFileName().toString()),
                    Say.t("Not saved"),JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
                if(stay!=JOptionPane.YES_OPTION)return;
            }
            closing=true;frame.dispose();
            d.status.setToolTipText(null);d.status.setText(Say.t("{0} is saved and closed",file.getFileName().toString()));
        }
    }
}
