/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.stacksync.desktop.db.models;

import com.stacksync.desktop.config.Config;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.apache.log4j.Logger;

/**
 *
 * @author cotes
 */
public class CloneItemServices {
    
    private static final Logger logger = Logger.getLogger(CloneItemServices.class.getName());
    private static final Config config = Config.getInstance();
    
    public List<CloneItem> getVersionHistory() {
        List<CloneItem> versions = new ArrayList<CloneItem>();

        /*versions.addAll(getPreviousVersions());
        versions.add(this);
        versions.addAll(getNextVersions());*/

        return versions;
    }

    public CloneItem getFirstVersion(CloneItem item) {
        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id "
               // + "   and c.version = 1");
                + "     order by c.version asc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setMaxResults(1);
        query.setParameter("id", item.getId());

        try {
            return (CloneItem) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }
    
    // TODO optimize this!!! It returns a list with ALL the entries where the
    // parent is this file. This is incorrect since could be files moved to
    // other folders in further versions.
    public List<CloneItem> getChildren() {
        
        String queryStr = "select c from CloneFile c where "
                + "     c.parent = :parent";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setParameter("parent", this);
        List<CloneItem> list = query.getResultList();
        
        /*for(CloneFile cf: list){
            config.getDatabase().getEntityManager().refresh(cf);
        }*/
        return list;
    }

    public CloneItem getLastVersion(CloneItem item) {
        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id"
                + "     order by c.version desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");        
        
        query.setMaxResults(1);
        query.setParameter("id", item.getId());

        try {
            return (CloneItem) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }
    
    public CloneItem getLastSyncedVersion(CloneItem item) {

        String queryStr = "select c from CloneFile c where "
                + "     c.id = :id and "
                + "     c.version < :version and "
                + "     c.syncStatus = :syncStatus "
                + "     order by c.version desc";

        Query query = config.getDatabase().getEntityManager().createQuery(queryStr, CloneItem.class);
        query.setHint("javax.persistence.cache.storeMode", "REFRESH");
        query.setHint("eclipselink.cache-usage", "DoNotCheckCache");    
        query.setMaxResults(1);
        
        query.setParameter("id", item.getId());
        //query.setParameter("version", getVersion());
        query.setParameter("syncStatus", CloneItem.SyncStatus.UPTODATE);

        try {
            return (CloneItem) query.getSingleResult();
        } catch (NoResultException ex) {
            logger.info(" No result -> " + ex.getMessage());
            return null;
        }
    }

    
}
