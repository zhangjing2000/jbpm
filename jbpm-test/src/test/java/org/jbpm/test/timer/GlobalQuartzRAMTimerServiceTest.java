package org.jbpm.test.timer;

import java.util.Arrays;
import java.util.Collection;

import org.jbpm.process.core.timer.impl.QuartzSchedulerService;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;

@RunWith(Parameterized.class)
public class GlobalQuartzRAMTimerServiceTest extends GlobalTimerServiceBaseTest {
    
    private int managerType;
    
    @Parameters
    public static Collection<Object[]> persistence() {
        Object[][] data = new Object[][] { { 1 }, { 2 }, { 3 }  };
        return Arrays.asList(data);
    };
    
    public GlobalQuartzRAMTimerServiceTest(int managerType) {
        this.managerType = managerType;
        
    }
    
    @Before
    public void setUp() {
        tearDownOnce();
        setUpOnce();
        cleanupSingletonSessionId();
        System.setProperty("org.quartz.properties", "quartz-ram.properties");
        globalScheduler = new QuartzSchedulerService();
        ((QuartzSchedulerService)globalScheduler).forceShutdown();
    }
    
    @After
    public void tearDown(){
        try {
            globalScheduler.shutdown();
        } catch (Exception e) {
            
        }        
        tearDownOnce();
    }

    @Override
    protected RuntimeManager getManager(RuntimeEnvironment environment, boolean waitOnStart) {
    	RuntimeManager manager = null;
    	if (managerType ==1) {
    		manager = RuntimeManagerFactory.Factory.get().newSingletonRuntimeManager(environment);
        } else if (managerType == 2) {
        	manager = RuntimeManagerFactory.Factory.get().newPerRequestRuntimeManager(environment);
        } else if (managerType == 3) {
        	manager = RuntimeManagerFactory.Factory.get().newPerProcessInstanceRuntimeManager(environment);
        } else {
            throw new IllegalArgumentException("Invalid runtime maanger type");
        }
    	if (waitOnStart) {
	        // wait for the 2 seconds (default startup delay for quartz)
	    	try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// do nothing
			}
    	}
    	return manager;
    }

}
