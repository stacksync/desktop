package com.stacksync.desktop.syncserver;

import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;
import java.util.LinkedList;
import java.util.List;

public class TempIdManager {
    
    
    public TempIdManager() {
    }
    
    public CloneFile changeTempId(CloneFile oldFile, Long newId) {
        
        CloneFile fileWithNewId;
        
        fileWithNewId = (CloneFile)oldFile.clone();
        fileWithNewId.setServerUploadedAck(oldFile.getServerUploadedAck());
        fileWithNewId.setServerUploadedTime(oldFile.getServerUploadedTime());
        
        //newFile.setChunks(localFile.getChunks());
        fileWithNewId.setId(newId);
        fileWithNewId.setUsingTempId(false);
        fileWithNewId.merge();
        
        DatabaseHelper.getInstance().updateParentId(fileWithNewId, oldFile);
        oldFile.deleteFromDB();
        
        return fileWithNewId;
    }
    
    public List<CloneFile> changeTempIdFromUncommitedItems(List<CloneFile> oldFiles, Long newId) {
        
        LinkedList<CloneFile> newFiles = new LinkedList<CloneFile>();
        
        for (CloneFile oldFile : oldFiles) {
            CloneFile fileWithNewId;
        
            fileWithNewId = (CloneFile)oldFile.clone();

            //newFile.setChunks(localFile.getChunks());
            fileWithNewId.setId(newId);
            fileWithNewId.setUsingTempId(false);
            fileWithNewId.merge();

            DatabaseHelper.getInstance().updateParentId(fileWithNewId, oldFile);
            oldFile.deleteFromDB();
            newFiles.add(fileWithNewId);
        }
        
        return newFiles;
    }
    
}
