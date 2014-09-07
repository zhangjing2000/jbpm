package org.jbpm.services.task.events;

import java.util.Iterator;

import org.drools.core.event.AbstractEventSupport;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.model.Task;
import org.kie.internal.task.api.TaskContext;
import org.kie.internal.task.api.TaskPersistenceContext;

public class TaskEventSupport extends AbstractEventSupport<TaskLifeCycleEventListener> {

    public void fireBeforeTaskActivated(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskActivatedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskClaimed(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskClaimedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskSkipped(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskSkippedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskStarted(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskStartedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskStopped(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskStoppedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskCompleted(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskCompletedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskFailed(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskFailedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskAdded(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskAddedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskExited(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskExitedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskReleased(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskReleasedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskResumed(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskResumedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskSuspended(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskSuspendedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskForwarded(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskForwardedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskDelegated(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskDelegatedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireBeforeTaskNominated(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().beforeTaskNominatedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    // after methods
    
    public void fireAfterTaskActivated(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskActivatedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskClaimed(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskClaimedEvent(new TaskEventImpl(task, context));;
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskSkipped(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskSkippedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskStarted(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskStartedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskStopped(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskStoppedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskCompleted(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskCompletedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskFailed(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskFailedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskAdded(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskAddedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskExited(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskExitedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskReleased(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskReleasedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskResumed(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskResumedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskSuspended(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskSuspendedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskForwarded(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskForwardedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskDelegated(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskDelegatedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
    
    public void fireAfterTaskNominated(final Task task, TaskContext context) {
        final Iterator<TaskLifeCycleEventListener> iter = getEventListenersIterator();
        if (iter.hasNext()) {
            do{
                iter.next().afterTaskNominatedEvent(new TaskEventImpl(task, context));
            } while (iter.hasNext());
        }
    }
}
