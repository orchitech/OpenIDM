/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.openidm.audit.impl;

import java.util.ArrayList;
//import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;

import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

import org.forgerock.openidm.audit.AuditService;
import org.forgerock.openidm.config.EnhancedConfig;
import org.forgerock.openidm.config.InvalidException;
import org.forgerock.openidm.config.JSONEnhancedConfig;
import org.forgerock.openidm.objset.BadRequestException;
import org.forgerock.openidm.objset.ConflictException;
import org.forgerock.openidm.objset.ForbiddenException;
import org.forgerock.openidm.objset.NotFoundException;
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.objset.Patch;
import org.forgerock.openidm.objset.PreconditionFailedException;
import org.forgerock.openidm.repo.QueryConstants;
import org.forgerock.openidm.repo.RepositoryService; 


/**
 * Audit module
 * @author aegloff
 */
@Component(name = "org.forgerock.openidm.audit", immediate=true, policy=ConfigurationPolicy.REQUIRE)
@Service
@Properties({
    @Property(name = "service.description", value = "Audit Service"),
    @Property(name = "service.vendor", value = "ForgeRock AS")
})
public class AuditServiceImpl implements AuditService {
    final static Logger logger = LoggerFactory.getLogger(AuditServiceImpl.class);

    // Keys in the JSON configuration
    public final static String CONFIG_LOG_TO = "logTo";
    public final static String CONFIG_LOG_TYPE = "logType";
    public final static String CONFIG_LOG_TYPE_CSV = "csv";
    public final static String CONFIG_LOG_TYPE_REPO = "repository";
    
    EnhancedConfig enhancedConfig = new JSONEnhancedConfig();

    List<AuditLogger> auditLoggers;
    
    /**
     * Gets an object from the audit logs by identifier. The returned object is not validated 
     * against the current schema and may need processing to conform to an updated schema.
     * <p>
     * The object will contain metadata properties, including object identifier {@code _id},
     * and object version {@code _rev} to enable optimistic concurrency 
     *
     * @param id the identifier of the object to retrieve from the object set.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws BadRequestException if the passed identifier is invalid
     * @return the requested object.
     */
    public Map<String, Object> read(String fullId) throws ObjectSetException {
        // TODO
        return new HashMap();
    }

    /**
     * Creates a new object in the object set.
     * <p>
     * This method sets the {@code _id} property to the assigned identifier for the object,
     * and the {@code _rev} property to the revised object version (For optimistic concurrency)
     *
     * @param id the client-generated identifier to use, or {@code null} if server-generated identifier is requested.
     * @param object the contents of the object to create in the object set.
     * @throws NotFoundException if the specified id could not be resolved. 
     * @throws ForbiddenException if access to the object or object set is forbidden.
     * @throws PreconditionFailedException if an object with the same ID already exists.
     */
    public void create(String fullId, Map<String, Object> obj) throws ObjectSetException {
        String id = stripFirstLevel(fullId);
        logger.debug("Create audit entry for {} with {}", id, obj);
        for (AuditLogger auditLogger : auditLoggers) {
            auditLogger.create(id, obj);
        }
    }
    
    /**
     * Audit service does not support changing audit entries.
     */
    public void update(String fullId, String rev, Map<String, Object> obj) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Audit service currently does not support deleting audit entries.
     * 
     * Deletes the specified object from the object set.
     *
     * @param id the identifier of the object to be deleted.
     * @param rev the version of the object to delete or {@code null} if not provided.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws ForbiddenException if access to the object is forbidden.
     * @throws ConflictException if version is required but is {@code null}.
     * @throws PreconditionFailedException if version did not match the existing object in the set.
     */ 
    public void delete(String fullId, String rev) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Audit service does not support changing audit entries.
     */
    public void patch(String id, String rev, Patch patch) throws ObjectSetException {
        throw new UnsupportedOperationException();
    }

    /**
     * Performs the query on the specified object and returns the associated results.
     * <p>
     * Queries are parametric; a set of named parameters is provided as the query criteria.
     * The query result is a JSON object structure composed of basic Java types. 
     * 
     * The returned map is structured as follow: 
     * - The top level map contains meta-data about the query, plus an entry with the actual result records.
     * - The <code>QueryConstants</code> defines the map keys, including the result records (QUERY_RESULT)
     *
     * @param id identifies the object to query.
     * @param params the parameters of the query to perform.
     * @return the query results, which includes meta-data and the result records in JSON object structure format.
     * @throws NotFoundException if the specified object could not be found. 
     * @throws BadRequestException if the specified params contain invalid arguments, e.g. a query id that is not
     * configured, a query expression that is invalid, or missing query substitution tokens.
     * @throws ForbiddenException if access to the object or specified query is forbidden.
     */
    public Map<String, Object> query(String fullId, Map<String, Object> params) throws ObjectSetException {
        // TODO
        return new HashMap();
        /*
        // TODO: replace with common utility
        String type = fullId; 
        // This should not be necessary as relative URI should not start with slash
        if (fullId != null && fullId.startsWith("/")) {
            type = fullId.substring(1);
        }
        logger.trace("Full id: {} Extracted type: {}", fullId, type);
        
        Map<String, Object> result = new HashMap<String, Object>();
        ODatabaseDocumentTx db = pool.acquire(dbURL, user, password);
        try {
            List<Map<String, Object>> docs = new ArrayList<Map<String, Object>>();
            result.put(QueryConstants.QUERY_RESULT, docs);
            long start = System.currentTimeMillis();
            List<ODocument> queryResult = queries.query(type, params, db); 
            long end = System.currentTimeMillis();
            if (queryResult != null) {
                long convStart = System.currentTimeMillis();
                for (ODocument entry : queryResult) {
                    Map<String, Object> convertedEntry = DocumentUtil.toMap(entry);
                    docs.add(convertedEntry);
                }
                long convEnd = System.currentTimeMillis();
                result.put(QueryConstants.STATISTICS_CONVERSION_TIME, Long.valueOf(convEnd-convStart));
            }
            result.put(QueryConstants.STATISTICS_QUERY_TIME, Long.valueOf(end-start));
            
            if (logger.isDebugEnabled()) {
                logger.debug("Query result contains {} records, took {} ms and took {} ms to convert result.",
                        new Object[] {((Map) result.get(QueryConstants.QUERY_RESULT)).size(),
                        result.get(QueryConstants.STATISTICS_QUERY_TIME),
                        result.get(QueryConstants.STATISTICS_CONVERSION_TIME)});
            }
        } finally {
            if (db != null) {
                db.close();
                pool.release(db);
            }
        }
        
        return result;
        */
    }
    
    // TODO: replace with common utility to handle ID, this is temporary
    private String getLocalId(String id) {
        String localId = null;
        int lastSlashPos = id.lastIndexOf("/");
        if (lastSlashPos > -1) {
            localId = id.substring(id.lastIndexOf("/") + 1);
        }
        logger.trace("Full id: {} Extracted local id: {}", id, localId);
        return localId;
    }
    
    // strip off log/ in the beginning of the id 
    private String stripFirstLevel(String id) {
        int firstSlashPos = id.indexOf("/");
        return id.substring(firstSlashPos + 1);
    }
    
    @Activate
    void activate(ComponentContext compContext) { 
        logger.debug("Activating Service with configuration {}", compContext.getProperties());
        JsonNode config = null;
        try {
            config = enhancedConfig.getConfigurationAsJson(compContext);
            System.out.println("Audit service config: " + config);
            auditLoggers = new ArrayList();
            List logTo = config.get(CONFIG_LOG_TO).asList();
            for (Map entry : (List<Map>)logTo) {
                String logType = (String) entry.get(CONFIG_LOG_TYPE);
                // TDDO: make pluggable
                AuditLogger auditLogger = null;
                if (logType != null && logType.equalsIgnoreCase(CONFIG_LOG_TYPE_CSV)) {
                    auditLogger = new CSVAuditLogger();
                    auditLogger.setConfig(entry);
                    logger.info("Audit configured to log to CSV");
                } else if (logType != null && logType.equalsIgnoreCase(CONFIG_LOG_TYPE_REPO)) {
                 //   auditLogger = new RepoAuditLogger(entry);
                    logger.info("Audit configured to log to Repository");
                } else {
                    throw new InvalidException("Configured audit logType is unknown: " + logType);
                }
                if (auditLogger != null) {
                    auditLoggers.add(auditLogger);
                }
            }
        } catch (RuntimeException ex) {
            logger.warn("Configuration invalid, can not start Audit service.", ex);
            throw ex;
        }
        logger.info("Audit service started.");
    }

    /* Currently rely on deactivate/activate to be called by DS if config changes instead
    @Modified
    void modified(ComponentContext compContext) {
    }
    */

    
    @Deactivate
    void deactivate(ComponentContext compContext) { 
        logger.debug("Deactivating Service {}", compContext.getProperties());
        for (AuditLogger auditLogger : auditLoggers) {
            try {
                auditLogger.cleanup();
            } catch (Exception ex) {
                logger.info("AuditLogger cleanup reported failure", ex);
            }
        }
        logger.info("Audit service stopped.");
    }
}