package com.stacksync.desktop.index.requests;

import com.ast.cloudABE.exceptions.AttributeNotFoundException;
import com.stacksync.desktop.Environment;
import com.stacksync.desktop.chunker.ChunkEnumeration;
import com.stacksync.desktop.chunker.FileChunk;
import com.stacksync.desktop.config.Folder;
import com.stacksync.desktop.db.models.ABEMetaComponent;
import com.stacksync.desktop.db.models.CloneChunk;
import com.stacksync.desktop.db.models.CloneFile;
import com.stacksync.desktop.db.models.CloneWorkspace;
import com.stacksync.desktop.encryption.AbeCipherData;
import com.stacksync.desktop.encryption.AbeEncryption;
import com.stacksync.desktop.encryption.AbePlainData;
import com.stacksync.desktop.encryption.CipherData;
import com.stacksync.desktop.encryption.Encryption;
import com.stacksync.desktop.encryption.PlainData;
import com.stacksync.desktop.gui.sharing.AttributesSelector;
import com.stacksync.desktop.gui.tray.Tray;
import com.stacksync.desktop.logging.RemoteLogs;
import com.stacksync.desktop.util.FileUtil;
import java.io.File;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import javax.swing.tree.TreePath;
import org.apache.log4j.Logger;

public class NewIndexSharedRequest extends SingleRootIndexRequest {

    private final Logger logger = Logger.getLogger(NewIndexRequest.class.getName());

    private File file;
    private long checksum;
    private CloneFile previousVersion;

    public NewIndexSharedRequest(Folder root, File file, CloneFile previousVersion, long checksum) {
        super(root);

        this.file = file;
        this.checksum = checksum;
        this.previousVersion = previousVersion;
    }

    @Override
    public void process() {
        logger.info("Indexer: Indexing new share file " + file + " ...");

        // Find file in DB
        CloneFile dbFile = db.getFileOrFolder(root, file);
        if (dbFile != null) {
            if (dbFile.getChecksum() == checksum) {
                logger.warn("Indexer: Error already Indexed this version. Ignoring.");
                return;
            }

            if (dbFile.isFolder()) {
                logger.warn("Indexer: Error already NewIndexRequest this folder. Ignoring.");
                return;
            }
        }

        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPDATING);

        // Create DB entry
        CloneFile newVersion = (previousVersion == null) ? addNewVersion() : addChangedVersion();

        File parentFile = FileUtil.getCanonicalFile(file.getParentFile());
        CloneFile parentCF = db.getFolder(root, parentFile);
        newVersion.setParent(parentCF);

        CloneWorkspace workspace = parentCF.getWorkspace();
        newVersion.setWorkspace(workspace);

        // IMPORTANT Since a shared folder is created without using watcher+indexer
        // it should always exist a parent.
        // This will check if the file is inside a folder that isn't created.
        /*if (newVersion.getParent() == null && !newVersion.getPath().equals("/")) {
         Indexer.getInstance().queueNewIndex(root, file, null, checksum);
         return;
         }*/
        newVersion.setFolder(file.isDirectory());
        newVersion.setSize(file.length());

        newVersion.setLastModified(new Date(file.lastModified()));
        newVersion.setSyncStatus(CloneFile.SyncStatus.LOCAL);
        newVersion.merge();

        if (newVersion.isFolder()) {
            processFolder(newVersion);
        } else {
            processFile(newVersion);
        }

        this.tray.setStatusIcon(this.processName, Tray.StatusIcon.UPTODATE);
    }

    private CloneFile addNewVersion() {
        CloneFile newVersion = new CloneFile(root, file);

        newVersion.setVersion(1);
        newVersion.setStatus(CloneFile.Status.NEW);

        return newVersion;
    }

    private CloneFile addChangedVersion() {
        CloneFile newVersion = (CloneFile) previousVersion.clone();

        if (newVersion.getSyncStatus() == CloneFile.SyncStatus.UNSYNC
                && previousVersion.getStatus() != CloneFile.Status.RENAMED) {
            if (previousVersion.getVersion() == 1) {
                previousVersion.remove();
                newVersion = this.addNewVersion();
            } else {
                newVersion.setStatus(CloneFile.Status.CHANGED);
                newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
                newVersion.setMimetype(FileUtil.getMimeType(newVersion.getFile()));
                previousVersion.remove();
            }
        } else {
            newVersion.setVersion(previousVersion.getVersion() + 1);
            newVersion.setStatus(CloneFile.Status.CHANGED);
            newVersion.setChunks(new ArrayList<CloneChunk>()); // otherwise we would append!
            newVersion.setMimetype(FileUtil.getMimeType(newVersion.getFile()));
        }

        return newVersion;
    }

    private void processFolder(CloneFile cf) {
        // Add rest of the DB stuff 
        cf.setChecksum(0);

        if (FileUtil.checkIllegalName(cf.getName())
                || FileUtil.checkIllegalName(cf.getPath().replace("/", ""))) {
            cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
        } else {
            cf.setSyncStatus(CloneFile.SyncStatus.UPTODATE);
        }
        cf.merge();

        // Not necessary yet. Shared folders don't contain files.
        // Analyze file tree (if directory) -- RECURSIVELY!!
        /*logger.info("Indexer: Indexing CHILDREN OF "+file+" ...");
         for (File child: file.listFiles()) {
         // Ignore .ignore files
         if (FileUtil.checkIgnoreFile(root, child)) {
         continue; 
         }
            
         // Do it!
         logger.info("Indexer: Parent: "+file+" / CHILD "+child+" ...");
         Indexer.getInstance().queueNewIndex(root, child, null, -1);
         }*/
    }

        private TreePath[] openPermissions(String resourcesPath){
         /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
   
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AttributesSelector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AttributesSelector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AttributesSelector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AttributesSelector.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        
        /* Create and display the dialog */

        AttributesSelector dialog = new AttributesSelector(new javax.swing.JFrame(), true, resourcesPath);
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.exit(0);
            }
        });

        dialog.setVisible(true);

        /*
        if(dialog.getSelecteds()!=null){
            for(TreePath path:dialog.getSelecteds()){
                System.out.println(path);
            }
        }*/

        return dialog.getSelecteds();

    }
        
    private void processFile(CloneFile cf) {
        Encryption enc;

        String path = Environment.getInstance().getDefaultUserConfigDir().getAbsolutePath()+"/conf/abe/";
        
        /*FIXME! correct path*/
        System.out.println(config.getResDir());
        System.out.println(config.getConfDir());

        
        TreePath attributes[] = openPermissions(path);
        ArrayList attrs = new ArrayList();

        if (attributes != null) {
            for (TreePath attribute : attributes) {
                attrs.add(attribute.getLastPathComponent().toString());
            }
        }
            
        try {
            // 1. Chunk it!
            FileChunk chunkInfo = null;

            logger.info("[ABE Benchmarking - File Info] New Shared File: " + cf.getName() + " size: " + cf.getSize());

            // 1.a Attribute-Based encryption tasks 
            if (cf.getWorkspace().isAbeEncrypted()) {
                logger.info("[ABE Benchmarking - Symmetric key gen] Generating symmetric key... ");
                // Get the ABE encryption
                AbeEncryption abenc = (AbeEncryption) root.getProfile().getEncryption(cf.getWorkspace().getId());
                // Generate key
                byte[] key = abenc.generateSymKey();
                // Initialize BasicEncryption object from generated key (encryption of chunks)
                enc = abenc.getBasicEncryption(key);
                // Encrypt key using ABE protocol
                AbeCipherData abeCipherMeta = getEncryptedSymKey(abenc, key, attrs);
                // Save the produced ABE encryption metadata in CloneFile Object
                cf.setCipherSymKey(abeCipherMeta.getCipherText());
                cf.setAbeComponents(abeCipherMeta.getAbeMetaComponents());
            } else {
                // 1.b Get encryption 
                enc = root.getProfile().getEncryption(cf.getWorkspace().getId());
            }

            //ChunkEnumeration chunks = chunker.createChunks(file, root.getProfile().getRepository().getChunkSize());
            ChunkEnumeration chunks = chunker.createChunks(file);
            logger.info("[ABE Benchmarking - Packing] Packing chunks...");
            while (chunks.hasMoreElements()) {
                chunkInfo = chunks.nextElement();

                // create chunk in DB (or retrieve it)
                CloneChunk chunk = db.getChunk(chunkInfo.getChecksum(), CloneChunk.CacheStatus.CACHED);

                // write encrypted chunk (if it does not exist)
                File chunkCacheFile = config.getCache().getCacheChunk(chunk);

                byte[] packed = FileUtil.pack(chunkInfo.getContents(), enc);
                if (!chunkCacheFile.exists()) {
                    FileUtil.writeFile(packed, chunkCacheFile);
                } else {
                    if (chunkCacheFile.length() != packed.length) {
                        FileUtil.writeFile(packed, chunkCacheFile);
                    }
                }

                cf.addChunk(chunk);
            }
            logger.info("[ABE Benchmarking - Packing] All chunks have been successfully packed.");

            logger.info("Indexer: saving chunks...");
            cf.merge();
            logger.info("Indexer: chunks saved...");

            // 2. Add the rest to the DB, and persist it
            if (chunkInfo != null) { // The last chunk holds the file checksum                
                cf.setChecksum(chunkInfo.getFileChecksum());
            } else {
                cf.setChecksum(checksum);
            }
            cf.merge();
            chunks.closeStream();

            // 2.a. Persist produced ABE metadata if required
            if (cf.getWorkspace().isAbeEncrypted()) {
                for (ABEMetaComponent meta : cf.getAbeComponents()) {
                    meta.setFile(cf);
                    meta.setVersion(0L);
                    meta.persist();
                }
            }

            // 3. Upload it
            if (FileUtil.checkIllegalName(cf.getName())
                    || FileUtil.checkIllegalName(cf.getPath().replace("/", ""))) {
                logger.info("This filename contains illegal characters.");
                cf.setSyncStatus(CloneFile.SyncStatus.UNSYNC);
                cf.merge();
            } else {
                logger.info("Indexer: Added to DB. Now Q file " + file + " at uploader ...");
                root.getProfile().getUploader().queue(cf);
            }

        } catch (Exception ex) {
            logger.error("Could not index new file " + file + ". IGNORING.", ex);
            RemoteLogs.getInstance().sendLog(ex);
        }
    }

    /**
     * Encrypt a given key using ABE protocol
     *
     * @param enc
     * @param data
     * @return produced ABE metadata
     */
    private AbeCipherData getEncryptedSymKey(AbeEncryption enc, byte[] data, ArrayList<String> attributes) {

        //FIXME: Attribute set obtained from file (testing purposes)
        PlainData plainData = new AbePlainData(data, attributes);

        try {
            logger.info("[ABE Benchmarking - Symmetric key ABE encryption] Encrypting symmetric key... ");
            AbeCipherData cipher = enc.encrypt(plainData);
            logger.info("[ABE Benchmarking - Symmetric key ABE encryption] Symmetric key encrypted... ");
            return cipher;
        } catch (InvalidKeyException ex) {
            java.util.logging.Logger.getLogger(NewIndexSharedRequest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (AttributeNotFoundException ex) {
            java.util.logging.Logger.getLogger(NewIndexSharedRequest.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public String toString() {
        return NewIndexSharedRequest.class.getSimpleName() + "[" + "file=" + file + "]";
    }

}
