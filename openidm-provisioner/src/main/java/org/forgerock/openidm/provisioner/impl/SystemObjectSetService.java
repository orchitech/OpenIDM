/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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
package org.forgerock.openidm.provisioner.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.fluent.JsonValueException;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.ServiceUnavailableException;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.core.ServerConstants;
import org.forgerock.openidm.provisioner.ConnectorConfigurationHelper;
import org.forgerock.openidm.provisioner.Id;
import org.forgerock.openidm.provisioner.ProvisionerService;
import org.forgerock.openidm.provisioner.SystemIdentifier;
import org.forgerock.openidm.quartz.impl.ExecutionException;
import org.forgerock.openidm.quartz.impl.ScheduledService;
import org.forgerock.openidm.router.RouteService;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * SystemObjectSetService implement the {@link SingletonResourceProvider}.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@Component(name = "org.forgerock.openidm.provisioner",
        policy = ConfigurationPolicy.IGNORE,
        metatype = true,
        description = "OpenIDM System Object Set Service",
        immediate = true)
@Service(value = {ScheduledService.class, SingletonResourceProvider.class})
@Properties({
        @Property(name = Constants.SERVICE_VENDOR, value = ServerConstants.SERVER_VENDOR_NAME),
        @Property(name = Constants.SERVICE_DESCRIPTION, value = "OpenIDM System Object Set Service"),
        @Property(name = ServerConstants.ROUTER_PREFIX, value = SystemObjectSetService.ROUTER_PREFIX)
})
public class SystemObjectSetService implements ScheduledService, SingletonResourceProvider {

    private final static Logger logger = LoggerFactory.getLogger(SystemObjectSetService.class);

    public static final String ROUTER_PREFIX =  "/system";

    /**
     * Actions supported on this resource provider
     */
    public enum SystemAction {
        /** Captures the changes on a remote system, the pushes those changes to OpenIDM */
        activeSync,
        /** Captures the changes on a remote system, the pushes those changes to OpenIDM */
        liveSync,
        /** Test a connector to see if the connection is available */
        test,
        /** Test an existing connector configuration */
        testConfig,
        /** Multi phase configuration event calls this to generate the response */
        createConfiguration,
        /** List the connector [types] available in the system */
        availableConnectors,
        /** Generates the core configuration for a connector */
        createCoreConfig,
        /** Generates the full configuration for a connector */
        createFullConfig;

        private static Set<SystemAction> requiringConnectorConfigurationHelper = EnumSet.of(
                createConfiguration, availableConnectors, createCoreConfig, createFullConfig);

        /** Checks to see that ConnectorConfigurationHelper is available */
        boolean requiresConnectorConfigurationHelper() {
            return requiringConnectorConfigurationHelper.contains(this);
        }

        private static Set<SystemAction> liveSyncActions = EnumSet.of(activeSync, liveSync);

        /** Checks to see if action is live sync */
        boolean isLiveSync() {
            return liveSyncActions.contains(this);
        }
    }

    @Reference(referenceInterface = ProvisionerService.class,
            cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
            bind = "bindProvisionerService",
            unbind = "unbindProvisionerService",
            policy = ReferencePolicy.DYNAMIC,
            strategy = ReferenceStrategy.EVENT)
    private Map<SystemIdentifier, ProvisionerService> provisionerServices = new HashMap<SystemIdentifier, ProvisionerService>();

    protected void bindProvisionerService(ProvisionerService service, Map properties) {
        provisionerServices.put(service.getSystemIdentifier(), service);
    }

    protected void unbindProvisionerService(ProvisionerService service, Map properties) {
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (service.equals(entry.getValue())) {
                provisionerServices.remove(entry.getKey());
                break;
            }
        }
    }

    @Reference(target = "("+ServerConstants.ROUTER_PREFIX + "=/*)")
    RouteService routeService;
    ServerContext routerContext = null;

    private void bindRouteService(final RouteService service) throws ResourceException {
        routeService = service;
        routerContext = service.createServerContext();
    }

    private void unbindRouteService(final RouteService service) {
        routeService = null;
        routerContext = null;
    }

    /** The Connection Factory */
    @Reference(policy = ReferencePolicy.STATIC, target="(service.pid=org.forgerock.openidm.internal)")
    protected ConnectionFactory connectionFactory;

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY, policy = ReferencePolicy.DYNAMIC)
    private ConnectorConfigurationHelper connectorConfigurationHelper;

    @Override
    public void actionInstance(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            final ProvisionerService ps;
            final JsonValue content = request.getContent();
            final JsonValue id = content.get("id");
            final SystemAction action = request.getActionAsEnum(SystemAction.class);
            if (action.requiresConnectorConfigurationHelper() && null == connectorConfigurationHelper) {
                throw new ServiceUnavailableException("The required service is not available");
            }
            switch (action) {
            case createConfiguration:
                handler.handleResult(connectorConfigurationHelper.configure(content));
                break;
            case testConfig:
                JsonValue config = content;
                if (!id.isNull()) {
                    throw new BadRequestException("A system ID must not be specified in the request");
                }
                ps = locateServiceForTest(config.get("name"));
                if (ps == null) {
                    throw new BadRequestException("Invalid configuration to test: no 'name' specified");
                }
                handler.handleResult(new JsonValue(ps.testConfig(config)));
                break;
            case test:
                ps = locateServiceForTest(id);
                if (ps != null) {
                    handler.handleResult(new JsonValue(ps.getStatus()));
                } else {
                    List<Object> list = new ArrayList<Object>();
                    for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
                        list.add(entry.getValue().getStatus());
                    }
                    handler.handleResult(new JsonValue(list));
                }
                break;
            case activeSync:
            case liveSync:
                JsonValue params = new JsonValue(request.getAdditionalParameters());
                String source = params.get("source").asString();
                if (source == null) {
                    logger.debug("liveSync requires an explicit source parameter, source is : {}", source );
                    throw new BadRequestException("liveSync action requires either an explicit source parameter, "
                            + "or needs to be called on a specific provisioner URI");
                } else {
                    logger.debug("liveSync called with explicit source parameter {}", source);
                }
                handler.handleResult(liveSync(source, Boolean.valueOf(params.get("detailedFailure").asString())));
                break;
            case availableConnectors:
                handler.handleResult(connectorConfigurationHelper.getAvailableConnectors());
                break;
            case createCoreConfig:
                handler.handleResult(connectorConfigurationHelper.generateConnectorCoreConfig(content));
                break;
            case createFullConfig:
                handler.handleResult(connectorConfigurationHelper.generateConnectorFullConfig(content));
                break;
            default:
                handler.handleError(new BadRequestException("Unsupported actionId: " + request.getAction()));
                return;
            }
        } catch (ResourceException e) {
            handler.handleError(e);
        } catch (IllegalArgumentException e) {
            // from getActionAsEnum
            handler.handleError(new BadRequestException(e.getMessage(), e));
        } catch (Exception e) {
            handler.handleError(new InternalServerErrorException(e));
        }
    }

    @Override
    public void readInstance(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Read are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void patchInstance(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Patch are not supported for resource instances");
        handler.handleError(e);
    }

    @Override
    public void updateInstance(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        final ResourceException e =
                new NotSupportedException("Update are not supported for resource instances");
        handler.handleError(e);
    }

    /**
     * Invoked by the scheduler when the scheduler triggers.
     *
     * @param schedulerContext Context information passed by the scheduler service
     * @throws org.forgerock.openidm.quartz.impl.ExecutionException
     *          if execution of the scheduled work failed.
     *          Implementations can also throw RuntimeExceptions which will get logged.
     */
    public void execute(ServerContext context, Map<String, Object> schedulerContext) throws ExecutionException {
        try {
            JsonValue params = new JsonValue(schedulerContext).get(CONFIGURED_INVOKE_CONTEXT);
            if (params.get("action").asEnum(SystemAction.class).isLiveSync()) {
                String source = params.get("source").required().asString();
                liveSync(source, true);
            }
        } catch (JsonValueException jve) {
            throw new ExecutionException(jve);
        } catch (ResourceException e) {
            throw new ExecutionException(e);
        } catch (IllegalArgumentException e) {
            // not a liveSync action, so no-op
        } catch (RuntimeException e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * Live sync the specified provisioner resource
     * @param source the URI of the provisioner instance to live sync
     * @param detailedFailure whether in the case of failures additional details such as the
     * record content of where it failed should be included in the response
     */
    private JsonValue liveSync(String source, boolean detailedFailure) throws ResourceException {
        JsonValue response;
        Id id = new Id(source);
        String previousStageResourceContainer = "repo/synchronisation/pooledSyncStage";
        String previousStageId = id.toString().replace("/", "").toUpperCase();
        Resource previousStage = null;
        try {
            ReadRequest readRequest = Requests.newReadRequest(previousStageResourceContainer, previousStageId);
            previousStage = connectionFactory.getConnection().read(routerContext, readRequest);

            response = locateService(id).liveSynchronize(id.getObjectType(),
                    previousStage != null && previousStage.getContent() != null ? previousStage.getContent() : null);
            UpdateRequest updateRequest = Requests.newUpdateRequest(previousStageResourceContainer, previousStageId, response);
            updateRequest.setRevision(previousStage.getRevision());
            connectionFactory.getConnection().update(routerContext, updateRequest);
        } catch (ResourceException e) { // NotFoundException?
            if (null == previousStage) {
				response = locateService(id).liveSynchronize(id.getObjectType(), null);
                CreateRequest createRequest = Requests.newCreateRequest(previousStageResourceContainer, previousStageId, response);
                connectionFactory.getConnection().create(routerContext, createRequest);
            }
            else {
                throw e;
            }
        }
        if (response != null && !detailedFailure) {
            // The detailedFailure option handling ideally should move into provisioners
            response.get("lastException").remove("syncDelta");
        }
        return response;
    }

    private ProvisionerService locateService(Id identifier) throws ResourceException {
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (entry.getKey().is(identifier)) {
                return entry.getValue();
            }
        }
        throw new ServiceUnavailableException("System: " + identifier + " is not available.");
    }

    private ProvisionerService locateServiceForTest(JsonValue requestId) throws ResourceException {
        if (requestId.isNull()) {
            return null;
        }

        Id id = new Id(requestId.asString() + "/test");
        for (Map.Entry<SystemIdentifier, ProvisionerService> entry : provisionerServices.entrySet()) {
            if (entry.getKey().is(id)) {
                return entry.getValue();
            }
        }

        throw new NotFoundException("System: " + requestId.asString() + " is not available.");
    }
}
