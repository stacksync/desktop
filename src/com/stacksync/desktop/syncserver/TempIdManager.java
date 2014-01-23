package com.stacksync.desktop.syncserver;

import com.stacksync.desktop.db.DatabaseHelper;
import com.stacksync.desktop.db.models.CloneFile;

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
        fileWithNewId.merge();
        
        DatabaseHelper.getInstance().updateParentId(fileWithNewId, oldFile);
        oldFile.deleteFromDB();
        
        return fileWithNewId;
    }
    
}
