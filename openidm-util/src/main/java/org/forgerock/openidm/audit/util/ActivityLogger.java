/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openidm.audit.util;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;

/**
 * Create an audit activity log for a request result.
 *
 */
public interface ActivityLogger {

    /**
     * Write an activity audit log.
     *
     * @param context the context of the request to log
     * @param request the request to log
     * @param message the message to log with this request
     * @param objectId the resourceId being operated on
     * @param before the object value "before" the request
     * @param after the object value "after" the request
     * @param status the status of the operation
     * @throws ResourceException on failure to write to audit log unless suppressed.
     */
    void log(ServerContext context, Request request, String message, String objectId,
             JsonValue before, JsonValue after, Status status) throws ResourceException;
}
