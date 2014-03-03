package com.stacksync.desktop.db;

import org.apache.log4j.Logger;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.logging.SessionLogEntry;
import com.stacksync.desktop.Environment;

public class CustomSessionLogger extends AbstractSessionLog implements SessionLog{
    
    public static final Environment env = Environment.getInstance();
	public static final Logger logger = Logger.getLogger(CustomSessionLogger.class);
    
    @Override
	public void log(SessionLogEntry sessionLogEntry) {
		/*
        switch (sessionLogEntry.getLevel()) {
            case SEVERE:
				logger.error(sessionLogEntry.getMessage(), sessionLogEntry.getException());                
				break;
			case WARNING:
				logger.warn(sessionLogEntry.getMessage(), sessionLogEntry.getException());
				break;
			case INFO:
				logger.info(sessionLogEntry.getMessage(), sessionLogEntry.getException());
				break;
			default:
				logger.debug(sessionLogEntry.getMessage(), sessionLogEntry.getException());
        }
        */
        logger.error("Database Error: ", (Exception) sessionLogEntry.getException());
        //LogConfig.sendErrorLogs();
	}

}