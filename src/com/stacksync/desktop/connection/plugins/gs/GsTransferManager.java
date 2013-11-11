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
package com.stacksync.desktop.connection.plugins.gs;

import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.GSBucket;
import org.jets3t.service.model.StorageBucket;
import com.stacksync.desktop.connection.plugins.rest.RestTransferManager;
import com.stacksync.desktop.exceptions.StorageException;

/**
 *
 * @author Philipp C. Heckel
 */
public class GsTransferManager extends RestTransferManager {
    public GsTransferManager(GsConnection connection) {
        super(connection);
    }

    @Override
    public GsConnection getConnection() {
        return (GsConnection) super.getConnection();
    }
    
    @Override
    protected RestStorageService createService() throws ServiceException {
        return new GoogleStorageService(getConnection().getCredentials());
    }

    @Override
    protected StorageBucket createBucket() {
        return new GSBucket(getConnection().getBucket());
    }   

    @Override
    public void initStorage() throws StorageException {
        //nothing
    }

    @Override
    public String getUser() {
        return getConnection().getUsername();
    }

    public String getStorageIp() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
