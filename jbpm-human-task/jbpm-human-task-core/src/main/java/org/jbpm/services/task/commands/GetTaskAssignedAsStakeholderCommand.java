package org.jbpm.services.task.commands;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.kie.api.task.model.TaskSummary;
import org.kie.internal.command.Context;

@XmlRootElement(name = "get-task-assigned-as-stakeholder-command")
@XmlAccessorType(XmlAccessType.NONE)
public class GetTaskAssignedAsStakeholderCommand extends UserGroupCallbackTaskCommand<List<TaskSummary>> {

    private static final long serialVersionUID = 5077599352603072633L;

    public GetTaskAssignedAsStakeholderCommand() {
    }

    public GetTaskAssignedAsStakeholderCommand(String userId) {
        this.userId = userId;

    }

    public List<TaskSummary> execute(Context cntxt) {
        TaskContext context = (TaskContext) cntxt;
        doCallbackUserOperation(userId, context);
        doUserGroupCallbackOperation(userId, null, context);
        return context.getTaskQueryService().getTasksAssignedAsTaskStakeholder(userId);

    }

}
