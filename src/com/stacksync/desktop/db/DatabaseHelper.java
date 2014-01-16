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
import com.stacksync.desktop.db.models.Workspace;
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

        /// GGIPART ///
        String path = FileUtil.getRelativeParentDirectory(root.getLocalFile(), file);
        path = FileUtil.getFilePathCleaned(path);
        /// GGIENDPART ///

        // First, check by full file path
        String queryStr =
                "select f from CloneFile f where "
                + "      f.filePath = :path and "
                + "      f.name = :name "
                + ((folder != null) ? "and f.folder = :folder " : " ")
                + "      and f.status <> :notStatus1 "
                //+ "      and f.status <> :notStatus2 "
                //+ "      and f.syncStatus <> :notSyncStatus "
                + "      and f.version = (select max(ff.version) from CloneFile ff where "
                + "         f.fileId = ff.fileId) "
                + "      order by f.updated desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        
        //System.err.println(queryStr);
        //logger.severe("getFileOrFolder: rel parent = " + FileUtil.getRelativeParentDirectory(root.getLocalFile(), file) + " / file name = " + file.getName() + " / folder = " + file.isDirectory());
        query.setMaxResults(1);
        query.setParameter("path", path);
        query.setParameter("name", file.getName());
        query.setParameter("notStatus1", Status.DELETED);
        //query.setParameter("notStatus2", Status.MERGED);
        //query.setParameter("notSyncStatus", CloneFile.SyncStatus.SYNCING); // this is required for chmgr.applyNewOrChange()

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
                //+ "      f.status <> :notStatus2 and "
                + "      f.parent = :parent and "
                + "      f.version = (select max(ff.version) from CloneFile ff where "
                + "                                         f.fileId = ff.fileId) ";

//	Systconfig.getDatabase().getEntityManager().err.println("rel parent = "+FileUtil.getRelativeParentDirectory(root.getFile(), file) + " / file name = "+file.getName() + " / folder = "+file.isDirectory());
        
        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");
        
        query.setParameter("notStatus1", Status.DELETED);
        //query.setParameter("notStatus2", Status.MERGED);
        query.setParameter("parent", parentFile);

        return query.getResultList();
    }

    public List<CloneFile> getAllChildren(CloneFile parentFile) {
        // First, check by full file path
        String queryStr = "select f from CloneFile f where "
                + "      f.status <> :notStatus1 and "
                //+ "      f.status <> :notStatus2 and "
                + "      f.path like :pathPrefix and "
                + "      f.version = (select max(ff.version) from CloneFile ff where "
                + "                                         f.fileId = ff.fileId) ";

//	Systconfig.getDatabase().getEntityManager().err.println("rel parent = "+FileUtil.getRelativeParentDirectory(root.getFile(), file) + " / file name = "+file.getName() + " / folder = "+file.isDirectory());
        
        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("notStatus1", Status.DELETED);
        //query.setParameter("notStatus2", Status.MERGED);
        query.setParameter("pathPrefix", FileUtil.getRelativePath(parentFile.getRoot().getLocalFile(), parentFile.getFile()));

        return query.getResultList();
    }

    /**
     * Get file in exact version.
     *
     * @param fileId
     * @param version
     * @return
     */
    public CloneFile getFileOrFolder(long fileId, long version) {        
        for (int i = 1; i <= MAXTRIES; i++) {
            try {
                String queryStr = "select f from CloneFile f where "
                        + "      f.fileId = :fileId and "
                        + "      f.version = :version";

                Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
                query.setHint("javax.persistence.cache.storeMode", "REFRESH");
                query.setHint("eclipselink.cache-usage", "DoNotCheckCache");                
                
                query.setParameter("fileId", fileId);
                query.setParameter("version", version);

                return (CloneFile) query.getSingleResult();

            } catch (NoResultException ex) {
                logger.debug(" No result for fId->" + fileId + " fV-> " + version + " -> " + ex.getMessage());
                continue;
            } 
        }
        
        return null;
    }

    /**
     * Get file in current (newest) version.
     */
    public CloneFile getFileOrFolder(long fileId) {
        String queryStr = "select f from CloneFile f "
                + "where f.fileId = :fileId "
                + "      and f.version = (select max(ff.version) from CloneFile ff where "
                + "                                     f.fileId = ff.fileId)";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("fileId", fileId);

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
                //+ "      f.status <> :notStatus2 and "
                + "      f.version = (select max(ff.version) from CloneFile ff where "
                + "                                     f.fileId = ff.fileId) "
                + "      order by f.updated desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("notStatus1", Status.DELETED);
        //query.setParameter("notStatus2", Status.MERGED);
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
                //+ "      f.status <> :notStatus2 and"
                + "      f.version = (select max(ff.version) from CloneFile ff where "
                + "                                     f.fileId = ff.fileId) ";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneFile.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("notStatus1", Status.DELETED);
        //query.setParameter("notStatus2", Status.MERGED);

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
               
        newFile.setFileId(update.getFileId());
        newFile.setVersion(update.getVersion());
        newFile.setUpdated(update.getUpdated());
        newFile.setChecksum(update.getChecksum());
        newFile.setProfile(profile);

        String path = update.getPath();

        path = FileUtil.getFilePathCleaned(path);
        newFile.setPath(path);
        newFile.setWorkspace(update.getWorkspace());
        
        newFile.setMimetype(update.getMimeType());
        newFile.setServerUploadedAck(true);
        newFile.setServerUploadedTime(update.getServerUploadedTime());        
        
        newFile.setName(update.getName());
        newFile.setLastModified(update.getLastModified());
        newFile.setFileSize(update.getFileSize());
        newFile.setStatus(update.getStatus());
        newFile.setSyncStatus(syncStatus);
        newFile.setFolder(update.isFolder());

        if (update.getParentFileId() != 0) {
            CloneFile parentCF = getFileOrFolder(update.getParentFileId(), update.getParentFileVersion());
            newFile.setParent(parentCF);
        }
        newFile.merge();
        
        // Chunks from previous version
        if (update.getVersion() > 1) {

            CloneFile previousVersion = getFileOrFolder(update.getFileId(), update.getVersion() - 1);
            if (previousVersion != null) {
                /// GGI -> removed now always add the news chunks
                //newFile.setChunks(previousVersion.getChunks());
            } else {
                logger.warn("Could not find previous version for file" + newFile + "in database.");
            }
        }

        // Add Chunks (if there are any!)
        // Triggered for new files (= version 1) AND for grown files (= more chunks)
        if (!update.getChunks().isEmpty()) {
            for(int i=0; i<update.getChunks().size(); i++){
                String chunkId = update.getChunks().get(i);
                CloneChunk chunk = getChunk(chunkId, path, i, CacheStatus.REMOTE);
                                                
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
    public Long getFileVersionCount(Profile profile) {
        Calendar cal = Calendar.getInstance();  
        cal.set(getFieldTimeout(), getValueTimeout(cal)); 
        Date time = cal.getTime();
 
        // Newest file update
        String queryStr = "select count(c.fileId) from CloneFile c where "
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

    public synchronized CloneChunk getChunk(String checksum, String path, int chunkOrder, CacheStatus status) {
        CloneChunk chunk;

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        String queryStr = "select c from CloneChunk c where "
                + "     c.checksum = :checksum and "
                + "     c.chunkOrder = :chunkOrder and "
                + "     c.chunkpath = :path";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneChunk.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("checksum", checksum);
        query.setParameter("chunkOrder", chunkOrder);
        query.setParameter("path", path);

        try {
            chunk = (CloneChunk) query.getSingleResult();
            logger.info("Found chunk (" + chunkOrder + ") in DB: " + chunk);
        } catch (NoResultException e) {
            logger.info("New chunk (" + chunkOrder + "): " + checksum);

            chunk = new CloneChunk(checksum, chunkOrder, status);
            chunk.setPath(path);

            /*try {
                chunk.merge();
            } catch (Exception e1) {
                logger.info("RETRY for chunk (" + chunkOrder + ") " + checksum + ", because adding failed!! (try = " + tryCount + ")", e1);
                continue;
            }*/
            // TODO: can clash if two accounts index the same files at the same time
            // TODO: that's why we do a merge here!
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
    
    public Map<String, List<CloneFile>> getHistoryUptoDate(Profile profile) {
        Calendar cal = Calendar.getInstance();  
        cal.set(getFieldTimeout(), getValueTimeout(cal)); 
        Date time = cal.getTime();        
        
        String queryStr = "select c from CloneFile c where "
                + "     c.syncStatus = :statusSync and "                
                + "     c.serverUploadedAck = false and "                
                + "     (c.serverUploadedTime < :timeNow or "
                + "     c.serverUploadedTime is null) order by "
                + "                                     c.filePath asc, c.version asc";    
        
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
    
    public Map<String, Workspace> getWorkspaces() {        
        String queryStr = "select w from Workspace w";        
        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, Workspace.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        
        List<Workspace> workspaces = query.getResultList();
        Map<String, Workspace> localWorkspaces = new HashMap<String, Workspace>();
        
        for (Workspace w: workspaces){
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

}