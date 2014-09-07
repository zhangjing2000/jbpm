/*
 * Copyright 2010 JBoss Inc
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

package org.jbpm.process.workitem.webservice;

import java.lang.reflect.Array;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.message.Message;
import org.kie.api.runtime.process.WorkItem;
import org.kie.internal.executor.api.Command;
import org.kie.internal.executor.api.CommandContext;
import org.kie.internal.executor.api.ExecutionResults;
import org.kie.internal.runtime.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web Service executor command that executes web service call using Apache CXF. 
 * It expects following parameters to be able to operate:
 * <ul>
 *  <li>Interface - valid interface/service name of the web service (port type name from wsdl)</li>
 *  <li>Operation - valid operation name</li>
 *  <li>Parameter - object that is going to be used as web service message</li>
 *  <li>Url - location of the wsdl file used to look up service definition</li>
 *  <li>Namespace - name space of the web service</li>
 *  <li>Endpoint - overrides the endpoint address defined in the referenced WSDL.</li>
 * </ul>
 * 
 * Web service call is synchronous but since it's executor command it will be invoked as asynchronous task any way.
 */
public class WebServiceCommand implements Command, Cacheable {
    
    private static final Logger logger = LoggerFactory.getLogger(WebServiceCommand.class);
    private volatile static ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<String, Client>();
    private JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();

    @Override
    public ExecutionResults execute(CommandContext ctx) throws Exception {
    	Object[] parameters = null;
        WorkItem workItem = (WorkItem) ctx.getData("workItem");
        
        String interfaceRef = (String) workItem.getParameter("Interface");
        String operationRef = (String) workItem.getParameter("Operation");
        String endpointAddress = (String) workItem.getParameter("Endpoint");
        if ( workItem.getParameter("Parameter") instanceof Object[]) {
        	parameters =  (Object[]) workItem.getParameter("Parameter");
        } else if (workItem.getParameter("Parameter") != null && workItem.getParameter("Parameter").getClass().isArray()) {
        	int length = Array.getLength(workItem.getParameter("Parameter"));
            parameters = new Object[length];
            for(int i = 0; i < length; i++) {
            	parameters[i] = Array.get(workItem.getParameter("Parameter"), i);
            }            
        } else {
        	parameters = new Object[]{ workItem.getParameter("Parameter")};
        }
        
        Client client = getWSClient(workItem, interfaceRef);
        
        //Override endpoint address if configured.
        if (endpointAddress != null && !"".equals(endpointAddress)) {
       	 client.getRequestContext().put(Message.ENDPOINT_ADDRESS, endpointAddress) ;
        }
        
        Object[] result = client.invoke(operationRef, parameters);
        
        ExecutionResults results = new ExecutionResults();       

        if (result == null || result.length == 0) {
            results.setData("Result", null);
        } else {
            results.setData("Result", result[0]);
        }
        logger.debug("Received sync response {}", result);
        
        
        return results;
    }
    
    
    protected synchronized Client getWSClient(WorkItem workItem, String interfaceRef) {
        if (clients.containsKey(interfaceRef)) {
            return clients.get(interfaceRef);
        }
        
        String importLocation = (String) workItem.getParameter("Url");
        String importNamespace = (String) workItem.getParameter("Namespace");
        if (importLocation != null && importLocation.trim().length() > 0 
                && importNamespace != null && importNamespace.trim().length() > 0) {
            Client client = dcf.createClient(importLocation, new QName(importNamespace, interfaceRef), Thread.currentThread().getContextClassLoader(), null);
            clients.put(interfaceRef, client);
            return client;
        }

        return null;
    }
    
	@Override
	public void close() {
		if (clients != null) {
			for (Client client : clients.values()) {
				client.destroy();
			}
		}
	}

}
