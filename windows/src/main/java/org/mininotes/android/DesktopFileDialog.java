// SPDX-License-Identifier: GPL-3.0-or-later
// Mininotes is free software: GNU General Public License, version 3 or later. See LICENSE.
package org.mininotes.android;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.awt.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Windows' own file box, as Explorer and Notepad show it, for saving and opening a Markdown file and for choosing a folder
 * (the owner, 2026-10-13: "make sure we use the modern Windows Explorer when saving and opening a MD file, not this old
 * school one"). Java's two boxes both draw the old one; this is IFileOpenDialog and IFileSaveDialog, called through JNA,
 * on a thread of their own with COM in it, while the window waits, still drawn (a secondary loop), behind the box that
 * Windows keeps in front of it. Where Windows cannot give it, the answer is {@link Unavailable} and the caller uses Java's.
 */
final class DesktopFileDialog {
    private DesktopFileDialog(){}

    /** Windows' box could not be shown here: not Windows, or COM refused. Java's box is used instead. */
    static final class Unavailable extends Exception {Unavailable(String why){super(why);}}

    private static final Guid.GUID FILE_OPEN=Guid.GUID.fromString("{DC1C5A9C-E88A-4dde-A5A1-60F82A20AEF7}"),
        FILE_SAVE=Guid.GUID.fromString("{C0B4E2F3-BA21-4773-8DBA-335EC946EB8B}"),
        I_OPEN=Guid.GUID.fromString("{d57c7288-d4ad-4768-be02-9d969532d960}"),
        I_SAVE=Guid.GUID.fromString("{84bccd23-5fde-4cdb-aea4-af64b83d78ab}"),
        I_ITEM=Guid.GUID.fromString("{43826d1e-e718-42ee-bc55-a1e261c37bfe}");
    // The boxes' functions, by their place in IFileDialog, IFileOpenDialog, IShellItem and IShellItemArray.
    private static final int RELEASE=2,SHOW=3,SET_FILE_TYPES=4,SET_OPTIONS=9,GET_OPTIONS=10,SET_FOLDER=12,SET_FILE_NAME=15,
        SET_TITLE=17,SET_OK_LABEL=18,GET_RESULT=20,SET_DEFAULT_EXTENSION=22,GET_RESULTS=27,ITEM_NAME=5,ARRAY_COUNT=7,ARRAY_ITEM=8;
    private static final int PICK_FOLDERS=0x20,FILE_SYSTEM=0x40,MANY=0x200,PATH_MUST_EXIST=0x800,FILE_MUST_EXIST=0x1000,NO_CHANGE_DIR=0x8;
    private static final int FILE_SYSTEM_PATH=0x80058000,CANCELLED=0x800704C7;

    private interface Shell extends StdCallLibrary {
        Shell SHELL=Native.load("shell32",Shell.class,W32APIOptions.DEFAULT_OPTIONS);
        int SHCreateItemFromParsingName(WString path,Pointer context,Guid.GUID kind,PointerByReference item);
    }

    /** Save as: a Markdown file, named {@code name} at first, in {@code folder}. The path chosen, or null for Cancel. */
    static Path save(Window owner,String title,Path folder,String name,String ok) throws Unavailable {
        List<Path> got=show(owner,false,title,folder,name,ok,false,false);
        return got==null||got.isEmpty()?null:got.get(0);
    }
    /** Save as: any kind of file, named {@code name} at first (Save on my computer, a backup, a recording). */
    static Path saveAny(Window owner,String title,Path folder,String name,String ok) throws Unavailable {
        List<Path> got=show(owner,false,title,folder,name,ok,false,false,true);
        return got==null||got.isEmpty()?null:got.get(0);
    }
    /** Open: one Markdown file, or several. The paths chosen, or null for Cancel. */
    static List<Path> open(Window owner,String title,Path folder,boolean many) throws Unavailable {
        return show(owner,true,title,folder,null,null,false,many);
    }
    /** Open: any kind of file, one or several, with no kind offered first (Share a file through Mininotes…, Edit a file…). */
    static List<Path> openAny(Window owner,String title,Path folder,boolean many) throws Unavailable {
        return show(owner,true,title,folder,null,null,false,many,true);
    }
    /** A folder chosen. The path, or null for Cancel. */
    static Path folder(Window owner,String title,Path folder,String ok) throws Unavailable {
        List<Path> got=show(owner,true,title,folder,null,ok,true,false);
        return got==null||got.isEmpty()?null:got.get(0);
    }

    private static boolean windows(){return System.getProperty("os.name","").startsWith("Windows");}

    /** The box, on a thread of its own; the window's events go on while it is up. */
    private static List<Path> show(Window owner,boolean opening,String title,Path folder,String name,String ok,boolean folders,boolean many) throws Unavailable {
        return show(owner,opening,title,folder,name,ok,folders,many,false);
    }
    /** @param anyKind every kind of file shown, with no Markdown first */
    private static List<Path> show(Window owner,boolean opening,String title,Path folder,String name,String ok,boolean folders,boolean many,boolean anyKind) throws Unavailable {
        if(!windows())throw new Unavailable("not Windows");
        Pointer window=owner==null||!owner.isDisplayable()?null:Native.getWindowPointer(owner);
        AtomicReference<Object> answer=new AtomicReference<>();
        SecondaryLoop wait=EventQueue.isDispatchThread()?Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop():null;
        Thread box=new Thread(()->{
            try{answer.set(ask(window,opening,title,folder,name,ok,folders,many,anyKind));}
            catch(Throwable failed){answer.set(failed);}
            finally{if(wait!=null)wait.exit();}
        },"mininotes-file-box");
        box.setDaemon(true);box.start();
        if(wait!=null)wait.enter();
        try{box.join();}catch(InterruptedException stopped){Thread.currentThread().interrupt();throw new Unavailable("stopped");}
        Object got=answer.get();
        if(got instanceof Unavailable u)throw u;
        if(got instanceof Throwable t)throw new Unavailable(t.getClass().getSimpleName()+": "+t.getMessage());
        @SuppressWarnings("unchecked") List<Path> paths=(List<Path>)got;
        return paths;
    }

    private static int call(Pointer object,int at,Object... args) {
        Pointer table=object.getPointer(0);
        Function f=Function.getFunction(table.getPointer((long)at*Native.POINTER_SIZE),Function.ALT_CONVENTION);
        Object[] all=new Object[args.length+1];all[0]=object;System.arraycopy(args,0,all,1,args.length);
        return f.invokeInt(all);
    }
    private static void release(Pointer object){if(object!=null)call(object,RELEASE);}
    private static void check(int result,String what) throws Unavailable{if(result<0)throw new Unavailable(what+" 0x"+Integer.toHexString(result));}
    /** Wide text for the box, held in {@code kept} until it is closed, so none of it is let go while Windows reads it. */
    private static Memory wide(String s,List<Memory> kept){Memory m=new Memory((s.length()+1L)*2);m.setWideString(0,s);kept.add(m);return m;}

    /** On the box's own thread: COM, the box made and set, shown, and what was chosen read back as paths. */
    private static List<Path> ask(Pointer window,boolean opening,String title,Path folder,String name,String ok,boolean folders,boolean many,boolean anyKind) throws Unavailable {
        int com=Ole32.INSTANCE.CoInitializeEx(null,Ole32.COINIT_APARTMENTTHREADED).intValue();
        if(com<0)throw new Unavailable("COM 0x"+Integer.toHexString(com));
        Pointer dialog=null;List<Memory> kept=new ArrayList<>();
        try {
            PointerByReference made=new PointerByReference();
            check(Ole32.INSTANCE.CoCreateInstance(opening?FILE_OPEN:FILE_SAVE,null,WTypes.CLSCTX_INPROC_SERVER,opening?I_OPEN:I_SAVE,made).intValue(),"the box");
            dialog=made.getValue();
            IntByReference options=new IntByReference();
            check(call(dialog,GET_OPTIONS,options),"its options");
            int wanted=options.getValue()|FILE_SYSTEM|NO_CHANGE_DIR|PATH_MUST_EXIST;
            if(folders)wanted|=PICK_FOLDERS;else if(opening)wanted|=FILE_MUST_EXIST;
            if(many)wanted|=MANY;
            check(call(dialog,SET_OPTIONS,wanted),"its options");
            if(!folders&&anyKind) {
                // Every kind, as Explorer shows a folder: nothing hidden, no kind first (the owner, 2026-10-15).
                Memory spec=new Memory(2L*Native.POINTER_SIZE);
                spec.setPointer(0,wide(Say.t("All files")+" (*.*)",kept));spec.setPointer(Native.POINTER_SIZE,wide("*.*",kept));kept.add(spec);
                check(call(dialog,SET_FILE_TYPES,1,spec),"its kinds of file");
            }
            else if(!folders) {
                // Markdown first, and everything as the other choice, as Notepad offers its own kind and All files.
                Memory spec=new Memory(4L*Native.POINTER_SIZE);
                spec.setPointer(0,wide("Markdown (*.md)",kept));spec.setPointer(Native.POINTER_SIZE,wide("*.md;*.markdown",kept));
                spec.setPointer(2L*Native.POINTER_SIZE,wide(Say.t("All files")+" (*.*)",kept));spec.setPointer(3L*Native.POINTER_SIZE,wide("*.*",kept));kept.add(spec);
                check(call(dialog,SET_FILE_TYPES,2,spec),"its kinds of file");
                check(call(dialog,SET_DEFAULT_EXTENSION,new WString("md")),"its extension");
            }
            if(title!=null)call(dialog,SET_TITLE,new WString(Say.t(title)));
            if(ok!=null)call(dialog,SET_OK_LABEL,new WString(Say.t(ok)));
            if(name!=null)call(dialog,SET_FILE_NAME,new WString(name));
            if(folder!=null&&Files.isDirectory(folder)) {
                PointerByReference item=new PointerByReference();
                if(Shell.SHELL.SHCreateItemFromParsingName(new WString(folder.toAbsolutePath().toString()),null,I_ITEM,item)>=0) {
                    call(dialog,SET_FOLDER,item.getValue());release(item.getValue());
                }
            }
            int shown=call(dialog,SHOW,window);
            if(shown==CANCELLED)return null;
            check(shown,"showing it");
            List<Path> out=new ArrayList<>();
            if(opening&&!folders) {
                PointerByReference items=new PointerByReference();
                check(call(dialog,GET_RESULTS,items),"what was chosen");
                Pointer array=items.getValue();
                try {
                    IntByReference count=new IntByReference();check(call(array,ARRAY_COUNT,count),"how many");
                    for(int i=0;i<count.getValue();i++){PointerByReference one=new PointerByReference();check(call(array,ARRAY_ITEM,i,one),"one chosen");
                        try{out.add(path(one.getValue()));}finally{release(one.getValue());}}
                } finally{release(array);}
            } else {
                PointerByReference one=new PointerByReference();
                check(call(dialog,GET_RESULT,one),"what was chosen");
                try{out.add(path(one.getValue()));}finally{release(one.getValue());}
            }
            return out;
        } finally {
            release(dialog);kept.clear();
            Ole32.INSTANCE.CoUninitialize();
        }
    }
    private static Path path(Pointer item) throws Unavailable {
        PointerByReference said=new PointerByReference();
        check(call(item,ITEM_NAME,FILE_SYSTEM_PATH,said),"its path");
        try{return Path.of(said.getValue().getWideString(0));}finally{Ole32.INSTANCE.CoTaskMemFree(said.getValue());}
    }
}
