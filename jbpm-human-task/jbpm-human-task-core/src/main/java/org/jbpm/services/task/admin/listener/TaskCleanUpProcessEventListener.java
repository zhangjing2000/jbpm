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
package org.jbpm.services.task.admin.listener;

import java.util.ArrayList;
import java.util.List;

import org.drools.core.event.DefaultProcessEventListener;
import org.jbpm.services.task.admin.listener.internal.GetCurrentTxTasksCommand;
import org.jbpm.services.task.commands.GetTasksForProcessCommand;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.task.api.EventService;
import org.kie.internal.task.api.InternalTaskService;


public class TaskCleanUpProcessEventListener extends DefaultProcessEventListener {

    private InternalTaskService taskService;
    
    @SuppressWarnings("unchecked")
	public TaskCleanUpProcessEventListener(TaskService taskService) {
        this.taskService = (InternalTaskService) taskService;
        if (taskService instanceof EventService<?>) {
        	boolean alreadyRegistered = false;
        	List<?> listeners = ((EventService<?>) taskService).getTaskEventListeners();
        	if (listeners != null) {
        		for (Object listener : listeners) {
        			if (listener instanceof ContextStorageTaskEventListener) {
        				alreadyRegistered = true;
        			}
        		}
        	}
        	if (!alreadyRegistered) {
        		((EventService<TaskLifeCycleEventListener>) taskService).registerTaskEventListener(new ContextStorageTaskEventListener());
        	}
        }
    }

 
    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {        
        List<Status> statuses = new ArrayList<Status>();
        statuses.add(Status.Error);
        statuses.add(Status.Failed);
        statuses.add(Status.Obsolete);
        statuses.add(Status.Suspended);
        statuses.add(Status.Completed);
        statuses.add(Status.Exited);
        List<TaskSummary> completedTasksByProcessId = ((InternalTaskService)taskService).execute(new GetTasksForProcessCommand(event.getProcessInstance().getId(), statuses, "en-UK"));
        // include tasks from current transaction
        List<TaskSummary> currentTxTasks = taskService.execute(new GetCurrentTxTasksCommand(event.getProcessInstance().getId()));
        completedTasksByProcessId.addAll(currentTxTasks);
        // archive and remove
        taskService.archiveTasks(completedTasksByProcessId);
        taskService.removeTasks(completedTasksByProcessId);
    }
   
}
