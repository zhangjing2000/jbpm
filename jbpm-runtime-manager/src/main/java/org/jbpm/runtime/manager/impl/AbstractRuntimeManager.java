/*
 * Copyright 2013 JBoss Inc
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
package org.jbpm.runtime.manager.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.drools.core.time.TimerService;
import org.drools.persistence.OrderedTransactionSynchronization;
import org.drools.persistence.TransactionManager;
import org.drools.persistence.TransactionManagerHelper;
import org.drools.persistence.TransactionSynchronization;
import org.drools.persistence.jta.JtaTransactionManager;
import org.jbpm.process.core.timer.GlobalSchedulerService;
import org.jbpm.process.core.timer.TimerServiceRegistry;
import org.jbpm.process.core.timer.impl.GlobalTimerService;
import org.jbpm.runtime.manager.api.SchedulerProvider;
import org.jbpm.runtime.manager.impl.deploy.DeploymentDescriptorManager;
import org.jbpm.services.task.wih.ExternalTaskEventListener;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.event.rule.RuleRuntimeEventListener;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.manager.RegisterableItemsFactory;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.manager.CacheManager;
import org.kie.internal.runtime.manager.Disposable;
import org.kie.internal.runtime.manager.DisposeListener;
import org.kie.internal.runtime.manager.InternalRegisterableItemsFactory;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.internal.runtime.manager.RuntimeManagerRegistry;
import org.kie.internal.runtime.manager.SecurityManager;
import org.kie.internal.task.api.ContentMarshallerContext;
import org.kie.internal.task.api.EventService;
import org.kie.internal.task.api.InternalTaskService;

/**
 * Common implementation that all <code>RuntimeManager</code> implementation should inherit from.
 * Provides following capabilities:
 * <ul>
 *  <li>keeps track of all active managers by its identifier and prevents of having multiple managers with same id</li>
 *  <li>provides common close operation</li>
 *  <li>injects RuntimeManager into ksession's environment for further reference</li>
 *  <li>registers dispose callbacks (via transaction synchronization) 
 *  to dispose runtime engine automatically on transaction completion</li>
 *  <li>registers all defined items (work item handlers, event listeners)</li>
 * </ul>
 * Additionally, provides abstract <code>init</code> method that will be called on RuntimeManager instantiation. 
 */
public abstract class AbstractRuntimeManager implements InternalRuntimeManager {

    protected RuntimeManagerRegistry registry = RuntimeManagerRegistry.get();
    protected RuntimeEnvironment environment;
    protected DeploymentDescriptor deploymentDescriptor;
    
    protected CacheManager cacheManager = new CacheManagerImpl();
    
    protected boolean engineInitEager = Boolean.parseBoolean(System.getProperty("org.jbpm.rm.engine.eager", "false"));

	protected String identifier;
    
    protected boolean closed = false;
    
    protected SecurityManager securityManager = null;
    
    public AbstractRuntimeManager(RuntimeEnvironment environment, String identifier) {
        this.environment = environment;
        this.identifier = identifier;
        if (registry.isRegistered(identifier)) {
            throw new IllegalStateException("RuntimeManager with id " + identifier + " is already active");
        }
        internalSetDeploymentDescriptor();
        ((InternalRegisterableItemsFactory)environment.getRegisterableItemsFactory()).setRuntimeManager(this);
        String eagerInit = (String)((SimpleRuntimeEnvironment)environment).getEnvironmentTemplate().get("RuntimeEngineEagerInit");
        if (eagerInit != null) {
        	engineInitEager = Boolean.parseBoolean(eagerInit);
        }
    }
    
    private void internalSetDeploymentDescriptor() {
    	this.deploymentDescriptor = (DeploymentDescriptor) ((SimpleRuntimeEnvironment)environment).getEnvironmentTemplate().get("KieDeploymentDescriptor");
    	if (this.deploymentDescriptor == null) {
    		this.deploymentDescriptor = new DeploymentDescriptorManager().getDefaultDescriptor();
    	}
	}

	public abstract void init();
    
    protected void registerItems(RuntimeEngine runtime) {
        RegisterableItemsFactory factory = environment.getRegisterableItemsFactory();
        // process handlers
        Map<String, WorkItemHandler> handlers = factory.getWorkItemHandlers(runtime);
        for (Entry<String, WorkItemHandler> entry : handlers.entrySet()) {
            runtime.getKieSession().getWorkItemManager().registerWorkItemHandler(entry.getKey(), entry.getValue());
        }
        
        // register globals
        Map<String, Object> globals = factory.getGlobals(runtime);
        for (Entry<String, Object> entry : globals.entrySet()) {
            runtime.getKieSession().setGlobal(entry.getKey(), entry.getValue());
        }
        
        // process listeners
        List<ProcessEventListener> processListeners = factory.getProcessEventListeners(runtime);
        for (ProcessEventListener listener : processListeners) {
            runtime.getKieSession().addEventListener(listener);
        }
        
        // agenda listeners
        List<AgendaEventListener> agendaListeners = factory.getAgendaEventListeners(runtime);
        for (AgendaEventListener listener : agendaListeners) {
            runtime.getKieSession().addEventListener(listener);
        }
        
        // working memory listeners
        List<RuleRuntimeEventListener> wmListeners = factory.getRuleRuntimeEventListeners(runtime);
        for (RuleRuntimeEventListener listener : wmListeners) {
            runtime.getKieSession().addEventListener(listener);
        }
    }
    
    protected void registerDisposeCallback(RuntimeEngine runtime, TransactionSynchronization sync) {
    	if (hasEnvironmentEntry("IS_JTA_TRANSACTION", false)) {
    		return;
    	}
        // register it if there is an active transaction as we assume then to be running in a managed environment e.g CMT       
        TransactionManager tm = getTransactionManager(runtime.getKieSession().getEnvironment());
        if (tm.getStatus() != JtaTransactionManager.STATUS_NO_TRANSACTION
                && tm.getStatus() != JtaTransactionManager.STATUS_ROLLEDBACK
                && tm.getStatus() != JtaTransactionManager.STATUS_COMMITTED) {
            TransactionManagerHelper.registerTransactionSyncInContainer(tm, (OrderedTransactionSynchronization) sync);
        }
    }
    
    protected void attachManager(RuntimeEngine runtime) {
        runtime.getKieSession().getEnvironment().set("RuntimeManager", this);
        runtime.getKieSession().getEnvironment().set("deploymentId", this.getIdentifier());
    }
    
    @Override
    public boolean isClosed() {
    	return this.closed;
    }

    @Override
    public void close() {
        close(false);
    }
    
    public void close(boolean removeJobs) {
    	cacheManager.dispose();
        environment.close();
        registry.remove(identifier);
        TimerService timerService = TimerServiceRegistry.getInstance().remove(getIdentifier() + TimerServiceRegistry.TIMER_SERVICE_SUFFIX);
        if (timerService != null) {
            if (removeJobs && timerService instanceof GlobalTimerService) {
                ((GlobalTimerService) timerService).destroy();
            }
            timerService.shutdown();
            GlobalSchedulerService schedulerService = ((SchedulerProvider) environment).getSchedulerService();  
            if (schedulerService != null) {
                schedulerService.shutdown();
            }
        }
        this.closed = true;
    }

    public org.kie.internal.runtime.manager.RuntimeEnvironment getEnvironment() {
        return (org.kie.internal.runtime.manager.RuntimeEnvironment)environment;
    }

    public void setEnvironment(RuntimeEnvironment environment) {
        this.environment = environment;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void configureRuntimeOnTaskService(InternalTaskService internalTaskService, RuntimeEngine engine) {
        if (internalTaskService != null) {
            internalTaskService.addMarshallerContext(getIdentifier(), 
                new ContentMarshallerContext(environment.getEnvironment(), environment.getClassLoader()));
            ExternalTaskEventListener listener = new ExternalTaskEventListener();
            if (internalTaskService instanceof EventService) {
                ((EventService)internalTaskService).registerTaskEventListener(listener);
            }
            
            if (engine != null && engine instanceof Disposable) {
                ((Disposable)engine).addDisposeListener(new DisposeListener() {
                    
                    @Override
                    public void onDispose(RuntimeEngine runtime) {
                        if (runtime.getTaskService() instanceof EventService) {
                            ((EventService)runtime.getTaskService()).clearTaskEventListeners();;
                        }
                    }
                });
            }
        }
    }
    
    protected void removeRuntimeFromTaskService(InternalTaskService internalTaskService) {
        if (internalTaskService != null) {
            internalTaskService.removeMarshallerContext(getIdentifier());
        }
    }
    

    protected boolean canDestroy(RuntimeEngine runtime) {
    	if (hasEnvironmentEntry("IS_JTA_TRANSACTION", false)) {
    		return false;
    	}
        TransactionManager tm = getTransactionManager(runtime.getKieSession().getEnvironment());
        if (tm.getStatus() == JtaTransactionManager.STATUS_NO_TRANSACTION ||
                tm.getStatus() == JtaTransactionManager.STATUS_ACTIVE) {
            return true;
        }
        return false;
    }

    protected boolean hasEnvironmentEntry(String name, Object value) {
    	Object envEntry = environment.getEnvironment().get(name);
    	if (value == null) {
    		return envEntry == null;
    	}
    	return value.equals(envEntry);
    }
    
    protected TransactionManager getTransactionManager(Environment env) {
    	if (env == null) {
    		env = environment.getEnvironment();
    	}
    	Object txm = env.get(EnvironmentName.TRANSACTION_MANAGER);
    	if (txm != null && txm instanceof TransactionManager) {
    		return (TransactionManager) txm;
    	}
    	
    	return new JtaTransactionManager(null, null, null);
    }
    
    @Override
    public DeploymentDescriptor getDeploymentDescriptor() {
		return deploymentDescriptor;
	}

    @Override
	public void setDeploymentDescriptor(DeploymentDescriptor deploymentDescriptor) {
		this.deploymentDescriptor = deploymentDescriptor;
	}
    
    @Override
    public void setSecurityManager(SecurityManager securityManager) {
    	if (this.securityManager != null) {
    		throw new IllegalStateException("Security Manager for " + this.identifier + " manager is already set");
    	}
    	this.securityManager = securityManager;    
    }
    
    protected void checkPermission() {
    	if (this.securityManager != null) {
    		this.securityManager.checkPermission();
    	}
    }

	@Override
	public void setCacheManager(CacheManager cacheManager) {
		if (cacheManager != null) {
			this.cacheManager = cacheManager;
		}
	}

	@Override
	public CacheManager getCacheManager() {
		return cacheManager;
	}
    
    
}
