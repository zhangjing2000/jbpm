package org.jbpm.services.task.audit.commands;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.jbpm.services.task.commands.TaskCommand;
import org.jbpm.services.task.audit.impl.model.BAMTaskSummaryImpl;
import org.jbpm.services.task.utils.ClassUtil;
import org.kie.internal.command.Context;
import org.kie.internal.task.api.TaskContext;
import org.kie.internal.task.api.TaskPersistenceContext;

@XmlRootElement(name="get-bam-task-summaries-for-task-command")
@XmlAccessorType(XmlAccessType.NONE)
public class GetBAMTaskSummariesCommand extends TaskCommand<List<BAMTaskSummaryImpl>> {

	private static final long serialVersionUID = -7929370526623674312L;

	public GetBAMTaskSummariesCommand() {
		
	}
	
	public GetBAMTaskSummariesCommand(long taskId) {
		this.taskId = taskId;
	}
	
	@Override
	public List<BAMTaskSummaryImpl> execute(Context context) {
		TaskPersistenceContext persistenceContext = ((TaskContext) context).getPersistenceContext();
		if( this.taskId != null ) { 
		    return persistenceContext.queryWithParametersInTransaction("getAllBAMTaskSummaries", 
		            persistenceContext.addParametersToMap("taskId", taskId),
		            ClassUtil.<List<BAMTaskSummaryImpl>>castClass(List.class));
		} else { 
		    return persistenceContext.queryStringInTransaction("FROM BAMTaskSummaryImpl",
		            ClassUtil.<List<BAMTaskSummaryImpl>>castClass(List.class));
		}
	}

}
