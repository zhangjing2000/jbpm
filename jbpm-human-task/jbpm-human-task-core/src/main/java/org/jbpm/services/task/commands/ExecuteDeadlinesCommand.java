package org.jbpm.services.task.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;

import org.jbpm.services.task.deadlines.NotificationListener;
import org.jbpm.services.task.deadlines.notifications.impl.email.EmailNotificationListener;
import org.jbpm.services.task.utils.ContentMarshallerHelper;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.task.model.Content;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskData;
import org.kie.internal.command.Context;
import org.kie.internal.task.api.ContentMarshallerContext;
import org.kie.internal.task.api.TaskDeadlinesService.DeadlineType;
import org.kie.internal.task.api.TaskPersistenceContext;
import org.kie.internal.task.api.UserInfo;
import org.kie.internal.task.api.model.Deadline;
import org.kie.internal.task.api.model.Escalation;
import org.kie.internal.task.api.model.InternalPeopleAssignments;
import org.kie.internal.task.api.model.InternalTaskData;
import org.kie.internal.task.api.model.Notification;
import org.kie.internal.task.api.model.NotificationEvent;
import org.kie.internal.task.api.model.NotificationType;
import org.kie.internal.task.api.model.Reassignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name = "execute-deadlines-command")
@XmlAccessorType(XmlAccessType.NONE)
public class ExecuteDeadlinesCommand extends TaskCommand<Void> {

	private static final long serialVersionUID = 3140157192156956692L;
	private static final Logger logger = LoggerFactory
			.getLogger(ExecuteDeadlinesCommand.class);

	@XmlElement
	@XmlSchemaType(name = "long")
	private Long deadlineId;
	@XmlElement
	private DeadlineType type;
	@XmlTransient
	private NotificationListener notificationListener;

	public ExecuteDeadlinesCommand() {

	}

	public ExecuteDeadlinesCommand(long taskId, long deadlineId,
			DeadlineType type) {
		this.taskId = taskId;
		this.deadlineId = deadlineId;
		this.type = type;
	}

	public ExecuteDeadlinesCommand(long taskId, long deadlineId,
			DeadlineType type, NotificationListener notificationListener) {
		this.taskId = taskId;
		this.deadlineId = deadlineId;
		this.type = type;
		this.notificationListener = notificationListener;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Void execute(Context context) {
		TaskContext ctx = (TaskContext) context;
		if (notificationListener == null) {
			this.notificationListener = new EmailNotificationListener(
					(UserInfo) context.get(EnvironmentName.TASK_USER_INFO));
		}

		TaskPersistenceContext persistenceContext = ctx.getPersistenceContext();

		try {
			Task task = persistenceContext.findTask(taskId);
			Deadline deadline = persistenceContext.findDeadline(deadlineId);
			if (task == null || deadline == null) {
				return null;
			}
			TaskData taskData = task.getTaskData();

			if (taskData != null) {
				// check if task is still in valid status
				if (type.isValidStatus(taskData.getStatus())) {
					Map<String, Object> variables = null;

					Content content = persistenceContext.findContent(taskData
							.getDocumentContentId());

					if (content != null) {
						ContentMarshallerContext mContext = ctx
								.getTaskContentService().getMarshallerContext(
										task);
						Object objectFromBytes = ContentMarshallerHelper
								.unmarshall(content.getContent(),
										mContext.getEnvironment(),
										mContext.getClassloader());

						if (objectFromBytes instanceof Map) {
							variables = (Map<String, Object>) objectFromBytes;

						} else {

							variables = new HashMap<String, Object>();
							variables.put("content", objectFromBytes);
						}
					} else {
						variables = Collections.emptyMap();
					}

					if (deadline == null || deadline.getEscalations() == null) {
						return null;
					}

					for (Escalation escalation : deadline.getEscalations()) {

						// we won't impl constraints for now
						// escalation.getConstraints()

						// run reassignment first to allow notification to be
						// send to new potential owners
						if (!escalation.getReassignments().isEmpty()) {
							// get first and ignore the rest.
							Reassignment reassignment = escalation
									.getReassignments().get(0);
							logger.debug("Reassigning to {}",
									reassignment.getPotentialOwners());
							((InternalTaskData) task.getTaskData())
									.setStatus(Status.Ready);

							List<OrganizationalEntity> potentialOwners = new ArrayList<OrganizationalEntity>(
									reassignment.getPotentialOwners());
							((InternalPeopleAssignments) task
									.getPeopleAssignments())
									.setPotentialOwners(potentialOwners);
							((InternalTaskData) task.getTaskData())
									.setActualOwner(null);

						}
						for (Notification notification : escalation
								.getNotifications()) {
							if (notification.getNotificationType() == NotificationType.Email) {
								logger.debug("Sending an Email");
								notificationListener
										.onNotification(new NotificationEvent(
												notification, task, variables));
							}
						}
					}
				}

			}

			deadline.setEscalated(true);
			persistenceContext.updateDeadline(deadline);
			persistenceContext.updateTask(task);
		} catch (Exception e) {

			logger.error("Error when executing deadlines", e);
		}
		return null;
	}

}
