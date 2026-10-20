// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import java.awt.Component;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;

/**
 * Every box that picks a file or a folder on this PC: Windows' own, as Explorer shows it (the owner, 2026-10-18: "make sure we
 * use the latest explorer windows, not this legacy one"; decision 200), and Java's only where Windows' cannot be shown. It
 * opens where the last one was left.
 */
final class DesktopPick {
    private DesktopPick(){}

    private static Path last;
    private static Path start() {
        if(last!=null&&Files.isDirectory(last))return last;
        Path documents=Path.of(System.getProperty("user.home"),"Documents");
        return Files.isDirectory(documents)?documents:Path.of(System.getProperty("user.home"));
    }
    private static void remember(Path chosen){if(chosen!=null)last=Files.isDirectory(chosen)?chosen:chosen.getParent();}
    private static Window window(Component c){return c==null?null:c instanceof Window w?w:SwingUtilities.getWindowAncestor(c);}

    /** Files to open: one, or several. Empty for Cancel. */
    static List<Path> open(Component owner,String title,boolean many) {
        try {
            List<Path> got=DesktopFileDialog.openAny(window(owner),Say.t(title),start(),many);
            if(got==null)return List.of();
            if(!got.isEmpty())remember(got.get(0));
            return got;
        } catch(DesktopFileDialog.Unavailable old){/* Java's own box, then */}
        JFileChooser pick=new JFileChooser(start().toFile());pick.setDialogTitle(Say.t(title));pick.setMultiSelectionEnabled(many);
        if(pick.showOpenDialog(owner)!=JFileChooser.APPROVE_OPTION)return List.of();
        List<Path> out=new ArrayList<>();
        java.io.File[] chosen=pick.getSelectedFiles();
        if(chosen.length==0&&pick.getSelectedFile()!=null)chosen=new java.io.File[]{pick.getSelectedFile()};
        for(java.io.File one:chosen)out.add(one.toPath());
        if(!out.isEmpty())remember(out.get(0));
        return out;
    }
    /** One file to open, or null. */
    static Path openOne(Component owner,String title){List<Path> got=open(owner,title,false);return got.isEmpty()?null:got.get(0);}

    /** Where to save a file named {@code name}, or null. Windows' box itself asks before writing over one already there. */
    static Path save(Component owner,String title,String name) {
        try {
            Path got=DesktopFileDialog.saveAny(window(owner),Say.t(title),start(),name,Say.t("Save"));
            remember(got);return got;
        } catch(DesktopFileDialog.Unavailable old){/* Java's own box, then */}
        JFileChooser pick=new JFileChooser(start().toFile());pick.setDialogTitle(Say.t(title));pick.setSelectedFile(start().resolve(name).toFile());
        if(pick.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION||pick.getSelectedFile()==null)return null;
        Path got=pick.getSelectedFile().toPath();
        if(Files.exists(got)&&!DesktopUi.confirm(window(owner),"Replace file?","A file called "+got.getFileName()+" is already there. Replace it?","Replace",true))return null;
        remember(got);return got;
    }

    /** A folder, or null. */
    static Path folder(Component owner,String title,String ok) {
        try {
            Path got=DesktopFileDialog.folder(window(owner),Say.t(title),start(),Say.t(ok));
            remember(got);return got;
        } catch(DesktopFileDialog.Unavailable old){/* Java's own box, then */}
        JFileChooser pick=new JFileChooser(start().toFile());pick.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);pick.setDialogTitle(Say.t(title));
        if(pick.showDialog(owner,Say.t(ok))!=JFileChooser.APPROVE_OPTION||pick.getSelectedFile()==null)return null;
        Path got=pick.getSelectedFile().toPath();remember(got);return got;
    }
}
