package org.jbpm.test.tasks.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.jbpm.process.instance.impl.demo.SystemOutWorkItemHandler;
import org.jbpm.services.task.admin.listener.TaskCleanUpProcessEventListener;
import org.jbpm.services.task.identity.DefaultUserInfo;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;
import org.kie.internal.task.api.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminAPIsWithListenerTest extends JbpmJUnitBaseTestCase {

    private static final Logger logger = LoggerFactory.getLogger(AdminAPIsWithListenerTest.class);

    private EntityManagerFactory emfTasks;
    protected UserInfo userInfo;
    protected Properties conf;

    public AdminAPIsWithListenerTest() {
        super(true, true);
    }

    
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        emfTasks = Persistence.createEntityManagerFactory("org.jbpm.services.task");
        userInfo = new DefaultUserInfo(null);
    }
    

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (emfTasks != null && emfTasks.isOpen()) {
            emfTasks.close();
        }
    }

 

    @Test
    public void automaticCleanUpTest() throws Exception {

        createRuntimeManager("patient-appointment.bpmn");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();

        KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);

        ksession.addEventListener(new TaskCleanUpProcessEventListener(taskService));

        logger.info("### Starting process ###");
        Map<String, Object> parameters = new HashMap<String, Object>();

        ProcessInstance process = ksession.startProcess("org.jbpm.PatientAppointment", parameters);
        long processInstanceId = process.getId();

        //The process is in the first Human Task waiting for its completion
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());

        //gets frontDesk's tasks
        List<TaskSummary> frontDeskTasks = taskService.getTasksAssignedAsPotentialOwner("frontDesk", "en-UK");
        Assert.assertEquals(1, frontDeskTasks.size());

        //doctor doesn't have any task
        List<TaskSummary> doctorTasks = taskService.getTasksAssignedAsPotentialOwner("doctor", "en-UK");
        Assert.assertTrue(doctorTasks.isEmpty());

        //manager doesn't have any task
        List<TaskSummary> managerTasks = taskService.getTasksAssignedAsPotentialOwner("manager", "en-UK");
        Assert.assertTrue(managerTasks.isEmpty());


        taskService.start(frontDeskTasks.get(0).getId(), "frontDesk");

        taskService.complete(frontDeskTasks.get(0).getId(), "frontDesk", null);

        //Now doctor has 1 task
        doctorTasks = taskService.getTasksAssignedAsPotentialOwner("doctor", "en-UK");
        Assert.assertEquals(1, doctorTasks.size());

        //No tasks for manager yet
        managerTasks = taskService.getTasksAssignedAsPotentialOwner("manager", "en-UK");
        Assert.assertTrue(managerTasks.isEmpty());


        taskService.start(doctorTasks.get(0).getId(), "doctor");

        taskService.complete(doctorTasks.get(0).getId(), "doctor", null);

        // tasks for manager 
        managerTasks = taskService.getTasksAssignedAsPotentialOwner("manager", "en-UK");
        Assert.assertEquals(1, managerTasks.size());
        taskService.start(managerTasks.get(0).getId(), "manager");

        taskService.complete(managerTasks.get(0).getId(), "manager", null);

        // since persisted process instance is completed it should be null
        process = ksession.getProcessInstance(process.getId());
        Assert.assertNull(process);


        final EntityManager em = emfTasks.createEntityManager();       
        Assert.assertEquals(0, em.createQuery("select t from TaskImpl t").getResultList().size());
        Assert.assertEquals(0, em.createQuery("select i from I18NTextImpl i").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_BAs").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_ExclOwners").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_PotOwners").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_Recipients").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_Stakeholders").getResultList().size());
        Assert.assertEquals(0, em.createQuery("select c from ContentImpl c").getResultList().size());
        em.close();
    }

    @Test
    public void automaticCleanUpTestAbortProcess() throws Exception {
    	
        createRuntimeManager("patient-appointment.bpmn");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();
        
        KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);

        ksession.addEventListener(new TaskCleanUpProcessEventListener(taskService));

        logger.info("### Starting process ###");
        Map<String, Object> parameters = new HashMap<String, Object>();

        ProcessInstance process = ksession.startProcess("org.jbpm.PatientAppointment", parameters);
        long processInstanceId = process.getId();

        //The process is in the first Human Task waiting for its completion
        Assert.assertEquals(ProcessInstance.STATE_ACTIVE, process.getState());

        //gets frontDesk's tasks
        List<TaskSummary> frontDeskTasks = taskService.getTasksAssignedAsPotentialOwner("frontDesk", "en-UK");
        Assert.assertEquals(1, frontDeskTasks.size());

        //doctor doesn't have any task
        List<TaskSummary> doctorTasks = taskService.getTasksAssignedAsPotentialOwner("doctor", "en-UK");
        Assert.assertTrue(doctorTasks.isEmpty());

        //manager doesn't have any task
        List<TaskSummary> managerTasks = taskService.getTasksAssignedAsPotentialOwner("manager", "en-UK");
        Assert.assertTrue(managerTasks.isEmpty());


        taskService.start(frontDeskTasks.get(0).getId(), "frontDesk");

        taskService.complete(frontDeskTasks.get(0).getId(), "frontDesk", null);

        //Now doctor has 1 task
        doctorTasks = taskService.getTasksAssignedAsPotentialOwner("doctor", "en-UK");
        Assert.assertEquals(1, doctorTasks.size());

        //No tasks for manager yet
        managerTasks = taskService.getTasksAssignedAsPotentialOwner("manager", "en-UK");
        Assert.assertTrue(managerTasks.isEmpty());


        taskService.start(doctorTasks.get(0).getId(), "doctor");

        taskService.complete(doctorTasks.get(0).getId(), "doctor", null);

        // abort process instance
        ksession.abortProcessInstance(processInstanceId);
        // since persisted process instance is completed it should be null
        process = ksession.getProcessInstance(process.getId());
        Assert.assertNull(process);


        final EntityManager em = emfTasks.createEntityManager();

        Assert.assertEquals(0, em.createQuery("select t from TaskImpl t").getResultList().size());
        Assert.assertEquals(0, em.createQuery("select i from I18NTextImpl i").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_BAs").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_ExclOwners").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_PotOwners").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_Recipients").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_Stakeholders").getResultList().size());
        Assert.assertEquals(0, em.createQuery("select c from ContentImpl c").getResultList().size());
        em.close();
    }
    
    @Test
    public void automaticCleanUpWitCallActivityTest() throws Exception {

        createRuntimeManager("BPMN2-CallActivity.bpmn2", "BPMN2-CallActivity2.bpmn2", "BPMN2-CallActivitySubProcess.bpmn2");
        RuntimeEngine runtimeEngine = getRuntimeEngine();
        KieSession ksession = runtimeEngine.getKieSession();
        TaskService taskService = runtimeEngine.getTaskService();        

        ksession.addEventListener(new TaskCleanUpProcessEventListener(taskService));
        
        ProcessInstance processInstance = ksession.startProcess("ParentProcessCA");
        
        List<TaskSummary> tasks = taskService.getTasksAssignedAsPotentialOwner("john", "en-UK");
        assertEquals(1,  tasks.size());
        
        long taskId = tasks.get(0).getId();
        
        taskService.start(taskId, "john");
        taskService.complete(taskId, "john", null);
        
        assertProcessInstanceCompleted(processInstance.getId(), ksession);

        final EntityManager em = emfTasks.createEntityManager();

        Assert.assertEquals(0, em.createQuery("select t from TaskImpl t").getResultList().size());
        Assert.assertEquals(0, em.createQuery("select i from I18NTextImpl i").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_BAs").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_ExclOwners").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_PotOwners").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_Recipients").getResultList().size());
        Assert.assertEquals(0, em.createNativeQuery("select * from PeopleAssignments_Stakeholders").getResultList().size());
        Assert.assertEquals(0, em.createQuery("select c from ContentImpl c").getResultList().size());
        em.close();

    }
}
