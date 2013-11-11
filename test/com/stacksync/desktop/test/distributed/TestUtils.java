/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.test.distributed;

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.sftp.SftpFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author Marc
 */
public class TestUtils {
    
    private  List<Computer> computerList = new LinkedList<Computer>();
	private  String stacksyncFolderPath = "";
	private  Logger logg = Logger.getLogger(TestUtils.class.getName());
	private  String hostHomeFolderPath = System.getProperty( "user.home" );
	private  int sleepTime = 20000;
	private  int syncRechkTime = 5000;
	private  String stacksyncFolder = "stacksync_folder";
	private  String testFolder = "testFolder";
	private static TestUtils testInstance;
    private ConsoleAppender log = new ConsoleAppender(new PatternLayout("$$$$%d{yyyy-MM-dd HH:mm:ss}#%p#[%t]#%c->%M#%m#%n"));
    
    private TestUtils(){
        
        computerList = new LinkedList<Computer>();
		BasicConfigurator.configure();
	    Logger logger = Logger.getRootLogger();
	    logger.setLevel(Level.WARN);
	    logger.removeAllAppenders();
	    if(!logg.isAttached(log)){
            logg.addAppender(log);    
        }
	    File folder = new File(testFolder);
	    folder.mkdir();
	    try {
			String line = "";
			BufferedReader in = new BufferedReader(new FileReader("test/org/stacksync/test/distributed/Computers.txt"));
			stacksyncFolderPath = in.readLine();
			while ((line = in.readLine()) != null) {
				StringTokenizer stk = new StringTokenizer(line, ",");
				computerList.add(new Computer(stk.nextToken(), stk.nextToken(),	stk.nextToken()));
			}
			in.close();
			
		} catch (IOException e) {
			logg.error("Failed to set up the tests");
			System.exit(0);
		}
    }
    
    public ConsoleAppender getConsoleAppender(){
        return log;
    }    
    
    public int getSleepTime(){
        return sleepTime;
    }
    
    public List<Computer> getComputerList(){
        return computerList;
    }
    
    public String getStacksyncFolderPath(){
        return stacksyncFolderPath;
    }
    
    public String getHostHomeFolderPath(){
      return hostHomeFolderPath; 
    }
    
    public int getSyncRechkTime(){
        return syncRechkTime;
    }
    
    public String getTestFolder(){
        return testFolder;
    }
    
    public static TestUtils getUtils(){
        
        if (testInstance == null){
           testInstance = new TestUtils();
        }
        return testInstance;
       
    }
    
	public void waitSync(String path, String ip) {

		String response = "";
		String wanted = "ok\nemblems\tuptodate\ndone\n";

		while (!(response.trim().equalsIgnoreCase(wanted.trim()))) {
			try {

				byte[] data = new byte[1024];
				Socket sock = null;

				sock = new Socket(ip, 32586);
				InputStream sockInput = sock.getInputStream();
				OutputStream sockOutput = sock.getOutputStream();

				String Message = "get_emblem_paths\ndone\n";
				sockOutput.write(Message.getBytes(), 0,	Message.getBytes().length);
				sockInput.read(data);
				response = new String(data);
				Message = "get_emblems\npath\t" + path + "\ndone\n";
				sockOutput.write(Message.getBytes(), 0,	Message.getBytes().length);

				data = new byte[1024];
				sockInput.read(data);
				response = new String(data);

				Thread.sleep(syncRechkTime);
			} catch(InterruptedException e){
				logg.error("Failed to sleep!");
				System.exit(0);
			}catch(UnknownHostException e){
				logg.error("Stacksync not running!!");
				System.exit(0);
			}catch(IOException e){
				logg.error("Can't comunicate with stacksync!!");
				System.exit(0);
			}
			
		}
	}

	public boolean checkRemoteChecksum(String remoteFilePath,String remoteIP, String remoteUser, String remotePass) {
	
		try{
			SshClient ssh = new SshClient();
			ssh.connect(remoteIP, 22,new AlwaysAllowingConsoleKnownHostsKeyVerification());

			PasswordAuthenticationClient passwordAuthenticationClient = new PasswordAuthenticationClient();
			passwordAuthenticationClient.setUsername(remoteUser);
			passwordAuthenticationClient.setPassword(remotePass);
			ssh.authenticate(passwordAuthenticationClient);
			SftpClient client = ssh.openSftpClient();

			FileOutputStream fos = new FileOutputStream(testFolder+"/result");
			client.cd(stacksyncFolder);
			client.get(remoteFilePath, fos);

			FileInputStream fis = new FileInputStream(new File(testFolder+"/result"));
			String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
			fis = new FileInputStream(new File(stacksyncFolderPath+remoteFilePath));
			String md52 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);

			fis.close();
			fos.close();

			return (md5.equals(md52));
		}catch(IOException e){
			logg.error("Failed to check remote checksum");
			return false;
		}
	}
	
    public void copyFolder(File src, File dest)throws IOException{
        
    	if(src.isDirectory()){
       		if(!dest.exists()){
    		   dest.mkdir();
    		}
       		String files[] = src.list();
       		
    		for (String file : files) {
    		   File srcFile = new File(src, file);
    		   File destFile = new File(dest, file);
    		   copyFolder(srcFile,destFile);
    		}    
    	}else{       		
            OutputStream out = new FileOutputStream(dest);
            byte[] fileByteArray = org.apache.commons.io.FileUtils.readFileToByteArray(src);
            out.write(fileByteArray);
            out.close();
		}
    }

	public boolean checkRemoteFileDelete(String fileNamePath, String remoteIP, String remoteUser,String remotePass){
    	boolean itsok = true;
		
			SshClient ssh = new SshClient();
			try {
				ssh.connect(remoteIP, 22,new AlwaysAllowingConsoleKnownHostsKeyVerification());
			} catch (Exception e) {
				logg.error("Failed to connect to server:"+remoteIP);
				return false;
			}
			PasswordAuthenticationClient passwordAuthenticationClient = new PasswordAuthenticationClient();
			passwordAuthenticationClient.setUsername(remoteUser);
			passwordAuthenticationClient.setPassword(remotePass);
			
			try {
				ssh.authenticate(passwordAuthenticationClient);
				SftpClient client = ssh.openSftpClient();
				FileOutputStream fos = new FileOutputStream(testFolder+"/result");
				client.get(stacksyncFolder+"/"+fileNamePath,fos);
			} catch (IOException e) {
				itsok = false;
				return true;
			}
			if (itsok == true) {
				logg.error("cheking failed");
				return false;
			}
		
		return false;
	}
    
    public void remoteFileDelete(String fileNamePath, String remoteIP, String remoteUser,String remotePass){
		try {

    	SshClient ssh = new SshClient();
		ssh.connect(remoteIP, 22,new AlwaysAllowingConsoleKnownHostsKeyVerification());

		PasswordAuthenticationClient passwordAuthenticationClient = new PasswordAuthenticationClient();
		passwordAuthenticationClient.setUsername(remoteUser);
		passwordAuthenticationClient.setPassword(remotePass);
		ssh.authenticate(passwordAuthenticationClient);
		SftpClient client = ssh.openSftpClient();
		client.rm(stacksyncFolder+"/"+ fileNamePath);
		} catch (IOException e) {
		}

    }
    
	public String generateConflict(String simpleFileName, String remoteIP, String remoteUser,String remotePass, int compNumber) {
		try {
			File testFile = new File(hostHomeFolderPath+"/"+simpleFileName);
			FileOutputStream fis = new FileOutputStream(testFile);
			String testFileText = "Testing" + compNumber;
			fis.write(testFileText.getBytes());
			fis.close();
			
			SshClient ssh = new SshClient();
			ssh.connect(remoteIP, 22,new AlwaysAllowingConsoleKnownHostsKeyVerification());
			PasswordAuthenticationClient passwordAuthenticationClient = new PasswordAuthenticationClient();
			passwordAuthenticationClient.setUsername(remoteUser);
			passwordAuthenticationClient.setPassword(remotePass);
			ssh.authenticate(passwordAuthenticationClient);

			SftpClient client = ssh.openSftpClient();
			client.cd(stacksyncFolder);
			client.put(simpleFileName);
			ssh.disconnect();
			client.quit();

			FileInputStream fileOut = new FileInputStream(hostHomeFolderPath+"/"+simpleFileName);
			String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileOut);
			return md5;
		} catch (IOException e) {
			logg.error("Failed to generate conflict due to comunication errors");
			return null;

		}
	}
	
	public boolean checkMd5List(String fileName,List<String> md5List,String remoteIP, String remoteUser,String remotePass) {
		try{
			List<String> newMd5List = new LinkedList<String>();
			SshClient ssh = new SshClient();
			ssh.connect(remoteIP, 22,new AlwaysAllowingConsoleKnownHostsKeyVerification());
			PasswordAuthenticationClient passwordAuthenticationClient = new PasswordAuthenticationClient();
			passwordAuthenticationClient.setUsername(remoteUser);
			passwordAuthenticationClient.setPassword(remotePass);
			ssh.authenticate(passwordAuthenticationClient);	
			
			SftpClient client = ssh.openSftpClient();
			client.cd(stacksyncFolder);
			@SuppressWarnings("unchecked")
			List<SftpFile> ls = client.ls();
			
			for (SftpFile file :ls){
				if ((!file.isDirectory())&& (file.getFilename().contains(fileName))) {
					String filename = file.getFilename();
					FileOutputStream fos = new FileOutputStream(testFolder+"/"+filename);
					client.get(filename, fos);
					FileInputStream fis = new FileInputStream(new File(testFolder+"/"+filename));
					String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
					newMd5List.add(md5);
				}
			}
			ssh.disconnect();
			client.quit();
			return compareStringLists(newMd5List, md5List);
				
			

		}catch(IOException e){
			logg.error("Failed to get md5 remotelist");
			return false;
		}

	}
	
	public boolean compareStringLists(List<String>newMd5List, List<String>md5List){
		if(newMd5List.size()!= md5List.size()){
			return false;
		}else{
			boolean notFound = false;
			int listIndex = 0;
			while((!notFound)&&(listIndex<newMd5List.size())){
				String currMd5 = md5List.get(listIndex);
				notFound = true;
				for (String curMd5 : newMd5List){
					if(curMd5.equals(currMd5)){
						notFound = false;
						break;
					}
				}
				listIndex++;
			}
			if(notFound){
				return false;
			}else{
				return true;
			}
		}
	}

    public String createNewFile(String fileName, String Content){
		String testFileName = fileName;
		File testfile = new File(stacksyncFolderPath + testFileName);
		try {
			FileOutputStream fis = new FileOutputStream(testfile);
			String testFileText = Content;
			fis.write(testFileText.getBytes());
			fis.close();
		} catch (IOException e) {
			logg.error("failed to make new file");
			return null;
		}
		return testFileName;
    }
    
	public boolean checkRemoteFolderChecksum(String remoteFilePath,String remoteIP, String remoteUser, String remotePass) {
		try {

			File testFolderr = new File(testFolder+"/"+remoteFilePath);
			testFolderr.mkdir();

			SshClient ssh = new SshClient();
			ssh.connect(remoteIP, 22,new AlwaysAllowingConsoleKnownHostsKeyVerification());

			PasswordAuthenticationClient passwordAuthenticationClient = new PasswordAuthenticationClient();
			passwordAuthenticationClient.setUsername(remoteUser);
			passwordAuthenticationClient.setPassword(remotePass);
			ssh.authenticate(passwordAuthenticationClient);
			SftpClient client = ssh.openSftpClient();

			client.cd(stacksyncFolder+"/"+remoteFilePath);
			@SuppressWarnings("unchecked")
			List<SftpFile> ls = client.ls();
			for (SftpFile file : ls) {

				if (!file.isDirectory()) {
					String filename = file.getFilename();
					FileOutputStream fos = new FileOutputStream(testFolder+"/"+filename);
					client.get(filename, fos);
					
					FileInputStream fis = new FileInputStream(new File(testFolder+"/"+filename));
					String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
					fis = new FileInputStream(new File(stacksyncFolderPath+"/" +remoteFilePath+"/"+filename));
					String md52 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
					
					fis.close();
					fos.close();
					if (!(md5.equals(md52))) {
						return false;
					}
				}
			}
			return (true);

		} catch (IOException e) {
			logg.error("Failed to check remote checksum");
			return false;
		}
	}
    public void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) { // some JVMs return null for empty dirs
			for (File f : files) {
				if (f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}
    
     public String createNewFolder(String foldername,String filContent1, String filContent2, String fileName1, String fileName2){
		String testFolderName = foldername;
		File testFolder1 = new File(stacksyncFolderPath + testFolderName);
		testFolder1.mkdir();
		File testFile1 = new File(stacksyncFolderPath + testFolderName+"/"+fileName1);
		File testFile2 = new File(stacksyncFolderPath + testFolderName+"/"+fileName2);
		
		try {
			FileOutputStream fis = new FileOutputStream(testFile1);
			String testFileText = filContent1;
			fis.write(testFileText.getBytes());
			fis.close();
			
			fis = new FileOutputStream(testFile2);
			testFileText = filContent2;
			fis.write(testFileText.getBytes());
			fis.close();
			
		} catch (IOException e) {
			logg.error("failed to make new folder");
			return null;

		}
		return testFolderName;
    }
     public boolean createFolderLevels(int levels, String namePattern){
    	int i = 0;
    	String foldername = "";
    	for (i = 0; i<levels; i++){
    		 foldername = createNewFolder(foldername+"/"+namePattern+"level"+i,"testfile1","testfile2","file1.txt","file2.txt");
 			if(foldername == null){
				return false;
			}	
    		 waitSync(stacksyncFolderPath + foldername,"localhost");
    		logg.warn("Folder"+i+" created");
    	}
    	return true;
    }
}
