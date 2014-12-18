/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import com.stacksync.desktop.util.StringUtil;

public class RegisterOverlays {

    private static final CommandLineParser parser = new PosixParser();
    private static final String[] strDlls = {"StackSyncSyncingOverlay_x", "StackSyncUptodateOverlay_x", "StackSyncErrorOverlay_x"};
      
    //java -cp myJar.jar mypackage.myclass
    //java -cp dist\stacksync.jar com.stacksync.desktop.RegisterOverlays --install --path C:\
    private static Environment.OperatingSystem getOperatingSystem() throws RuntimeException {
        Environment.OperatingSystem operatingSystem = Environment.OperatingSystem.Linux;

        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            operatingSystem = Environment.OperatingSystem.Linux;
        } else if (osName.contains("windows")) {
            operatingSystem = Environment.OperatingSystem.Windows;
        } else if (osName.contains("mac os x")) {
            operatingSystem = Environment.OperatingSystem.Mac;
        } else {
            throw new RuntimeException("Your system is not supported at the moment: " + System.getProperty("os.name"));
        }

        return operatingSystem;
    }

    private static void executeCommand(String strCommand, boolean wait) throws IOException, InterruptedException {
        System.out.println("Command --> " + strCommand);
        
        Process p = Runtime.getRuntime().exec(strCommand);
        if (wait) {
            p.waitFor();
            p.destroy();
        } else {
            Thread.sleep(2000);
            p.destroy();
        }
    }

    private static void restartExplorer() throws IOException {        
        try {
            //C:\Windows\System32\taskkill.exe /f /im explorer.exe
            File command = new File(System.getenv("windir") + "\\System32\\taskkill.exe");
            String strCommand = command.toURI().toURL().getFile() + " /f /im explorer.exe";
            executeCommand(strCommand, false);

            //start C:\Windows\explorer.exe
            strCommand = "cmd.exe /c start " + System.getenv("windir") + "\\explorer.exe";
            //strCommand = "C:\\Windows\\explorer.exe";
            executeCommand(strCommand, false);
        } catch (InterruptedException ex) {
            throw new IOException(ex.getCause());
        }        
    }
    
    private static void restartNautilus() throws IOException {
        try {            
            String strCommand = "killall nautilus";
            executeCommand(strCommand, false);
        } catch (InterruptedException ex) {
            throw new IOException(ex.getCause());
        }
    }
    
    private static File getRegisterCommand(){
        File command = new File(System.getenv("windir") + "\\System32\\regsvr32.exe");
                      //new File(System.getenv("windir") + "\\SysWOW64\\regsvr32.exe");
        
        return command;
    }
    
    private static String getBits(){
        String bits = "86";
        File command = new File(System.getenv("windir") + "\\SysWOW64\\regsvr32.exe");
        if(command.exists()){
            bits = "64";
        }
        
        return bits;
    }

    private static void registerWindowsDll(File fileDll) throws MalformedURLException, IOException, InterruptedException {
        File command = getRegisterCommand();
        String strCommand = command.getAbsolutePath() + " /s \"" + fileDll.getAbsolutePath() + "\"";
        executeCommand(strCommand, true);
    }
    
    private static void registerWindowsOverlays(File filePath) throws IOException {
        System.out.println("Register windows overlays...");
        
        try {
            String bits = getBits();            
            for(String dll: strDlls){
                registerWindowsDll(new File(filePath.getAbsolutePath() + "\\dlls\\" + dll + bits + ".dll"));
            }
            
            //restartExplorer();
        } catch (InterruptedException ex) {
            throw new IOException(ex.getCause());
        }
    }

    private static void unRegisterWindowsDll(File fileDll) throws MalformedURLException, IOException, InterruptedException {
        File command = getRegisterCommand();
        String strCommand = command.getAbsolutePath() + " /s /u \"" + fileDll.getAbsolutePath() + "\"";
        executeCommand(strCommand, true);
    }

    private static void unRegisterWindowsOverlays(File filePath) throws IOException {
        System.out.println("Unregister windows overlays...");
        try {
            String bits = getBits();
            for(String dll: strDlls){
                unRegisterWindowsDll(new File(filePath.getAbsolutePath() + "\\dlls\\" + dll + bits + ".dll"));
            }

            //restartExplorer();
        } catch (InterruptedException ex) {
            throw new IOException(ex.getCause());
        }
    }

    private static void registerLinuxOverlays(File filePath) throws IOException {
        
        try{            
            File source = new File(filePath.getAbsoluteFile() + File.separator + "dlls/libnautilus-syncany.so");

            File target1 = new File("/usr/lib/nautilus/extensions-2.0/libnautilus-syncany.so");
            File target2 = new File("/usr/lib/nautilus/extensions-3.0/libnautilus-syncany.so");
            
            if(!target1.exists()){
                FileUtils.copyFile(source, target1);            
            }

            executeCommand("chown root:root " + target1.getAbsolutePath(), true);
            executeCommand("chmod 644 " + target1.getAbsolutePath(), true);
            
            if(!target2.exists()){
                executeCommand("ln -s " + target1.getAbsolutePath() + " " + target2.getAbsolutePath(), true);                        
            }

            executeCommand("chown root:root " + target2.getAbsolutePath(), true);
            executeCommand("chmod 644 " + target2.getAbsolutePath(), true);        

            restartNautilus(); 
        } catch (InterruptedException ex) {
            throw new IOException(ex.getCause());
        }
    }

    private static void unRegisterLinuxOverlays(File filePath) throws IOException {
        
        try{
            File source = new File(filePath.getAbsoluteFile() + File.separator + "dlls/libnautilus-syncany.so");

            File target1 = new File("/usr/lib/nautilus/extensions-2.0/libnautilus-syncany.so");
            File target2 = new File("/usr/lib/nautilus/extensions-3.0/libnautilus-syncany.so");
            
            if(target2.exists()){
                executeCommand("rm " + target2.getAbsolutePath(), true);            
            }            
            
            if(target1.exists()){
                executeCommand("rm " + target1.getAbsolutePath(), true);            
            }

        } catch (InterruptedException ex) {
            throw new IOException(ex.getCause());
        }
    }

    private static void registerOverlays(final File filePath) throws UnsupportedOperationException, IOException {
        System.out.println("Installing overlays...");

        switch (getOperatingSystem()) {
            case Windows:                
                registerWindowsOverlays(filePath);
                break;
            case Linux:
                registerLinuxOverlays(filePath);
                break;
            default:
                throw new UnsupportedOperationException("Not supported yet.");
        }

        System.out.println("Done...");
    }

    private static void unRegisterOverlays(File filePath) throws IOException {
        System.out.println("Uninstalling overlays...");

        switch (getOperatingSystem()) {
            case Windows:
                unRegisterWindowsOverlays(filePath);
                break;
            case Linux:
                unRegisterLinuxOverlays(filePath);
                break;
            default:
                throw new UnsupportedOperationException("Not supported yet.");
        }

        System.out.println("Done...");
    }

    private static void showHelp(Options options) {
        HelpFormatter h = new HelpFormatter();
        h.printHelp("java -cp Stacksync.jar com.stacksync.desktop.RegisterOverlays [options] --path AppPath", options);
        System.exit(0);
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption("i", "install", false, "Register dlls in the system.");
        options.addOption("u", "uninstall", false, "Unregister dlls in the system.");
        options.addOption("p", "path", true, "Application path.");
        options.addOption("h", "help", false, "Print this message.");

        return options;
    }

    public static void main(String[] args) {
        try {
            // create the command line parser
            Options options = createOptions();
            CommandLine line = parser.parse(options, args);

            // Help
            if (line.hasOption("help")) {
                showHelp(options);
            } else {
                if (!line.hasOption("path")) {
                    throw new ParseException("Path is obligatory.");
                }

                String path = line.getOptionValue("path");
                File filePath = new File(path);

                if (!filePath.exists()) {
                    throw new IOException("Path doesn't exist.");
                }

                if (line.hasOption("install")) {
                    registerOverlays(filePath);
                } else if (line.hasOption("uninstall")) {
                    unRegisterOverlays(filePath);
                }
            }

        } catch (ParseException ex) {
            System.err.println("ERROR: Command line arguments invalid: ");
            System.err.println(StringUtil.getStackTrace(ex));
            System.exit(1);
        } catch (UnsupportedOperationException ex) {
            System.err.println("ERROR: Unsupported Operating system: ");
            System.err.println(StringUtil.getStackTrace(ex));
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("ERROR: ");
            System.err.println(StringUtil.getStackTrace(ex));
            System.exit(1);
        }
    }
}
