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
package com.stacksync.desktop.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import com.stacksync.desktop.Constants;
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneChunk.CacheStatus;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneFile.Status;
import com.stacksync.desktop.db.models.CloneFile.SyncStatus;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.util.FileUtil;
import com.stacksync.desktop.util.StringUtil;

/**
 * Provides access to the database.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseHelper {
    
    private final Config config = Config.getInstance();
    private final Logger logger = Logger.getLogger(DatabaseHelper.class.getName());
    private static final DatabaseHelper instance = new DatabaseHelper();
    private int MAXTRIES = 5;
    

    private DatabaseHelper() {
        logger.debug("Creating DB helper ...");
    }

    public static DatabaseHelper getInstance() {
        return instance;
    }

    public CloneFile getFolder(Folder root, File file) {
        return getFileOrFolder(root, file, true);
    }

    public CloneFile getFile(Folder root, File file) {
        return getFileOrFolder(root, file, false);
    }

    public CloneFile getFileOrFolder(Folder root, File file) {
        return getFileOrFolder(root, file, null);
    }

    public CloneFile getFileOrFolder(File file) {
        // Get root
        Folder root = null;
        Profile profile = config.getProfile();
        
        // TODO This is terribly implemented, and veeery inefficient!
        if (profile != null) {
            
            Folder aRoot = profile.getFolder();
            
            if (aRoot != null && aRoot.getLocalFile() != null &&
                    file.getAbsolutePath().startsWith(aRoot.getLocalFile().getAbsolutePath())) {
                root = aRoot;
            }
        }

        if (root == null) {
            return null;
        }

        if(file.isDirectory()){        
            return getFolder(root, file);
        } else{
            return getFile(root, file);
        }
    }

    private CloneFile getFileOrFolder(Folder root, File file, Boolean folder) {
        assert root != null;

        // First, check by full file path
        String queryStr =
                "select f from CloneFile f where "
                + "      f.path = :path and "
                + "      f.name = :name "
                + ((folder != null) ? "and f.folder = :folder " : " ")
                + "      and f.status <> :notStatus1 "
                + "      and f.version = (select max(ff.version) from CloneFile ff where "
                + "         f.id = ff.id) "
                + "      order by f.lastModified desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        
        query.setMaxResults(1);
        
        String path = FileUtil.getRelativeParentDirectory(root.getLocalFile(), file);
        path = FileUtil.getFilePathCleaned(path);
        query.setParameter("path", path);
        query.setParameter("name", file.getName());
        query.setParameter("notStatus1", Status.DELETED);

        if (folder != null) {
            query.setParameter("folder", folder);
        }

        List<CloneFile> dbFiles = query.getResultList();

        // Error: No matching DB entries found.
        if (dbFiles.isEmpty()) {
            return null;
        } else {
            // Success
            return dbFiles.get(0);
        }
    }
            
    /*
     * get direct children
     */
    public List<CloneFile> getChildren(CloneFile parentFile) {
        // First, check by full file path
        String queryStr = "select f from CloneFile f where "
                + "      f.status <> :notStatus1 and "
                + "      f.parent = :parent and "
                + "      f.version = (select max(ff.version) from CloneFile ff where "
                + "                                         f.id = ff.id) ";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        
        query.setParameter("notStatus1", Status.DELETED);
        query.setParameter("parent", parentFile);

        return query.getResultList();
    }

    /**
     * Get file in exact version.
     *
     * @param id
     * @param version
     * @return
     */
    public CloneFile getFileOrFolder(long id, long version) {        
        for (int i = 1; i <= MAXTRIES; i++) {
            try {
                String queryStr = "select f from CloneFile f where "
                        + "      f.id = :id and "
                        + "      f.version = :version";

                Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
                query.setHint("javax.persistence.cache.storeMode", "REFRESH");
                query.setHint("eclipselink.cache-usage", "DoNotCheckCache");                
                
                query.setParameter("id", id);
                query.setParameter("version", version);

                return (CloneFile) query.getSingleResult();

            } catch (NoResultException ex) {
                logger.debug(" No result for fId->" + id + " fV-> " + version + " -> " + ex.getMessage());
                continue;
            } 
        }
        
        return null;
    }

    /**
     * Get file in current (newest) version.
     */
    public CloneFile getFileOrFolder(long id) {
        String queryStr = "select f from CloneFile f "
                + "where f.id = :id "
                + "      and f.version = (select max(ff.version) from CloneFile ff where "
                + "                                     f.id = ff.id)";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("id", id);

        try {
            return (CloneFile) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.debug(" No result -> " + ex.getMessage());
            return null;
        }
    }

    /**
     * Check the files with the same checksum and don't exist anymore to
     * determine the 'previous version' of this file.
     *
     * If more file are found, i.e. files with the same checksum that don't
     * exist, choose the one with the smallest Levenshtein distance.
     */
    public CloneFile getNearestFile(Folder root, File file, long checksum) {
        String queryStr = "select f from CloneFile f where "
                + "      f.checksum = :checksum and "
                + "      f.status <> :notStatus1 and "
                + "      f.version = (select max(ff.version) from CloneFile ff where "
                + "                                     f.id = ff.id) "
                + "      order by f.lastModified desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("notStatus1", Status.DELETED);
        query.setParameter("checksum", checksum);

        List<CloneFile> sameChecksumFiles = query.getResultList();

        CloneFile nearestPreviousVersion = null;
        int previousVersionDistance = Integer.MAX_VALUE;

        for (CloneFile cf : sameChecksumFiles) {
            // Ignore if the file actually exists
            if (cf.getFile().exists()) {
                continue;
            }

            // Check Levenshtein distance
            int distance = StringUtil.computeLevenshteinDistance(file.getAbsolutePath(), cf.getAbsolutePath());

            if (distance < previousVersionDistance) {
                nearestPreviousVersion = cf;
                previousVersionDistance = distance;
            }
        }

        // No history if the distance exceeds the maximum
        if (previousVersionDistance > Constants.MAXIMUM_FILENAME_LEVENSHTEIN_DISTANCE) {
            nearestPreviousVersion = null;
        }

        return nearestPreviousVersion;
    }

    public List<CloneFile> getFiles(Folder root) {
        String queryStr = "select f from CloneFile f where "
                + "      f.status <> :notStatus1 and"
                + "      f.version = (select max(ff.version) from CloneFile ff where "
                + "                                     f.id = ff.id) ";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("notStatus1", Status.DELETED);

        return query.getResultList();
    }

    public List<CloneFile> getFiles(Folder root, CloneFile.SyncStatus status) {
        String queryStr = "select f from CloneFile f where "
                + "      f.syncStatus = :StatusSync ";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("StatusSync", status);

        return query.getResultList();
    }

    public CloneFile createFile(Profile profile, Update update) {
        SyncStatus status = SyncStatus.REMOTE;
        return createFile(profile, update, status);
    }

    private CloneFile createFile(Profile profile, Update update, SyncStatus syncStatus) {
        CloneFile newFile = new CloneFile();
               
        newFile.setId(update.getFileId());
        newFile.setVersion(update.getVersion());
        newFile.setChecksum(update.getChecksum());
        newFile.setProfile(profile);
        newFile.setWorkspace(update.getWorkspace());
        
        newFile.setMimetype(update.getMimeType());
        newFile.setServerUploadedAck(true);
        newFile.setServerUploadedTime(update.getServerUploadedTime());        
        
        newFile.setName(update.getName());
        newFile.setLastModified(update.getModifiedAt());
        newFile.setSize(update.getFileSize());
        newFile.setStatus(update.getStatus());
        newFile.setSyncStatus(syncStatus);
        newFile.setFolder(update.isFolder());
        newFile.setUsingTempId(false);

        if (update.getParentFileId() != null) {
            CloneFile parentCF = getFileOrFolder(update.getParentFileId(), update.getParentFileVersion());
            newFile.setParent(parentCF);
        }
        newFile.generatePath();
        newFile.merge();

        // Add Chunks (if there are any!)
        // Triggered for new files (= version 1) AND for grown files (= more chunks)
        if (!update.getChunks().isEmpty()) {
            for(int i=0; i<update.getChunks().size(); i++){
                String chunkId = update.getChunks().get(i);
                CloneChunk chunk = getChunk(chunkId, CacheStatus.REMOTE);
                                                
                File chunkCacheFile = config.getCache().getCacheChunk(chunk);
                if(chunkCacheFile.exists() && chunkCacheFile.length() > 0){
                    chunk.setCacheStatus(CacheStatus.CACHED);
                }
                
                newFile.addChunk(chunk);
            }
        }

        newFile.merge();
        return newFile;
    }

    /**
     * Retrieves the last chunk/file update.
     */
    public Long getFileVersionCount() {
        Calendar cal = Calendar.getInstance();  
        cal.set(getFieldTimeout(), getValueTimeout(cal)); 
        Date time = cal.getTime();
 
        // Newest file update
        String queryStr = "select count(c.id) from CloneFile c where "
                + "     c.syncStatus = :StatusSync and "                                       
                + "     c.serverUploadedAck = false and "                
                + "     (c.serverUploadedTime < :timeNow or "
                + "     c.serverUploadedTime is null)";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, Long.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("StatusSync", CloneFile.SyncStatus.UPTODATE);
        query.setParameter("timeNow", time);
        query.setMaxResults(1);

        try {
            return (Long) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.debug("No result -> " + ex.getMessage());
            return new Long(0);
        }      
    }

    public synchronized CloneChunk getChunk(String checksum, CacheStatus status) {
        CloneChunk chunk;

        String queryStr = "select c from CloneChunk c where "
                + "     c.checksum = :checksum";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneChunk.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("checksum", checksum);

        try {
            chunk = (CloneChunk) query.getSingleResult();
            logger.info("Found chunk in DB: " + chunk);
        } catch (NoResultException e) {
            logger.info("New chunk: " + checksum);
            chunk = new CloneChunk(checksum, status);
        }

        return chunk;
    }

    public void persist(Object... objects) {
        config.getDatabase().getEntityManager().getTransaction().begin();

        for (Object o : objects) {
            config.getDatabase().getEntityManager().persist(o);
        }

        config.getDatabase().getEntityManager().flush();
        config.getDatabase().getEntityManager().clear();
        config.getDatabase().getEntityManager().getTransaction().commit();
    }

    public synchronized void merge(Object... objects) {
        config.getDatabase().getEntityManager().getTransaction().begin();

        for (Object o : objects) {
            config.getDatabase().getEntityManager().merge(o);
        }

        config.getDatabase().getEntityManager().flush();
        config.getDatabase().getEntityManager().clear();
        config.getDatabase().getEntityManager().getTransaction().commit();
    }
    
    public void remove(Object... objects) {
        config.getDatabase().getEntityManager().getTransaction().begin();

        for (Object o : objects) {
            config.getDatabase().getEntityManager().remove(o);
        }

        config.getDatabase().getEntityManager().flush();
        config.getDatabase().getEntityManager().clear();
        config.getDatabase().getEntityManager().getTransaction().commit();
    }
    
    private int getFieldTimeout(){
        return Calendar.MINUTE;
    }
    
    private int getValueTimeout(Calendar cal){
        return cal.get(Calendar.MINUTE) - 1;
    }
    
    public Map<String, List<CloneFile>> getHistoryUptoDate() {
        Calendar cal = Calendar.getInstance();  
        cal.set(getFieldTimeout(), getValueTimeout(cal)); 
        Date time = cal.getTime();        
        
        String queryStr = "select c from CloneFile c where "
                + "     c.syncStatus = :statusSync and "                
                + "     c.serverUploadedAck = false and "
                + "     c.workspaceRoot = false and "
                + "     (c.serverUploadedTime < :timeNow or "
                + "     c.serverUploadedTime is null) order by "
                + "                                     c.path asc, c.version asc";    
        
        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("statusSync", CloneFile.SyncStatus.UPTODATE);       
        query.setParameter("timeNow", time);
        
        Map<String, List<CloneFile>> workspaces = new HashMap<String, List<CloneFile>>();
        List<CloneFile> clonefiles = query.getResultList();
        for(CloneFile cf: clonefiles){
            if(!workspaces.containsKey(cf.getWorkspace().getId())){
                workspaces.put(cf.getWorkspace().getId(), new ArrayList<CloneFile>());
            } 
                
            workspaces.get(cf.getWorkspace().getId()).add(cf);            
        }
        
        return workspaces;
    }
    
    public Map<String, CloneWorkspace> getWorkspaces() {        
        String queryStr = "select w from CloneWorkspace w";        
        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneWorkspace.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        
        List<CloneWorkspace> workspaces = query.getResultList();
        Map<String, CloneWorkspace> localWorkspaces = new HashMap<String, CloneWorkspace>();
        
        for (CloneWorkspace w: workspaces){
            localWorkspaces.put(w.getId(), w);
        }
        
        return localWorkspaces;
    }

    public List<CloneChunk> getChunkCached() {
        String queryStr = "select c from CloneChunk c where "
                + "     c.status = :status ";

        Query query = config.getDatabase().createQuery(queryStr, CloneChunk.class);
        
        query.setParameter("status", CacheStatus.CACHED);        
        return query.getResultList();
    }

    public List<CloneFile> getCloneFiles(CloneChunk chunk) {
        String queryStr = "select distinct c from CloneFile c "
                + "     join c.chunks ch "
                + "     where "
                + "     ch.checksum = :checksum ";        

        Query query = config.getDatabase().createQuery(queryStr, CloneFile.class);
               
        query.setParameter("checksum", chunk.getChecksum());        
        return query.getResultList();        
    }

    public void updateParentId(CloneFile newParent, CloneFile currentParent) {
        String queryStr = "UPDATE CloneFile c set c.parent=:new_parent"
                + "     WHERE c.parent = :current_parent";

        
        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        
        query.setParameter("new_parent", newParent);
        query.setParameter("current_parent", currentParent);

        config.getDatabase().getEntityManager().getTransaction().begin();
        query.executeUpdate();
        config.getDatabase().getEntityManager().getTransaction().commit();
    }
    
    public CloneWorkspace getDefaultWorkspace() {
        
        CloneWorkspace defaultWorkspace;
        
        String queryStr = "select wp from CloneWorkspace wp where "
                + "     wp.pathWorkspace = :path";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneChunk.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        query.setParameter("path", "/");

        defaultWorkspace = (CloneWorkspace) query.getSingleResult();
        
        return defaultWorkspace;
    }

    public CloneFile getWorkspaceRoot(String id) {
        CloneFile workspaceRoot;
        
        String queryStr = "select cf from CloneFile cf where "
                + "        cf.workspaceRoot = :isRoot and"
                + "        cf.version = ("
                + "            select max(cf2.version) from CloneFile cf2 where cf.id = cf2.id) and "
                + "        cf.workspace = ("
                + "           select wp from CloneWorkspace wp where "
                + "           wp.id = :workspaceId"
                + "        )";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneChunk.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        query.setParameter("isRoot", true);
        query.setParameter("workspaceId", id);

        workspaceRoot = (CloneFile) query.getSingleResult();
        
        return workspaceRoot;
    }
    
    public List<CloneFile> getWorkspacesUpdates() {
        
        String queryStr = "select c from CloneFile c where "
                + "     c.syncStatus = :statusSync and "                
                + "     c.serverUploadedAck = false and "
                + "     c.workspaceRoot = true order by "
                + "         c.path asc, c.version asc";    
        
        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("statusSync", CloneFile.SyncStatus.UPTODATE);

        return query.getResultList();
    }
}