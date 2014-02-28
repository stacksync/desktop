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

import com.stacksync.commons.models.ItemMetadata;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.db.models.CloneWorkspace;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class Update {

    private String mimeType;
    private long fileId;
    private long version;
    private Long parentFileId;
    private Long parentFileVersion;

    private Status status;
    private Date modifiedAt;
    private long checksum;
    private long fileSize;
    private boolean folder;
    private String name;
    
    private CloneWorkspace workspace;
    
    private boolean serverUploaded;        
    private boolean serverUploadedAck;    
    private Date serverUploadedTime;
    private boolean conflicted;
    
    // chunkIds (checksums) 
    private List<String> chunks;   

    public Update() {
        
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

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<String> getChunks() {
        return chunks;
    }

    public void setChunks(List<String> chunks) {
        this.chunks = chunks;
    }

    public Long getParentFileId() {
        return parentFileId;
    }

    public void setParentFileId(Long parentFileId) {
        this.parentFileId = parentFileId;
    }

    public Long getParentFileVersion() {
        return parentFileVersion;
    }

    public void setParentFileVersion(Long parentFileVersion) {
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
        return "Update[fileId=" + getFileId() + ", version=" + getVersion() + ", status=" + getStatus() + "]";
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

    public void setWorkpace(CloneWorkspace workspace) {
        this.workspace = workspace;
    }
    
    public CloneWorkspace getWorkspace(){
        return this.workspace;                
    }
    
    public String getMimeType(){
        return mimeType;
    }
    
    public void setMimeType(String mimeType){
        this.mimeType = mimeType;
    }
    
    public static Update parse(ItemMetadata itemMetadata, CloneWorkspace workspace) 
        throws NullPointerException {
        
        Update update = new Update();

        update.setServerUploaded(true);
        update.setServerUploadedAck(true);
        update.setServerUploadedTime(new Date());

        update.setFileId(itemMetadata.getId());
        update.setVersion(itemMetadata.getVersion());
        
        update.setModifiedAt(itemMetadata.getModifiedAt());
        update.setStatus(CloneFile.Status.valueOf(itemMetadata.getStatus()));
        update.setChecksum(itemMetadata.getChecksum());
        update.setMimeType(itemMetadata.getMimetype());
        update.setFileSize(itemMetadata.getSize());
        update.setFolder(itemMetadata.isFolder());

        update.setName(itemMetadata.getFilename());

        // Parent
        if (itemMetadata.getParentId() != null && !itemMetadata.getParentId().toString().isEmpty()) {
            update.setParentFileId(itemMetadata.getParentId());
            if (itemMetadata.getParentVersion() != null) {
                update.setParentFileVersion(itemMetadata.getParentVersion());
            }
        }

        update.setChunks(itemMetadata.getChunks());
        update.setWorkpace(workspace);

        return update;
        
    }
    
}
