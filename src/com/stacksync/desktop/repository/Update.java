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
package com.stacksync.desktop.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.db.models.Workspace;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Update {

    private String mimeType;
    private String clientName; // This is just a helper field, NOT saved in the update file!
    private long fileId;
    private long version;
    private long parentFileId;
    private long parentFileVersion;

    private Date updated;
    private Status status;
    private Date lastModified;
    private long checksum;
    private long fileSize;
    private boolean folder;
    private String name;
    private String path;
    
    private String updateFilePath;
    private Workspace workspace;
    
    private boolean serverUploaded;        
    private boolean serverUploadedAck;    
    private Date serverUploadedTime;
    private boolean conflicted;
    
    // chunkIds (checksums) 
    private List<String> chunks;   

    public Update() {
        // Fressen
        updateFilePath = "";
        
        serverUploaded = false;
        serverUploadedAck = false;
        serverUploadedTime = null;
        conflicted = false;
    }
    
    public boolean getConflicted(){
        return conflicted;
    }
    
    public void setConflicted(boolean conflicted){
        this.conflicted = conflicted;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(long fileId) {
        this.fileId = fileId;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isFolder() {
        return folder;
    }

    public void setFolder(boolean folder) {
        this.folder = folder;
    }

    public Status getStatus() {
        return status;
    }    

    public void setStatus(Status status) {
        this.status = status;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public List<String> getChunks() {
        return chunks;
    }

    public void setChunks(List<String> chunks) {
        this.chunks = chunks;
    }

    public long getParentFileId() {
        return parentFileId;
    }

    public void setParentFileId(long parentFileId) {
        this.parentFileId = parentFileId;
    }

    public long getParentFileVersion() {
        return parentFileVersion;
    }

    public void setParentFileVersion(long parentFileVersion) {
        this.parentFileVersion = parentFileVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Update other = (Update) obj;
        if (this.fileId != other.fileId) {
            return false;
        }
        if (this.version != other.version) {
            return false;
        }
        if (this.checksum != other.checksum) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (int) (this.fileId ^ (this.fileId >>> 32));
        hash = 79 * hash + (int) (this.version ^ (this.version >>> 32));
        hash = 79 * hash + (int) (this.checksum ^ (this.checksum >>> 32));
        return hash;
    }
    
    @Override
    public String toString() {
        String strPath = getPath();
        if(!strPath.endsWith("/")){
            strPath = strPath + "/";
        }
        strPath = strPath + getName();
        
        return "Update[fileId=" + getFileId() + ", version=" + getVersion() + ", status=" + getStatus() + ", file=" + strPath + ", updatePath = " + updateFilePath + "]";
    }
    
    public void setUpdateFilePath(String path){
        this.updateFilePath = path;
    }
    
    public void setServerUploaded(boolean serverUploaded){
        this.serverUploaded = serverUploaded;
    }
    
    public void setServerUploadedAck(boolean serverUploadedAck){
        this.serverUploadedAck = serverUploadedAck;
    }
    
    public void setServerUploadedTime(Date serverUploadedTime){
        this.serverUploadedTime = serverUploadedTime;
    }
    
    public boolean getServerUploaded(){
        return this.serverUploaded;
    }
    
    public boolean getServerUploadedAck(){
        return this.serverUploadedAck;
    }
    
    public Date getServerUploadedTime(){
        return this.serverUploadedTime;
    }
    
    public String getUpdateFilePath(){
        return updateFilePath;
    }

    public void setWorkpace(Workspace workspace) {
        this.workspace = workspace;
    }
    
    public Workspace getWorkspace(){
        return this.workspace;                
    }
    
    public String getMimeType(){
        return mimeType;
    }
    
    public void setMimeType(String mimeType){
        this.mimeType = mimeType;
    }
    
    public static Update parse(CloneFile cf){
        
        Update update = new Update();
        
        update.setClientName(cf.getClientName());
        update.setFileId(cf.getFileId());
        update.setVersion(cf.getVersion());
        
        if(cf.getParent() != null){
            update.setParentFileId(cf.getParent().getFileId());
            update.setParentFileVersion(cf.getParent().getVersion());
        }
        
        update.setUpdated(cf.getUpdated());
        update.setStatus(cf.getStatus());
        
        update.setLastModified(cf.getLastModified());
        update.setChecksum(cf.getChecksum());
        
        update.setFileSize(cf.getFileSize());
        update.setFolder(cf.isFolder());
        update.setName(cf.getName());
        update.setPath(cf.getPath());
        update.setMimeType(cf.getMimetype());
        
        List<String> chunksAdded = new ArrayList<String>();
        for(CloneChunk chunk: cf.getChunks()){
            chunksAdded.add(chunk.getChecksum());
        }
                
        update.setChunks(chunksAdded);
        update.setServerUploadedAck(cf.getServerUploadedAck());
        
        return update;
    }
    
}
