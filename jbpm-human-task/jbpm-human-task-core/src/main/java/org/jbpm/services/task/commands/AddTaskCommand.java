/*
 * Copyright 2012 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.services.task.commands;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.drools.core.xml.jaxb.util.JaxbMapAdapter;
import org.jbpm.services.task.impl.model.xml.JaxbTask;
import org.jbpm.services.task.rule.TaskRuleService;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.command.Context;
import org.kie.internal.task.api.TaskDeadlinesService;
import org.kie.internal.task.api.TaskDeadlinesService.DeadlineType;
import org.kie.internal.task.api.model.ContentData;
import org.kie.internal.task.api.model.Deadline;
import org.kie.internal.task.api.model.Deadlines;
import org.kie.internal.task.api.model.InternalPeopleAssignments;
import org.kie.internal.task.api.model.InternalTask;
import org.kie.internal.task.api.model.InternalTaskData;

/**
 * Operation.Start : [ new OperationCommand().{ status = [ Status.Ready ],
 * allowed = [ Allowed.PotentialOwner ], setNewOwnerToUser = true, newStatus =
 * Status.InProgress }, new OperationCommand().{ status = [ Status.Reserved ],
 * allowed = [ Allowed.Owner ], newStatus = Status.InProgress } ], *
 */
@XmlRootElement(name="add-task-command")
@XmlAccessorType(XmlAccessType.NONE)
public class AddTaskCommand extends UserGroupCallbackTaskCommand<Long> {

	private static final long serialVersionUID = 743368767949233891L;

	@XmlElement
    private JaxbTask jaxbTask;
    
    @XmlTransient
    private Task task;
    
    @XmlJavaTypeAdapter(JaxbMapAdapter.class)
    @XmlElement(name="parameter")
    private Map<String, Object> params;
    
    // TODO support ContentData marshalling
    @XmlTransient // remove and add @XmlElement when done
    private ContentData data;
    
    public AddTaskCommand() {
    }
  

    public AddTaskCommand(Task task, Map<String, Object> params) {
        setTask(task);
        this.params = params;
    }

    public AddTaskCommand(Task task, ContentData data) {
    	setTask(task);
        this.data = data;
    }

    public Long execute(Context cntxt) {
    	Long taskId = null;
        TaskContext context = (TaskContext) cntxt;

    	if (task == null) {
    		task = jaxbTask;
    	}
    	Task taskImpl = null;
    	if (task instanceof JaxbTask) {
    	    taskImpl = ((JaxbTask) task).getTask();
    	} else {
    		taskImpl = task;
    	}
	    initializeTask(taskImpl);
	    context.getTaskRuleService().executeRules(taskImpl, userId, data != null?data:params, TaskRuleService.ADD_TASK_SCOPE);
        doCallbackOperationForPeopleAssignments((InternalPeopleAssignments) taskImpl.getPeopleAssignments(), context);
        doCallbackOperationForTaskData((InternalTaskData) taskImpl.getTaskData(), context);
        doCallbackOperationForTaskDeadlines(((InternalTask) taskImpl).getDeadlines(), context);
        
	    if (data != null) {
	    	taskId = context.getTaskInstanceService().addTask(taskImpl, data);
        } else {
        	taskId = context.getTaskInstanceService().addTask(taskImpl, params);
        }      
    	
    	scheduleDeadlinesForTask((InternalTask) taskImpl, context.getTaskDeadlinesService());
    	
    	return taskId;
    }

    public JaxbTask getJaxbTask() {
        return jaxbTask;
    }

    public void setJaxbTask(JaxbTask jaxbTask) {
        this.jaxbTask = jaxbTask;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
    	this.task = task;
        if (task instanceof JaxbTask) {
        	this.jaxbTask = (JaxbTask) task;
        } else {
        	this.jaxbTask = new JaxbTask(task);
        }
    }

    public Map<String, Object> getParams() {
        return params;
    }
    
    public void setParams(Map<String, Object> params) {
    	this.params = params;
    }

    public ContentData getData() {
        return data;
    }

    public void setData(ContentData data) {
        this.data = data;
    }
    
    private void scheduleDeadlinesForTask(final InternalTask task, TaskDeadlinesService deadlineService) {
        final long now = System.currentTimeMillis();

        Deadlines deadlines = task.getDeadlines();
        
        if (deadlines != null) {
            final List<? extends Deadline> startDeadlines = deadlines.getStartDeadlines();
    
            if (startDeadlines != null) {
                scheduleDeadlines(startDeadlines, now, task.getId(), DeadlineType.START, deadlineService);
            }
    
            final List<? extends Deadline> endDeadlines = deadlines.getEndDeadlines();
    
            if (endDeadlines != null) {
                scheduleDeadlines(endDeadlines, now, task.getId(), DeadlineType.END, deadlineService);
            }
        }
    }

    private void scheduleDeadlines(final List<? extends Deadline> deadlines, final long now, 
    		final long taskId, DeadlineType type, TaskDeadlinesService deadlineService) {
        for (Deadline deadline : deadlines) {
            if (!deadline.isEscalated()) {
                // only escalate when true - typically this would only be true
                // if the user is requested that the notification should never be escalated
                Date date = deadline.getDate();
                deadlineService.schedule(taskId, deadline.getId(), date.getTime() - now, type);
            }
        }
    }
    
    private void initializeTask(Task task){
        Status assignedStatus = null;
            
        if (task.getPeopleAssignments() != null && task.getPeopleAssignments().getPotentialOwners() != null && task.getPeopleAssignments().getPotentialOwners().size() == 1) {
            // if there is a single potential owner, assign and set status to Reserved
            OrganizationalEntity potentialOwner = task.getPeopleAssignments().getPotentialOwners().get(0);
            // if there is a single potential user owner, assign and set status to Reserved
            if (potentialOwner instanceof User) {
            	((InternalTaskData) task.getTaskData()).setActualOwner((User) potentialOwner);

                assignedStatus = Status.Reserved;
            }
            //If there is a group set as potentialOwners, set the status to Ready ??
            if (potentialOwner instanceof Group) {

                assignedStatus = Status.Ready;
            }
        } else if (task.getPeopleAssignments() != null && task.getPeopleAssignments().getPotentialOwners() != null && task.getPeopleAssignments().getPotentialOwners().size() > 1) {
            // multiple potential owners, so set to Ready so one can claim.
            assignedStatus = Status.Ready;
        } else {
            //@TODO: we have no potential owners
        }

        if (assignedStatus != null) {
            ((InternalTaskData) task.getTaskData()).setStatus(assignedStatus);
        }
        
    }
}
