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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.sync.impl;

// Java Standard Edition
import java.util.HashMap;
import java.util.Map;

// SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JSON Fluent library
import org.forgerock.json.fluent.JsonException;
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;
import org.forgerock.json.fluent.JsonPointer;

// OpenIDM
import org.forgerock.openidm.script.Script;
import org.forgerock.openidm.script.ScriptException;
import org.forgerock.openidm.script.Scripts;
import org.forgerock.openidm.sync.SynchronizationException;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 */
class PropertyMapping {

    /** TODO: Description. */
    private final static Logger LOGGER = LoggerFactory.getLogger(PropertyMapping.class);

    /** TODO: Description. */
    private final SynchronizationService service;

    /** TODO: Description. */
    private final JsonPointer targetPointer;

    /** TODO: Description. */
    private final JsonPointer sourcePointer;

    /** TODO: Description. */
    private final Script script;

    /** TODO: Description. */
    private final Object defaultValue;

    /**
     * TODO: Description.
     *
     * @param node TODO.
     * @param pointer TODO.
     * @param value TODO.
     * @throws SynchronizationException TODO.
     */
    private static void put(JsonNode targetObject, JsonPointer pointer, Object value) throws SynchronizationException {
        String[] tokens = pointer.toArray();
        if (tokens.length == 0) {
            throw new SynchronizationException("cannot replace root object");
        }
        JsonNode node = targetObject;
        for (int n = 0; n < tokens.length - 1; n++) {
            JsonNode child = node.get(tokens[n]);
            if (child.isNull() && !node.isDefined(tokens[n])) { 
                try {
                    node.put(tokens[n], new HashMap());
                } catch (JsonNodeException jne) {
                    throw new SynchronizationException(jne);
                }
                child = node.get(tokens[n]);
            }
            node = child;
        }
        try {
            node.put(tokens[tokens.length - 1], value);
        } catch (JsonNodeException jne) {
            throw new SynchronizationException(jne);
        }
    }

    /**
     * TODO: Description.
     *
     * @param config TODO.
     * @throws JsonNodeException TODO>
     */
    public PropertyMapping(SynchronizationService service, JsonNode config) throws JsonNodeException {
        this.service = service;
        targetPointer = config.get("target").required().asPointer();
        sourcePointer = config.get("source").asPointer(); // optional
        script = Scripts.newInstance(config.get("script"));
        defaultValue = config.get("default").getValue();
    }

    /**
     * TODO: Description.
     *
     * @param source TODO.
     * @param target TODO.
     * @throws MappingException TODO.
     */
    public void apply(JsonNode sourceObject, JsonNode targetObject) throws SynchronizationException {
        Object result = null;
        if (sourcePointer != null) { // optional
            JsonNode node = sourceObject.get(sourcePointer);
            if (node != null) { // source property value found
                result = node.getValue();
            }
        }
        if (script != null) { // optional
            Map<String, Object> scope = service.newScope();
            scope.put("source", result);
            try {
                result = script.exec(scope); // script yields transformation result
            } catch (ScriptException se) {
                LOGGER.debug("property mapping " + sourcePointer + " -> " + targetPointer +
                 "script encountered exception", se);
                throw new SynchronizationException(se);
            }
        }
        if (result == null) {
            result = defaultValue; // default null if not specified
        }
        put(targetObject, targetPointer, result);
    }
}
