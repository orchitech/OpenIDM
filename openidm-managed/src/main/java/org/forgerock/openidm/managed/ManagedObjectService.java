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

package org.forgerock.openidm.managed;

// Java Standard Edition
import java.util.HashMap;
import java.util.List;

// OSGi Framework
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

// Apache Felix Maven SCR Plugin
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;

// JSON-Fluent library
import org.forgerock.json.fluent.JsonNode;
import org.forgerock.json.fluent.JsonNodeException;

// ForgeRock OpenIDM
import org.forgerock.openidm.objset.ObjectSet;
import org.forgerock.openidm.objset.ObjectSetRouter;
import org.forgerock.openidm.objset.ServiceUnavailableException;
import org.forgerock.openidm.repo.RepositoryService; 
import org.forgerock.openidm.config.JSONEnhancedConfig;

/**
 * Provides access to managed objects.
 *
 * @author Paul C. Bryan
 */
@Component(name="org.forgerock.openidm.managed", immediate=true)
@Service
@Properties({
    @Property(name=Constants.SERVICE_DESCRIPTION, value="OpenIDM managed objects service"),
    @Property(name=Constants.SERVICE_VENDOR, value="ForgeRock AS")
})
public class ManagedObjectService extends ObjectSetRouter {

    /** Repository service. */
    @Reference(
        name="RepositoryServiceReference",
        referenceInterface=RepositoryService.class,
        bind="bind",
        unbind="unbind",
        cardinality=ReferenceCardinality.MANDATORY_UNARY,
        policy=ReferencePolicy.STATIC
    )
    private RepositoryService repository;

    /** TODO: Description. */
    private ComponentContext context;

    /**
     * TODO: Description.
     *
     * @param repository TODO.
     */
    protected void bind(RepositoryService repository) {
        this.repository = repository;
    }

    /**
     * TODO: Description.
     *
     * @param repository TODO.
     */
    protected void unbind(RepositoryService repository) {
        this.repository = null;
    }

    /**
     * TODO: Description.
     *
     * @param context TODO.
     */
    @Activate
    protected void activate(ComponentContext context) {
        this.context = context;
        JsonNode config = new JsonNode(new JSONEnhancedConfig().getConfiguration(context));
        try {
            for (JsonNode node : config.get("objects").expect(List.class)) {
                ManagedObjectSet objectSet = new ManagedObjectSet(this, node); // throws JsonNodeException
                String name = objectSet.getName();
                if (routes.containsKey(name)) {
                    throw new JsonNodeException(node, "object " + name + " already defined");
                }
                routes.put(name, objectSet);
            }
        }
        catch (JsonNodeException jne) {
            throw new ComponentException("Configuration error", jne);
        }
    }

    /**
     * TODO: Description.
     *
     * @param context TODO.
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        this.context = null;
        routes.clear();
    }

    /**
     * Returns the repository for which operations will be applied. If there is no
     * repository, throws {@link ServiceUnavailableException}.
     */
    ObjectSet getRepository() throws ServiceUnavailableException {
        if (repository == null) {
            throw new ServiceUnavailableException("not bound to repository");
        }
        return repository;
    }
}
