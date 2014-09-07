package org.jbpm.services.task.commands;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.drools.core.util.StringUtils;
import org.jbpm.services.task.exception.CannotAddTaskException;
import org.kie.api.task.model.Attachment;
import org.kie.api.task.model.Comment;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.User;
import org.kie.internal.command.Context;
import org.kie.internal.task.api.TaskContext;
import org.kie.internal.task.api.TaskModelProvider;
import org.kie.internal.task.api.model.Deadline;
import org.kie.internal.task.api.model.Deadlines;
import org.kie.internal.task.api.model.Escalation;
import org.kie.internal.task.api.model.InternalOrganizationalEntity;
import org.kie.internal.task.api.model.InternalPeopleAssignments;
import org.kie.internal.task.api.model.InternalTaskData;
import org.kie.internal.task.api.model.Notification;
import org.kie.internal.task.api.model.Reassignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlTransient
@XmlRootElement(name="user-group-callback-task-command")
@XmlAccessorType(XmlAccessType.NONE)
public class UserGroupCallbackTaskCommand<T> extends TaskCommand<T> {

	private static final long serialVersionUID = 2675686383800457244L;
    private static final Logger logger = LoggerFactory.getLogger(UserGroupCallbackTaskCommand.class);  

    private Map<String, Boolean> userGroupsMap = new HashMap<String, Boolean>();
    private static Set<String> restrictedGroups = new HashSet<String>(); 
    
    public UserGroupCallbackTaskCommand() {
    	
    }

    static {
    	try {
            InputStream in = UserGroupCallbackTaskCommand.class.getResourceAsStream("/restricted-groups.properties");
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                
                restrictedGroups.addAll(props.stringPropertyNames());
            }
        } catch (Exception e) {
            logger.warn("Error when loading restricted groups for human task service {}", e.getMessage());
        }
	}
	    
    protected List<String> doUserGroupCallbackOperation(String userId, List<String> groupIds, TaskContext context) {

        doCallbackUserOperation(userId, context);
        doCallbackGroupsOperation(userId, groupIds, context);
        List<String> allGroupIds = null;

        return filterGroups(context.getUserGroupCallback().getGroupsForUser(userId, groupIds, allGroupIds));

    }

    protected boolean doCallbackUserOperation(String userId, TaskContext context) {

        if (userId != null && context.getUserGroupCallback().existsUser(userId)) {
            addUserFromCallbackOperation(userId, context);
            return true;
        }
        return false;

    }

    protected boolean doCallbackGroupOperation(String groupId, TaskContext context) {

        if (groupId != null && context.getUserGroupCallback().existsGroup(groupId) && !restrictedGroups.contains(groupId)) {
            addGroupFromCallbackOperation(groupId, context);
            return true;
        }
        return false;

    }

    protected void addUserFromCallbackOperation(String userId, TaskContext context) {
    	User user = context.getPersistenceContext().findUser(userId);
        boolean userExists = user != null;
        if (!StringUtils.isEmpty(userId) && !userExists) {
            user = TaskModelProvider.getFactory().newUser();
            ((InternalOrganizationalEntity) user).setId(userId);
            
            persistIfNotExists(user, context);
        } 
    }
    
    protected void persistIfNotExists(final OrganizationalEntity entity, TaskContext context) {
    	context.getPersistenceContext().persistOrgEntity(entity);
    }

    protected void doCallbackGroupsOperation(String userId, List<String> groupIds, TaskContext context) {

        if (userId != null) {

            if (groupIds != null && groupIds.size() > 0) {

                List<String> userGroups = filterGroups(context.getUserGroupCallback().getGroupsForUser(userId, groupIds, null));
                for (String groupId : groupIds) {

                    if (context.getUserGroupCallback().existsGroup(groupId) && userGroups != null && userGroups.contains(groupId)) {
                        addGroupFromCallbackOperation(groupId, context);
                    }
                }
            } else {
                if (!(userGroupsMap.containsKey(userId) && userGroupsMap.get(userId).booleanValue())) {
                    List<String> userGroups = filterGroups(context.getUserGroupCallback().getGroupsForUser(userId, null, null));
                    if (userGroups != null && userGroups.size() > 0) {
                        for (String group : userGroups) {
                            addGroupFromCallbackOperation(group, context);
                        }
                        userGroupsMap.put(userId, true);
                    }
                }
            }
        } else {
            if (groupIds != null) {
                for (String groupId : groupIds) {
                    addGroupFromCallbackOperation(groupId, context);
                }
            }
        }

    }

    protected void addGroupFromCallbackOperation(String groupId, TaskContext context) {
    	Group group = context.getPersistenceContext().findGroup(groupId);
    	boolean groupExists = group != null;
        if (!StringUtils.isEmpty(groupId) && !groupExists) {
        	group = TaskModelProvider.getFactory().newGroup();
            ((InternalOrganizationalEntity) group).setId(groupId);
            persistIfNotExists(group, context);
        }    
    }

    protected void doCallbackOperationForTaskData(InternalTaskData data, TaskContext context) {

        if (data.getActualOwner() != null) {
            boolean userExists = doCallbackUserOperation(data.getActualOwner().getId(), context);
            if (!userExists) {
                // remove it from the task to avoid foreign key constraint exception
                data.setActualOwner(null);
                data.setStatus(Status.Ready);
            }
        }

        if (data.getCreatedBy() != null) {
            boolean userExists = doCallbackUserOperation(data.getCreatedBy().getId(), context);
            if (!userExists) {
                // remove it from the task to avoid foreign key constraint exception
                data.setCreatedBy(null);
            }
        }

    }

    protected void doCallbackOperationForPotentialOwners(List<OrganizationalEntity> potentialOwners, TaskContext context) {

        List<OrganizationalEntity> nonExistingEntities = new ArrayList<OrganizationalEntity>();

        for (OrganizationalEntity orgEntity : potentialOwners) {
            if (orgEntity instanceof User) {
                boolean userExists = doCallbackUserOperation(orgEntity.getId(), context);
                if (!userExists) {
                    nonExistingEntities.add(orgEntity);
                }
            }
            if (orgEntity instanceof Group) {
                boolean groupExists = doCallbackGroupOperation(orgEntity.getId(), context);
                if (!groupExists) {
                    nonExistingEntities.add(orgEntity);
                }
            }
        }
        if (!nonExistingEntities.isEmpty()) {
            potentialOwners.removeAll(nonExistingEntities);
        }

    }

    protected void doCallbackOperationForPeopleAssignments(InternalPeopleAssignments assignments, TaskContext context) {

        List<OrganizationalEntity> nonExistingEntities = new ArrayList<OrganizationalEntity>();

        if (assignments != null) {
            List<? extends OrganizationalEntity> businessAdmins = assignments.getBusinessAdministrators();
            if (businessAdmins != null) {
                for (OrganizationalEntity admin : businessAdmins) {
                    if (admin instanceof User) {
                        boolean userExists = doCallbackUserOperation(admin.getId(), context);
                        if (!userExists) {
                            nonExistingEntities.add(admin);
                        }
                    }
                    if (admin instanceof Group) {
                        boolean groupExists = doCallbackGroupOperation(admin.getId(), context);
                        if (!groupExists) {
                            nonExistingEntities.add(admin);
                        }
                    }
                }

                if (!nonExistingEntities.isEmpty()) {
                    businessAdmins.removeAll(nonExistingEntities);
                    nonExistingEntities.clear();
                }
            }

            if (businessAdmins == null || businessAdmins.isEmpty()) {
                // throw an exception as it should not be allowed to create task without administrator
                throw new CannotAddTaskException("There are no known Business Administrators, task cannot be created according to WS-HT specification");
            }

            List<? extends OrganizationalEntity> potentialOwners = assignments.getPotentialOwners();
            if (potentialOwners != null) {
                for (OrganizationalEntity powner : potentialOwners) {
                    if (powner instanceof User) {
                        boolean userExists = doCallbackUserOperation(powner.getId(), context);
                        if (!userExists) {
                            nonExistingEntities.add(powner);
                        }
                    }
                    if (powner instanceof Group) {
                        boolean groupExists = doCallbackGroupOperation(powner.getId(), context);
                        if (!groupExists) {
                            nonExistingEntities.add(powner);
                        }
                    }
                }
                if (!nonExistingEntities.isEmpty()) {
                    potentialOwners.removeAll(nonExistingEntities);
                    nonExistingEntities.clear();
                }
            }

            if (assignments.getTaskInitiator() != null && assignments.getTaskInitiator().getId() != null) {
                doCallbackUserOperation(assignments.getTaskInitiator().getId(), context);
            }

            List<? extends OrganizationalEntity> excludedOwners = assignments.getExcludedOwners();
            if (excludedOwners != null) {
                for (OrganizationalEntity exowner : excludedOwners) {
                    if (exowner instanceof User) {
                        boolean userExists = doCallbackUserOperation(exowner.getId(), context);
                        if (!userExists) {
                            nonExistingEntities.add(exowner);
                        }
                    }
                    if (exowner instanceof Group) {
                        boolean groupExists = doCallbackGroupOperation(exowner.getId(), context);
                        if (!groupExists) {
                            nonExistingEntities.add(exowner);
                        }
                    }
                }
                if (!nonExistingEntities.isEmpty()) {
                    excludedOwners.removeAll(nonExistingEntities);
                    nonExistingEntities.clear();
                }
            }

            List<? extends OrganizationalEntity> recipients = assignments.getRecipients();
            if (recipients != null) {
                for (OrganizationalEntity recipient : recipients) {
                    if (recipient instanceof User) {
                        boolean userExists = doCallbackUserOperation(recipient.getId(), context);
                        if (!userExists) {
                            nonExistingEntities.add(recipient);
                        }
                    }
                    if (recipient instanceof Group) {
                        boolean groupExists = doCallbackGroupOperation(recipient.getId(), context);
                        if (!groupExists) {
                            nonExistingEntities.add(recipient);
                        }
                    }
                }
                if (!nonExistingEntities.isEmpty()) {
                    recipients.removeAll(nonExistingEntities);
                    nonExistingEntities.clear();
                }
            }

            List<? extends OrganizationalEntity> stakeholders = assignments.getTaskStakeholders();
            if (stakeholders != null) {
                for (OrganizationalEntity stakeholder : stakeholders) {
                    if (stakeholder instanceof User) {
                        boolean userExists = doCallbackUserOperation(stakeholder.getId(), context);
                        if (!userExists) {
                            nonExistingEntities.add(stakeholder);
                        }
                    }
                    if (stakeholder instanceof Group) {
                        boolean groupExists = doCallbackGroupOperation(stakeholder.getId(), context);
                        if (!groupExists) {
                            nonExistingEntities.add(stakeholder);
                        }
                    }
                }
                if (!nonExistingEntities.isEmpty()) {
                    stakeholders.removeAll(nonExistingEntities);
                    nonExistingEntities.clear();
                }
            }
        }


    }
     protected void doCallbackOperationForTaskDeadlines(Deadlines deadlines, TaskContext context) {
        if(deadlines != null) {
            if(deadlines.getStartDeadlines() != null) {
                List<? extends Deadline> startDeadlines = deadlines.getStartDeadlines();
                for(Deadline startDeadline : startDeadlines) {
                    List<? extends Escalation> escalations = startDeadline.getEscalations();
                    if(escalations != null) {
                        for(Escalation escalation : escalations) {
                            List<? extends Notification> notifications = escalation.getNotifications();
                            List<? extends Reassignment> ressignments = escalation.getReassignments();
                            if(notifications != null) {
                                for(Notification notification : notifications) {
                                    List<? extends OrganizationalEntity> recipients = notification.getRecipients();
                                    if(recipients != null) {
                                        for(OrganizationalEntity recipient : recipients) {
                                            if(recipient instanceof User) {
                                                doCallbackUserOperation(recipient.getId(), context);
                                            }
                                            if(recipient instanceof Group) {
                                                doCallbackGroupOperation(recipient.getId(), context);
                                            }
                                        }
                                    }
                                    List<? extends OrganizationalEntity> administrators = notification.getBusinessAdministrators();
                                    if(administrators != null) {
                                        for(OrganizationalEntity administrator : administrators) {
                                            if(administrator instanceof User) {
                                                doCallbackUserOperation(administrator.getId(), context);
                                            }
                                            if(administrator instanceof Group) {
                                                doCallbackGroupOperation(administrator.getId(), context);
                                            }
                                        }
                                    }
                                }
                            }
                            if(ressignments != null) {
                                for(Reassignment reassignment : ressignments) {
                                    List<? extends OrganizationalEntity> potentialOwners = reassignment.getPotentialOwners();
                                    if(potentialOwners != null) {
                                        for(OrganizationalEntity potentialOwner : potentialOwners) {
                                            if(potentialOwner instanceof User) {
                                                doCallbackUserOperation(potentialOwner.getId(), context);
                                            }
                                            if(potentialOwner instanceof Group) {
                                                doCallbackGroupOperation(potentialOwner.getId(), context);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if(deadlines.getEndDeadlines() != null) {
                List<? extends Deadline> endDeadlines = deadlines.getEndDeadlines();
                for(Deadline endDeadline : endDeadlines) {
                    List<? extends Escalation> escalations = endDeadline.getEscalations();
                    if(escalations != null) {
                        for(Escalation escalation : escalations) {
                            List<? extends Notification> notifications = escalation.getNotifications();
                            List<? extends Reassignment> ressignments = escalation.getReassignments();
                            if(notifications != null) {
                                for(Notification notification : notifications) {
                                    List<? extends OrganizationalEntity> recipients = notification.getRecipients();
                                    if(recipients != null) {
                                        for(OrganizationalEntity recipient : recipients) {
                                            if(recipient instanceof User) {
                                                doCallbackUserOperation(recipient.getId(), context);
                                            }
                                            if(recipient instanceof Group) {
                                                doCallbackGroupOperation(recipient.getId(), context);
                                            }
                                        }
                                    }
                                    List<? extends OrganizationalEntity> administrators = notification.getBusinessAdministrators();
                                    if(administrators != null) {
                                        for(OrganizationalEntity administrator : administrators) {
                                            if(administrator instanceof User) {
                                                doCallbackUserOperation(administrator.getId(), context);
                                            }
                                            if(administrator instanceof Group) {
                                                doCallbackGroupOperation(administrator.getId(), context);
                                            }
                                        }
                                    }
                                }
                            }
                            if(ressignments != null) {
                                for(Reassignment reassignment : ressignments) {
                                    List<? extends OrganizationalEntity> potentialOwners = reassignment.getPotentialOwners();
                                    if(potentialOwners != null) {
                                        for(OrganizationalEntity potentialOwner : potentialOwners) {
                                            if(potentialOwner instanceof User) {
                                                doCallbackUserOperation(potentialOwner.getId(), context);
                                            }
                                            if(potentialOwner instanceof Group) {
                                                doCallbackGroupOperation(potentialOwner.getId(), context);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
     
     protected void doCallbackOperationForComment(Comment comment, TaskContext context) {
         if (comment != null) {
             if (comment.getAddedBy() != null) {
                 doCallbackUserOperation(comment.getAddedBy().getId(), context);
             }
         }
     }
     
     protected void doCallbackOperationForAttachment(Attachment attachment, TaskContext context) {
         if (attachment != null) {
             if (attachment.getAttachedBy() != null) {
                 doCallbackUserOperation(attachment.getAttachedBy().getId(), context);
             }
         }
     }
     
     protected List<String> filterGroups(List<String> groups) {
         if (groups != null) {
             groups.removeAll(restrictedGroups);
         }
         
         return groups;
     }

	@Override
	public T execute(Context context) {
		return null;
	}

}
