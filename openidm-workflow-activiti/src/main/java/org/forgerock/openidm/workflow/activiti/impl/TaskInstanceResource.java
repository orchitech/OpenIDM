/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright © 2012-2015 ForgeRock AS. All rights reserved.
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
package org.forgerock.openidm.workflow.activiti.impl;

import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.task.IdentityLink;
import org.forgerock.openidm.workflow.activiti.ActivitiConstants;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.task.DelegationState;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.util.ResourceUtil;
import org.forgerock.openidm.workflow.activiti.impl.mixin.TaskEntityMixIn;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * Resource implementation of TaskInstance related Activiti operations
 */
public class TaskInstanceResource implements CollectionResourceProvider {

    private final static ObjectMapper mapper;
    private ProcessEngine processEngine;

    static {
        mapper = new ObjectMapper();
        mapper.getSerializationConfig().addMixInAnnotations(TaskEntity.class, TaskEntityMixIn.class);
        mapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY, true);
    }

    public TaskInstanceResource(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public void setProcessEngine(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(ResourceUtil.notSupportedOnCollection(request));
    }

    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request, ResultHandler<JsonValue> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskService taskService = processEngine.getTaskService();
            Task task = processEngine.getTaskService().createTaskQuery().taskId(resourceId).singleResult();
            if (task == null) {
                handler.handleError(new NotFoundException());
            } else {
                if ("claim".equals(request.getAction())) {
                    taskService.claim(resourceId, request.getContent().expect(Map.class).asMap().get("userId").toString());
                } else if ("complete".equals(request.getAction())) {
                    taskService.complete(resourceId, request.getContent().expect(Map.class).asMap());
                } else {
                    handler.handleError(new BadRequestException("Unknown action"));
                }
                Map<String, String> result = new HashMap<String, String>(1);
                result.put("Task action performed", request.getAction());
                handler.handleResult(new JsonValue(result));
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    @Override
    public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request, ResultHandler<Resource> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());

            Task task = processEngine.getTaskService().createTaskQuery().taskId(resourceId).singleResult();
            if (task == null) {
                handler.handleError(new NotFoundException("Task " + resourceId + " not found."));
                return;
            }

            Map<String, Object> deletedTask = mapper.convertValue(task, Map.class);
            processEngine.getTaskService()
                    .deleteTask(resourceId, request.getAdditionalParameter(ActivitiConstants.ACTIVITI_DELETEREASON));
            handler.handleResult(new Resource(task.getId(), null, new JsonValue(deletedTask)));
        } catch (ActivitiObjectNotFoundException ex) {
            handler.handleError(new NotFoundException(ex.getMessage()));
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(ResourceUtil.notSupportedOnInstance(request));
    }

    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskQuery query = processEngine.getTaskService().createTaskQuery();
            if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())
                    || ActivitiConstants.QUERY_ALL_IDS.equals(request.getQueryId())) {
                if (ActivitiConstants.QUERY_FILTERED.equals(request.getQueryId())) {
                    setTaskParams(query, request);
                }
                setSortKeys(query, request);
                List<Task> list = query.list();
                for (Task taskInstance : list) {
                    Map value = mapper.convertValue(taskInstance, HashMap.class);
                    Resource r = new Resource(taskInstance.getId(), null, new JsonValue(value));
                    if (taskInstance.getDelegationState() == DelegationState.PENDING) {
                        r.getContent().add(ActivitiConstants.ACTIVITI_DELEGATE, taskInstance.getAssignee());
                    } else {
                        r.getContent().add(ActivitiConstants.ACTIVITI_ASSIGNEE, taskInstance.getAssignee());
                    }
                    handler.handleResource(r);
                }
                handler.handleResult(new QueryResult());
            } else {
                handler.handleError(new BadRequestException("Unknown query-id"));
            }
        } catch (NotSupportedException e) {
            handler.handleError(e);
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    @Override
    public void readInstance(ServerContext context, String resourceId, ReadRequest request, ResultHandler<Resource> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            TaskQuery query = processEngine.getTaskService().createTaskQuery();
            query.taskId(resourceId);
            Task task = query.singleResult();
            if (task == null) {
                handler.handleError(new NotFoundException());
            } else {
                Map value = mapper.convertValue(task, HashMap.class);
                TaskFormData data = processEngine.getFormService().getTaskFormData(task.getId());
                List<Map> propertyValues = new ArrayList<Map>();
                for (FormProperty p : data.getFormProperties()) {
                    Map<String, String> entry = new HashMap<String, String>();
                    entry.put(p.getId(), p.getValue());
                    propertyValues.add(entry);
                }
                value.put(ActivitiConstants.FORMPROPERTIES, propertyValues);

                if (task.getDelegationState() == DelegationState.PENDING) {
                    value.put(ActivitiConstants.ACTIVITI_DELEGATE, task.getAssignee());
                } else {
                    value.put(ActivitiConstants.ACTIVITI_ASSIGNEE, task.getAssignee());
                }
                Map<String, Object> variables = processEngine.getTaskService().getVariables(task.getId());
                if (variables.containsKey(ActivitiConstants.OPENIDM_CONTEXT)){
                    variables.remove(ActivitiConstants.OPENIDM_CONTEXT);
                }

                value.put(ActivitiConstants.ACTIVITI_VARIABLES, variables);
                value.put("candidates", getCandidateIdentities(task).getObject());

                handler.handleResult(new Resource(task.getId(), null, new JsonValue(value)));
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    /**
     * Retrieves candidate users and groups from a Task.
     *
     * @param task Task that needs to be searched
     * @return JsonValue of candidates
     */
    private JsonValue getCandidateIdentities(Task task) {
        JsonValue candidates = json(object())
                .add("candidateUsers", new HashSet<>())
                .add("candidateGroups", new HashSet<>());
        List<IdentityLink> candidateIdentity = processEngine.getTaskService().getIdentityLinksForTask(task.getId());
        for (IdentityLink identityLink : candidateIdentity) {
            if (identityLink.getUserId() != null) {
                candidates.get("candidateUsers").asSet().add(identityLink.getUserId());
            }
            if (identityLink.getGroupId() != null) {
                candidates.get("candidateGroups").asSet().add(identityLink.getGroupId());
            }
        }
        return candidates;
    }

    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request, ResultHandler<Resource> handler) {
        try {
            Authentication.setAuthenticatedUserId(context.asContext(SecurityContext.class).getAuthenticationId());
            Task task = processEngine.getTaskService().createTaskQuery().taskId(resourceId).singleResult();
            if (task == null) {
                handler.handleError(new NotFoundException());
            } else {
                Map value = request.getContent().expect(Map.class).asMap();
                if (value.containsKey(ActivitiConstants.ACTIVITI_ASSIGNEE)) {
                    task.setAssignee(value.get(ActivitiConstants.ACTIVITI_ASSIGNEE) == null
                            ? null : value.get(ActivitiConstants.ACTIVITI_ASSIGNEE).toString());
                }
                if (value.get(ActivitiConstants.ACTIVITI_DESCRIPTION) != null) {
                    task.setDescription(value.get(ActivitiConstants.ACTIVITI_DESCRIPTION).toString());
                }
                if (value.get(ActivitiConstants.ACTIVITI_NAME) != null) {
                    task.setName(value.get(ActivitiConstants.ACTIVITI_NAME).toString());
                }
                if (value.get(ActivitiConstants.ACTIVITI_OWNER) != null) {
                    task.setOwner(value.get(ActivitiConstants.ACTIVITI_OWNER).toString());
                }
                processEngine.getTaskService().saveTask(task);
                Map<String, String> result = new HashMap<String, String>(1);
                result.put("Task updated", resourceId);
                handler.handleResult(new Resource(resourceId, null, new JsonValue(result)));
            }
        } catch (Exception ex) {
            handler.handleError(new InternalServerErrorException(ex.getMessage(), ex));
        }
    }

    /**
     * Process the query parameters of the request and set it on the TaskQuery.
     *
     * @param query Query to update
     * @param request incoming request
     */
    private void setTaskParams(TaskQuery query, QueryRequest request) {

        for (Map.Entry<String, String> param : request.getAdditionalParameters().entrySet()) {
            switch (param.getKey()) {
                case ActivitiConstants.ACTIVITI_EXECUTIONID:
                    query.executionId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSDEFINITIONID:
                    query.processDefinitionId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSDEFINITIONKEY:
                    query.processDefinitionKey(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PROCESSINSTANCEID:
                    query.processInstanceId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_ASSIGNEE:
                    query.taskAssignee(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_CANDIDATEGROUP:
                    String taskCandidateGroup = param.getValue();
                    String[] taskCandidateGroups = taskCandidateGroup.split(",");
                    if (taskCandidateGroups.length > 1) {
                        query.taskCandidateGroupIn(Arrays.asList(taskCandidateGroups));
                    } else {
                        query.taskCandidateGroup(taskCandidateGroup);
                    }
                    break;
                case ActivitiConstants.ACTIVITI_CANDIDATEUSER:
                    query.taskCandidateUser(param.getValue());
                    break;
                case ActivitiConstants.ID:
                    query.taskId(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_NAME:
                    query.taskName(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_OWNER:
                    query.taskOwner(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_DESCRIPTION:
                    query.taskDescription(param.getValue());
                    break;
                case ActivitiConstants.ACTIVITI_PRIORITY:
                    query.taskPriority(Integer.parseInt(param.getValue()));
                    break;
                case ActivitiConstants.ACTIVITI_UNASSIGNED:
                    if (Boolean.parseBoolean(param.getValue())) {
                        query.taskUnassigned();
                    }
                    break;
                case ActivitiConstants.ACTIVITI_TENANTID:
                    query.taskTenantId(param.getValue());
                    break;
            }
        }

        Map<String, String> wfParams = ActivitiUtil.fetchVarParams(request);
        Iterator<Map.Entry<String, String>> itWf = wfParams.entrySet().iterator();
        while (itWf.hasNext()) {
            Map.Entry<String, String> e = itWf.next();
            query = query.processVariableValueEquals(e.getKey(), e.getValue());
        }
    }

    /**
     * Sets what the result set should be filtered by.
     *
     * @param query TaskQuery that needs to be modified for filtering
     * @param request incoming request
     * @throws NotSupportedException
     */
    private void setSortKeys(TaskQuery query, QueryRequest request) throws NotSupportedException {
        for (SortKey key : request.getSortKeys()) {
            if (key.getField() != null && !key.getField().isEmpty()) {
                switch (key.getField().toString().substring(1)) { // remove leading JsonPointer slash
                    case ActivitiConstants.ID:
                        query.orderByTaskId();
                        break;
                    case ActivitiConstants.ACTIVITI_NAME:
                        query.orderByTaskName();
                        break;
                    case ActivitiConstants.ACTIVITI_DESCRIPTION:
                        query.orderByTaskDescription();
                        break;
                    case ActivitiConstants.ACTIVITI_PRIORITY:
                        query.orderByTaskPriority();
                        break;
                    case ActivitiConstants.ACTIVITI_ASSIGNEE:
                        query.orderByTaskAssignee();
                        break;
                    case ActivitiConstants.ACTIVITI_CREATETIME:
                        query.orderByTaskCreateTime();
                        break;
                    case ActivitiConstants.ACTIVITI_PROCESSINSTANCEID:
                        query.orderByProcessInstanceId();
                        break;
                    case ActivitiConstants.ACTIVITI_EXECUTIONID:
                        query.orderByExecutionId();
                        break;
                    case ActivitiConstants.ACTIVITI_DUEDATE:
                        query.orderByDueDate();
                        break;
                    case ActivitiConstants.ACTIVITI_TENANTID:
                        query.orderByTenantId();
                        break;
                    default:
                        throw new NotSupportedException("Sort key: " + key.getField().toString().substring(1) + " is not valid");
                }
                query = key.isAscendingOrder() ? query.asc() : query.desc();
            }
        }

    }
}
