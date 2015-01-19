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
import com.stacksync.desktop.config.Config;
import com.stacksync.desktop.config.ConfigNode;
import com.stacksync.desktop.config.Database;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.config.profile.Profile;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneChunk.CacheStatus;
import com.stacksync.desktop.db.models.CloneItem;
import com.stacksync.desktop.db.models.CloneItemVersion;
import com.stacksync.desktop.db.models.CloneItemVersion.Status;
import com.stacksync.desktop.db.models.CloneItemVersion.SyncStatus;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.exceptions.ConfigException;
import com.stacksync.desktop.repository.Update;
import com.stacksync.desktop.util.FileUtil;
import javax.persistence.EntityManager;

/**
 * Provides access to the database.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 */
public class DatabaseHelper {
    
    private final Config config = Config.getInstance();
    private final Logger logger = Logger.getLogger(DatabaseHelper.class.getName());
    private static final DatabaseHelper instance = new DatabaseHelper();
    private Database database;
    private int MAXTRIES = 5;
    

    private DatabaseHelper() {
        logger.debug("Creating DB helper ...");
    }

    public static DatabaseHelper getInstance() {
        return instance;
    }

    public void initializeDatabase(String configFolder, ConfigNode node) throws ConfigException {
        this.database = new Database(configFolder);
        this.database.load(node);
    }
    
    public EntityManager getEntityManager() {
        return this.database.getEntityManager();
    }
    
    public CloneItem getFolder(Folder root, File file) {
        return getFileOrFolder(root, file, true);
    }

    public CloneItem getFile(Folder root, File file) {
        return getFileOrFolder(root, file, false);
    }

    public CloneItem getFileOrFolder(Folder root, File file) {
        return getFileOrFolder(root, file, null);
    }

    public CloneItem getFileOrFolder(File file) {
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

    private CloneItem getFileOrFolder(Folder root, File file, Boolean folder) {
        assert root != null;
        
        String queryStr = "select f from CloneItem f, CloneItemVersion v where "
                + "f.path = :path and "
                + "f.name = :name "
                + ((folder != null) ? "and f.folder = :folder " : " ")
                + " and v.item = f and "
                + "v.version = f.latestVersion and "
                + "v.status <> :notStatus1";

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        
        query.setMaxResults(1);
        
        String path = FileUtil.getRelativeParentDirectory(root.getLocalFile(), file);
        path = FileUtil.getFilePathCleaned(path);
        query.setParameter("path", path);
        query.setParameter("name", file.getName());
        query.setParameter("notStatus1", CloneItemVersion.Status.DELETED);

        if (folder != null) {
            query.setParameter("folder", folder);
        }

        CloneItem dbFile = null;
        try{
            dbFile = (CloneItem) query.getSingleResult();
        } catch(NoResultException ex) {}

        return dbFile;
    }
            
    /*
     * get direct children
     */
    public List<CloneItem> getChildren(CloneItem parentFile) {
        // First, check by full file path
        String queryStr = "select f from CloneItem f, CloneItemVersion v where "
                + "      f.parent = :parent and "
                + "      v.item = f and "
                + "      f.latestVersion = v.version and "
                + "      v.status <> :notStatus1";

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        
        query.setParameter("notStatus1", Status.DELETED);
        query.setParameter("parent", parentFile);

        return query.getResultList();
    }

    /*
     * Get file in current (newest) version.
     */
    public CloneItem getFileOrFolder(long id) {
        String queryStr = "select f from CloneItem f "
                + "where f.id = :id";

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("id", id);

        try {
            return (CloneItem) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.debug(" No result -> " + ex.getMessage());
            return null;
        }
    }

    /*
     * Check the files with the same checksum and don't exist anymore to
     * determine the 'previous version' of this file.
     *
     * If more file are found, i.e. files with the same checksum that don't
     * exist, choose the one with the smallest Levenshtein distance.
     */
    /*public CloneItem getNearestFile(Folder root, File file, long checksum) {
        String queryStr = "select f from CloneFile f where "
                + "      f.checksum = :checksum and "
                + "      f.status <> :notStatus1 and "
                + "      f.version = (select max(ff.version) from CloneFile ff where "
                + "                                     f.id = ff.id) "
                + "      order by f.lastModified desc";

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("notStatus1", Status.DELETED);
        query.setParameter("checksum", checksum);

        List<CloneItem> sameChecksumFiles = query.getResultList();

        CloneItem nearestPreviousVersion = null;
        int previousVersionDistance = Integer.MAX_VALUE;

        for (CloneItem cf : sameChecksumFiles) {
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
    }*/

    public List<CloneItem> getFiles() {

        String queryStr = "select f from CloneItem f, CloneItemVersion v where "
                + "v.item = f and "
                + "v.version = f.latestVersion and "
                + "v.status <> :notStatus1";
        
        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("notStatus1", CloneItemVersion.Status.DELETED);

        return query.getResultList();
    }

    public List<CloneItem> getFiles(SyncStatus status) {
        String queryStr = "select f from CloneItem f where "
                + "      f.syncStatus = :StatusSync ";

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("StatusSync", status);

        return query.getResultList();
    }

    public CloneItem createFile(Profile profile, Update update) {
        SyncStatus status = SyncStatus.REMOTE;
        return createFile(profile, update, status);
    }

    private CloneItem createFile(Profile profile, Update update, SyncStatus syncStatus) {
        CloneItem newFile = new CloneItem(profile.getFolder());
               
        newFile.setId(update.getFileId());
        newFile.setWorkspace(update.getWorkspace());
        newFile.setMimetype(update.getMimeType());
        newFile.setName(update.getName());
        
        newFile.setFolder(update.isFolder());
        newFile.setUsingTempId(false);
        if (update.getParentFileId() != null) {
            CloneItem parentCF = getFileOrFolder(update.getParentFileId());
            newFile.setParent(parentCF);
        }
        newFile.generatePath();
        
        CloneItemVersion version = new CloneItemVersion();
        version.setVersion(update.getVersion());
        version.setChecksum(update.getChecksum());
        version.setServerUploadedAck(true);
        version.setServerUploadedTime(update.getServerUploadedTime());        
        version.setLastModified(update.getModifiedAt());
        version.setSize(update.getFileSize());
        version.setStatus(update.getStatus());
        version.setSyncStatus(syncStatus);
        version.setItem(newFile);

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
                
                version.addChunk(chunk);
            }
        }

        newFile.addVersion(version);
        newFile.merge();
        return newFile;
    }

    public Long getFileVersionCount() {
        Calendar cal = Calendar.getInstance();  
        cal.set(getFieldTimeout(), getValueTimeout(cal)); 
        Date time = cal.getTime();
 
        // Newest file update
        String queryStr = "select count(c.id) from CloneItem c where "
                + "     c.syncStatus = :StatusSync and "                                       
                + "     c.serverUploadedAck = false and "                
                + "     (c.serverUploadedTime < :timeNow or "
                + "     c.serverUploadedTime is null)";

        Query query = this.database.getEntityManager().createQuery(queryStr, Long.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("StatusSync", SyncStatus.UPTODATE);
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

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneChunk.class);
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
        this.database.getEntityManager().getTransaction().begin();

        for (Object o : objects) {
            this.database.getEntityManager().persist(o);
        }

        this.database.getEntityManager().flush();
        this.database.getEntityManager().clear();
        this.database.getEntityManager().getTransaction().commit();
    }

    public synchronized void merge(Object... objects) {
        this.database.getEntityManager().getTransaction().begin();

        for (Object o : objects) {
            this.database.getEntityManager().merge(o);
        }

        this.database.getEntityManager().flush();
        this.database.getEntityManager().clear();
        this.database.getEntityManager().getTransaction().commit();
    }
    
    public void remove(Object... objects) {
        this.database.getEntityManager().getTransaction().begin();

        for (Object o : objects) {
            this.database.getEntityManager().remove(o);
        }

        this.database.getEntityManager().flush();
        this.database.getEntityManager().clear();
        this.database.getEntityManager().getTransaction().commit();
    }
    
    private int getFieldTimeout(){
        return Calendar.MINUTE;
    }
    
    private int getValueTimeout(Calendar cal){
        return cal.get(Calendar.MINUTE) - 1;
    }
    
    public Map<String, List<CloneItem>> getHistoryUptoDate() {
        Calendar cal = Calendar.getInstance();  
        cal.set(getFieldTimeout(), getValueTimeout(cal)); 
        Date time = cal.getTime();        
        
        String queryStr = "select c from CloneItem c where "
                + "     c.syncStatus = :statusSync and "                
                + "     c.serverUploadedAck = false and "
                + "     c.workspaceRoot = false and "
                + "     (c.serverUploadedTime < :timeNow or "
                + "     c.serverUploadedTime is null) order by "
                + "                                     c.path asc";    
        
        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("statusSync", SyncStatus.UPTODATE);       
        query.setParameter("timeNow", time);
        
        Map<String, List<CloneItem>> workspaces = new HashMap<String, List<CloneItem>>();
        List<CloneItem> clonefiles = query.getResultList();
        for(CloneItem cf: clonefiles){
            if(!workspaces.containsKey(cf.getWorkspace().getId())){
                workspaces.put(cf.getWorkspace().getId(), new ArrayList<CloneItem>());
            } 
                
            workspaces.get(cf.getWorkspace().getId()).add(cf);            
        }
        
        return workspaces;
    }
    
    public Map<String, CloneWorkspace> getWorkspaces() {        
        String queryStr = "select w from CloneWorkspace w";        
        Query query = this.database.getEntityManager().createQuery(queryStr, CloneWorkspace.class);
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

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneChunk.class);
        
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        
        query.setParameter("status", CacheStatus.CACHED);        
        return query.getResultList();
    }

    public List<CloneItem> getCloneFiles(CloneChunk chunk) {
        String queryStr = "select distinct c from CloneFile c "
                + "     join c.chunks ch "
                + "     where "
                + "     ch.checksum = :checksum ";        

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
               
        query.setParameter("checksum", chunk.getChecksum());        
        return query.getResultList();        
    }

    public void updateParentId(CloneItem newParent, CloneItem currentParent) {
        String queryStr = "UPDATE CloneItem c set c.parent=:new_parent"
                + "     WHERE c.parent = :current_parent";

        
        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        
        query.setParameter("new_parent", newParent);
        query.setParameter("current_parent", currentParent);

        this.database.getEntityManager().getTransaction().begin();
        query.executeUpdate();
        this.database.getEntityManager().getTransaction().commit();
    }
    
    public CloneWorkspace getDefaultWorkspace() {
        
        CloneWorkspace defaultWorkspace;
        
        String queryStr = "select wp from CloneWorkspace wp where "
                + "     wp.pathWorkspace = :path";

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneChunk.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        query.setParameter("path", "/");

        defaultWorkspace = (CloneWorkspace) query.getSingleResult();
        
        return defaultWorkspace;
    }

    public CloneItem getWorkspaceRoot(String id) {
        CloneItem workspaceRoot;
        
        String queryStr = "select cf from CloneItem cf where "
                + "        cf.workspaceRoot = :isRoot and"
                + "        cf.workspace = ("
                + "           select wp from CloneWorkspace wp where "
                + "           wp.id = :workspaceId"
                + "        )";

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneChunk.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        query.setParameter("isRoot", true);
        query.setParameter("workspaceId", id);

        workspaceRoot = (CloneItem) query.getSingleResult();
        
        return workspaceRoot;
    }
    
    public List<CloneItem> getWorkspacesUpdates() {
        
        Calendar cal = Calendar.getInstance();  
        cal.set(getFieldTimeout(), getValueTimeout(cal)); 
        Date time = cal.getTime();     
        
        String queryStr = "select c from CloneItem c where "
                + "     c.syncStatus = :statusSync and "                
                + "     c.serverUploadedAck = false and "
                + "     c.workspaceRoot = true and "
                + "     (c.serverUploadedTime < :timeNow or "
                + "     c.serverUploadedTime is null) order by "
                + "         c.path asc";
        
        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("statusSync", SyncStatus.UPTODATE);
        query.setParameter("timeNow", time);

        return query.getResultList();
    }

    public CloneWorkspace getWorkspace(String id) {
        String queryStr = "select w from CloneWorkspace w where"
                + "            w.id = :id";        
        Query query = this.database.getEntityManager().createQuery(queryStr, CloneWorkspace.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("id", id);
        CloneWorkspace workspace = (CloneWorkspace)query.getSingleResult();
        
        return workspace;
    }

    public List<CloneItem> getWorkspaceFiles(String id) {
        
        String queryStr = "select f from CloneItem f where "
                + "      f.status <> :notStatus1 and "
                + "      f.workspace = ("
                + "           select wp from CloneWorkspace wp where "
                + "           wp.id = :workspaceId"
                + "      ) and "
                + "      f.workspaceRoot = false";

        Query query = this.database.getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("notStatus1", Status.DELETED);
        query.setParameter("workspaceId", id);

        return query.getResultList();
        
    }
}