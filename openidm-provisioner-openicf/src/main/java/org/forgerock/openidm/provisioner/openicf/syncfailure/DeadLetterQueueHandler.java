/*
 * Copyright 2013-2014 ForgeRock, AS.
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package org.forgerock.openidm.provisioner.openicf.syncfailure;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openidm.util.Accessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Handle a LiveSync failure by saving its detail to a dead-letter queue.  The queue
 * is implemented as a repository target.
 *
 */
public class DeadLetterQueueHandler implements SyncFailureHandler {

    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueHandler.class);

    private final ConnectionFactory connectionFactory;

    /** accessor to the router */
    private final Accessor<ServerContext> accessor;

    /**
     * Construct this live sync failure handler.
     *
     * @param connectionFactory
     * @param accessor an accessor to the router
     */
    public DeadLetterQueueHandler(ConnectionFactory connectionFactory, Accessor<ServerContext> accessor) {
        this.connectionFactory = connectionFactory;
        this.accessor = accessor;
    }

    /**
     * Handle sync failure.
     *
     * @param syncFailure JsonValue that contains the sync failure data
     * @param failureCause the cause of the sync failure
     * @throws SyncHandlerException when retries are not exceeded
     */
    public void invoke(Map<String, Object> syncFailure, Exception failureCause)
        throws SyncHandlerException {

        final String resourceContainer = new StringBuilder("repo/synchronisation/deadLetterQueue/")
            .append(syncFailure.get("systemIdentifier"))
            .toString();
        final String resourceId = syncFailure.get("token").toString();

        try {
            Map<String,Object> syncDetail = new HashMap<String, Object>(syncFailure);
            syncDetail.put("failureCause", failureCause.toString());
            CreateRequest request = Requests.newCreateRequest(resourceContainer, resourceId, new JsonValue(syncDetail));
            ServerContext routeContext = accessor.access();
            connectionFactory.getConnection().create(routeContext, request);
            logger.info("{} saved to dead letter queue", syncFailure.get("uid"));
        } catch (ResourceException e) {
            throw new SyncHandlerException("Failed reading/writing " + resourceContainer + "/" + resourceId, e);
        }
    }
}
