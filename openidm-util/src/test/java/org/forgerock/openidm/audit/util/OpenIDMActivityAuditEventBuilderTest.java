/*
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
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openidm.audit.util;

import static org.fest.assertions.api.Assertions.assertThat;

import org.forgerock.audit.events.AuditEvent;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.RootContext;
import org.testng.annotations.Test;

public class OpenIDMActivityAuditEventBuilderTest {

    public static final String TEST_CONTEXT_ID = "TEST_CONTEXT_ID";
    public static final String TEST_MESSAGE = "this is a test message";
    public static final String TEST_OBJECT_ID = "managed/user/jdoe";
    public static final String TEST_RUN_AS = "principal";

    /**
     * OpenIDMActivityAuditEventBuilder only adds a few fields so only those are tested here.
     *
     * @see org.forgerock.audit.events.ActivityAuditEventBuilderTest for tests on the base attributes
     */
    @Test
    public void testAuditEventBuilder() {

        Request request = Requests.newActionRequest("some/resource", "customAction");
        RootContext context = new RootContext(TEST_CONTEXT_ID);
        String[] changedFields = new String[]{"test"};

        AuditEvent event = OpenIDMActivityAuditEventBuilder.auditEventBuilder()
                .transactionIdFromRootContext(context)
                .timestamp(System.currentTimeMillis())
                .eventName(RouterActivityLogger.ACTIVITY_EVENT_NAME)
                .authentication("fake")
                .runAs(TEST_RUN_AS)
                .resourceOperationFromRequest(request)
                .before(null)
                .after(null)
                .changedFields(changedFields)
                .revision("6")
                .message(TEST_MESSAGE)
                .objectId(TEST_OBJECT_ID)
                .passwordChanged(false)
                .status(Status.SUCCESS)
                .toEvent();

        JsonValue eventValue = event.getValue();
        assertThat(eventValue.get(OpenIDMActivityAuditEventBuilder.MESSAGE).asString()).isEqualTo(TEST_MESSAGE);
        assertThat(eventValue.get(OpenIDMActivityAuditEventBuilder.OBJECT_ID).asString()).isEqualTo(TEST_OBJECT_ID);
        assertThat(eventValue.get(OpenIDMActivityAuditEventBuilder.PASSWORD_CHANGED).asBoolean()).isEqualTo(false);
        assertThat(eventValue.get(OpenIDMActivityAuditEventBuilder.STATUS).asEnum(Status.class))
                .isEqualTo(Status.SUCCESS);
    }
}
