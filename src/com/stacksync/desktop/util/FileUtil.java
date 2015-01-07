/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011 Philipp C. Heckel <philipp.heckel@gmail.com> 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.stacksync.desktop.util;

import com.oogly.mime.identifier.magic.MagicMimeTypeIdentifier;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.encryption.BasicEncryption;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.encryption.Encryption;
import com.stacksync.desktop.gui.linux.BrowseFileRequest;
import com.stacksync.desktop.gui.linux.BrowseFileRequest.BrowseType;
import com.stacksync.desktop.gui.linux.LinuxNativeClient;
import com.stacksync.desktop.logging.RemoteLogs;
import java.awt.Desktop;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.InvalidKeyException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.swing.JFileChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class FileUtil {
    private static final MagicMimeTypeIdentifier mimeTypesMap = new MagicMimeTypeIdentifier();
    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());    
    private static final Environment env = Environment.getInstance();
    
    private static final double BASE = 1024, KB = BASE, MB = KB * BASE, GB = MB * BASE;
    private static final DecimalFormat df = new DecimalFormat("#.##");
    

    public static String formatSize(double size) {
        if (size >= GB) {
            return df.format(size / GB) + " GB";
        }

        if (size >= MB) {
            return df.format(size / MB) + " MB";
        }

        if (size >= KB) {
            return df.format(size / KB) + " KB";
        }

        return "" + (int) size + " bytes";
    }

    public static String getRelativePath(File base, File file) {
        //System.err.println("rel path = base = "+base.getAbsolutePath() + " - file: "+file.getAbsolutePath()+ " ---> ");
        if (base.getAbsolutePath().length() >= file.getAbsolutePath().length()) {
            return "";
        }

        //System.err.println("aaa"+file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1));
        return file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1);
    }

    public static String getAbsoluteParentDirectory(File file) {
        return file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator));
    }

    public static String getRelativeParentDirectory(File base, File file) {
        return getRelativePath(base, new File(getAbsoluteParentDirectory(file)));
    }

    public static List<File> getRecursiveFileList(File root, boolean includeDirectories) throws FileNotFoundException {
        if (!root.isDirectory() || !root.canRead() || !root.exists()) {
            throw new FileNotFoundException("Invalid directory " + root);
        }

        List<File> result = getRecursiveFileListNoSort(root, includeDirectories);
        Collections.sort(result);

        return result;
    }

    private static List<File> getRecursiveFileListNoSort(File root, boolean includeDirectories) {
        List<File> result = new ArrayList<File>();
        List<File> filesDirs = Arrays.asList(root.listFiles());

        for (File file : filesDirs) {
            if (!file.isDirectory() || includeDirectories) {
                result.add(file);
            }

            if (file.isDirectory()) {
                List<File> deeperList = getRecursiveFileListNoSort(file, includeDirectories);
                result.addAll(deeperList);
            }
        }

        return result;
    }

    public static String getExtension(String filename, boolean includeDot) {
        int dot = filename.lastIndexOf(".");

        if (dot == -1) {
            return "";
        }

        return ((includeDot) ? "." : "")
                + filename.substring(dot + 1, filename.length());
    }

    /**
     * Retrieves the basename of a file. Example: "index" in the case of
     * "/htdocs/index.html"
     *
     * @param file
     * @return
     */
    public static String getBasename(File file) {
        return getBasename(file.getName());
    }

    public static String getBasename(String filename) {
        int dot = filename.lastIndexOf(".");

        if (dot == -1) {
            return filename;
        }

        return filename.substring(0, dot);
    }

    public static File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException ex) {
            return file;
        }
    }

    public static boolean renameVia(File fromFile, File toFile) {
        return renameVia(fromFile, toFile, ".ignore-rename-to-");
    }

    public static boolean renameVia(File fromFile, File toFile, String viaPrefix) {
        File tempFile = new File(toFile.getParentFile().getAbsoluteFile() + File.separator + viaPrefix + toFile.getName());
        FileUtil.deleteRecursively(tempFile); // just in case!	

        if (!fromFile.renameTo(tempFile)) {
            return false;
        }

        if (!tempFile.renameTo(toFile)) {
            tempFile.renameTo(fromFile);
            return false;
        }

        return true;
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream srcStream = new FileInputStream(src);
        OutputStream dstStream = new FileOutputStream(dst);
        copy(srcStream, dstStream);
        
        srcStream.close();
        dstStream.close();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        // Performance tests say 4K is the fastest (sschellh)
        byte[] buf = new byte[4096];

        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    /**
     * Allows throttling local copy operations.
     *
     * @param src
     * @param dst
     * @param kbps
     * @throws IOException
     */
    public static void copy(File src, File dst, int kbps) throws IOException {
        if (kbps <= 0) {
            copy(src, dst);
            return;
        }

        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        int bytesPer100ms = Math.round(((float) kbps) * 1024 / 10);
        //System.out.println(new Date()+" -- bytes per 100ms:"+bytesPer100ms);
        byte[] buf = new byte[bytesPer100ms];

        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
            //System.out.println(new Date()+" -- copy "+len);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }

        //System.out.println(new Date()+" -- fertig");

        in.close();
        out.close();
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        return FileUtils.readFileToByteArray(file);
    }
    
    public static String readFileToString(File file) throws IOException {
        return FileUtils.readFileToString(file);
    }    
    
    public static void writeFile(String content, File file) throws IOException{
        FileUtils.writeStringToFile(file, content);
    }

    public static void writeFile(byte[] bytes, File file) throws IOException {
        InputStream inStream = new ByteArrayInputStream(bytes);
        writeFile(inStream, file);
        
        inStream.close();
    }

    public static void writeFile(InputStream is, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);

        int read;
        byte[] bytes = new byte[4096];

        while ((read = is.read(bytes)) != -1) {
            fos.write(bytes, 0, read);
        }

        fos.close();
    }

    public static boolean deleteRecursively(File file) {
        boolean success = true;

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (f.isDirectory()) {
                    success = success && deleteRecursively(f);
                }

                success = success && f.delete();
            }
        }

        success = success && file.delete();
        return success;
    }

    public static byte[] gzip(byte[] content) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        gzipOutputStream.write(content);
        gzipOutputStream.close();

        byte[] result = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        
        return result;
    }

    public static byte[] gunzip(byte[] contentBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(new GZIPInputStream(new ByteArrayInputStream(contentBytes)), out);

        byte[] result = out.toByteArray();
        out.close();
        
        return result;
    }

    public static byte[] unpack(byte[] packed, Encryption enc)
            throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        if (enc != null) {
            packed = enc.decrypt(packed);
        }
        return FileUtil.gunzip(packed);
    }

    public static byte[] pack(byte[] raw, Encryption enc)
            throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        byte[] gzipped = FileUtil.gzip(raw);
        if (enc != null) {
            gzipped = enc.encrypt(gzipped);
        }
        return gzipped;
    }

    public static void main(String[] a) throws IOException {
        //System.out.println(getRelativeParentDirectory(new File("/home/pheckel/Coding/stacksync/stacksync-platop"), new File("/home/pheckel/Coding/stacksync/stacksync-platop/untitled folder/untitled folder")));
        //copy(new File("/home/pheckel/freed"), new File("/home/pheckel/freed2"), 100);
        System.out.println(new File("/home/pheckel").getParentFile());
    }

    public static void browsePage(String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ex) { /* Fressen */ }
    }

    private static boolean openLinuxExplorer(String command, File path){        
        boolean result = false;
        
        File f1 = new File(command);
        if(f1.exists()){
            try {
                Runtime.getRuntime().exec(command + " " + path.getAbsolutePath());
                result = true;
            } catch (IOException ex) { 
                System.out.println(ex);
            }            
        }        
        
        return result;
    }
    
    public static void showWindow(final File file) throws IOException {
        
        if (openLinuxExplorer("/usr/bin/nautilus", file)) {
            logger.debug("Opened file with /usr/bin/nautilus");
        } else if (openLinuxExplorer("/usr/share/nautilus", file)) {
            logger.debug("Opened file with /usr/share/nautilus");
        } else if (openLinuxExplorer("/usr/bin/xdg-open", file)) {
            logger.debug("Opened file with /usr/bin/xdg-open");
        } else if (openLinuxExplorer("/usr/share/xdg-open", file)) {
            logger.debug("Opened file with /usr/share/xdg-open");
        } else {
            logger.debug("Opened with the default Desktop.");
            Desktop.getDesktop().open(file);
        }
        
    }
    
    public static void openFile(final File file) {
        try {
                switch (env.getOperatingSystem()) {
                case Linux:                    
                    showWindow(file);
                    break;
                case Mac:
                case Windows:
                default:
                    Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) { /* Fressen */ }
    }

    public static File showBrowseDirectoryDialog() {
        switch (env.getOperatingSystem()) {
            case Linux:
                return showBrowseDialogLinux(BrowseType.DIRECTORIES_ONLY);

            case Mac:
                return showBrowseDialogMac(BrowseType.DIRECTORIES_ONLY);

            case Windows:
            default:
                return showBrowseDialogDefault(JFileChooser.DIRECTORIES_ONLY);
        }
    }

    public static File showBrowseFileDialog() {
        switch (env.getOperatingSystem()) {
            case Linux:
                return showBrowseDialogLinux(BrowseType.FILES_ONLY);

            case Mac:
                return showBrowseDialogMac(BrowseType.FILES_ONLY);

            case Windows:
            default:
                return showBrowseDialogDefault(JFileChooser.FILES_ONLY);
        }
    }

    private static File showBrowseDialogLinux(BrowseType type) {
        LinuxNativeClient nativeClient = LinuxNativeClient.getInstance();

        try {
            nativeClient.init("Everything is up to date.");
            Object responseObj = nativeClient.send(new BrowseFileRequest(type));

            // If responseObj returns null means that there is a problem
            // trying to connect with native linux socket.
            if (responseObj == null) {
                //return null;
                // Throw the exception to show the java native browser.
                throw new Exception();
            }

            return (File) responseObj;
        } catch (Exception e) {
            if (type == BrowseType.FILES_ONLY) {
                return showBrowseDialogDefault(JFileChooser.FILES_ONLY);
            } else if (type == BrowseType.DIRECTORIES_ONLY) {
                return showBrowseDialogDefault(JFileChooser.DIRECTORIES_ONLY);
            }

            throw new RuntimeException("Unknown browse type!");
        }
    }

    private static File showBrowseDialogMac(BrowseType type) {
        // AWT looks best on Mac:
        // http://today.java.net/pub/a/today/2004/01/29/swing.html

        String title = "";
        if (type == BrowseType.DIRECTORIES_ONLY) {
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            title = "Choose Folder";
        } else if (type == BrowseType.FILES_ONLY) {
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
            title = "Choose File";
        }

        FileDialog dialog = new FileDialog(new Frame(), title);
        dialog.setVisible(true);

        String path = dialog.getDirectory() + dialog.getFile();
        return new File(path);
    }

    private static File showBrowseDialogDefault(int jFileChooserFileSelectionMode) {
        JFileChooser fc = new JFileChooser();

        fc.setFileSelectionMode(jFileChooserFileSelectionMode);

        if (fc.showDialog(null, "Select") != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        return fc.getSelectedFile();
    }  
    
    public static String getFilePathCleaned(String path){
        if(env.getOperatingSystem() == Environment.OperatingSystem.Windows){
            if(path.contains("\\")){
                path = path.replace("\\", "/");
            }
        }
        
        if(path.isEmpty() || !path.startsWith("/")){
            path = "/" + path;
        }
        
        return path;
    }
        
    
    public static void createWindowsLink(String link, String target){
        // TODO Is it always working fine???
        File FileLink = new File(link);
        if (!FileLink.exists()) {
            String script = "Set sh = CreateObject(\"WScript.Shell\")"
                    + "\nSet shortcut = sh.CreateShortcut(\"" + link + "\")"
                    + "\nshortcut.TargetPath = \"" + target + "\""
                    + "\nshortcut.Save";

            File file = new File(env.getDefaultUserConfigDir() + File.separator + "temp.vbs");
            try {
                FileOutputStream fo = new FileOutputStream(file);
                fo.write(script.getBytes());
                fo.flush();
                fo.close();
                
                Runtime.getRuntime().exec("wscript.exe \"" + file.getAbsolutePath() + "\"");
            } catch (IOException ex) {
                logger.error(ex);
                RemoteLogs.getInstance().sendLog(ex);
            }
        }
    }
    
    public static boolean checkIgnoreFile(Folder root, File file){    
        String fileNameLower = file.getName().toLowerCase();
        
        /* This case is a special one. It will check if the file
           has the structure .nw_111_aaa meaning it is a new wp. */
        if (isSpecialFileToProcess(root, file)) {
            return false;
        }
        
        if (isStackSyncTemporalFile(root, file)) {
            return true;
        }
        
        //.ds_store
        if (fileNameLower.compareTo(Constants.FILE_IGNORE_MAC_PREFIX) == 0) {
            return true;
        }
        
        //.thumbs.db
        if (fileNameLower.compareTo("thumbs.db") == 0) {
            return true;
        }        
        
        // gedit temporal file
        if (fileNameLower.endsWith("~")){
            return true;
        }
        
        // .file linux hidden file
        if (fileNameLower.startsWith(".")){
            return true;
        }
        
        File parent = file.getParentFile();
        if(parent != null && parent.getAbsolutePath().compareTo(root.getLocalFile().getAbsolutePath()) != 0 &&
                             parent.getAbsolutePath().length() > root.getLocalFile().getAbsolutePath().length()){            
            return checkIgnoreFile(root, parent);
        }
        
        return false;
    }
    
    public static boolean isStackSyncTemporalFile(Folder root, File file) {
        String fileNameLower = file.getName().toLowerCase();
        
        // .ignore file
        if (fileNameLower.startsWith(Constants.FILE_IGNORE_PREFIX)) {
            return true;
        }
        
        return false;
    }
    
    public static boolean isSpecialFileToProcess(Folder root, File file) {
        String fileNameLower = file.getName().toLowerCase();
        
        // .nw_111_aaa file means: folder name = aaa, workspace id = 111
        if (fileNameLower.startsWith(".nw_")) {
            return true;
        }
        
        return false;
    }
    
    
    public static String getMimeType(File file){
        FileInputStream fis = null;
        try {
            String mimetype = null;
            
            fis = new FileInputStream(file);
            byte[] bytes = IOUtils.toByteArray(fis);
            
            if(mimeTypesMap != null){
                try {            
                    mimetype = mimeTypesMap.identify(bytes);        
                } catch (Exception ex){ }        
            }
            
            if(mimetype == null){
                mimetype = "unknown";
            }
            
            return mimetype;
        } catch (IOException ex) {
            return "unknown";
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) { }
            }
        }
    }
    
    public static String getPropertyFromManifest(String manifestPath, String property){
        try {
            URLClassLoader cl = (URLClassLoader) FileUtil.class.getClassLoader();
            
            URL url = cl.findResource(manifestPath);
            Manifest manifest = new Manifest(url.openStream());
            Attributes attr = manifest.getMainAttributes();
            
            return attr.getValue(property);            
        } catch (IOException ex) {
            logger.debug("Exception: ", ex);
            return null;
        }
    }
    
    public static boolean checkIllegalName(String filename) {
        Pattern pattern = Pattern.compile("[\\\\/:*<>|\"?]");
        Matcher matcher = pattern.matcher(filename);
        return matcher.find();
    }
}
