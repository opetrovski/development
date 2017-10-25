/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: 24.10.2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.openstack.api;

import static javax.ws.rs.Priorities.HEADER_DECORATOR;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

/**
 * @author kulle
 *
 */
@Provider
@Priority(HEADER_DECORATOR)
public class CorsResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) throws IOException {

        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
        headers.add("Access-Control-Allow-Headers",
                "X-Requested-With, Content-Type");
        headers.add("Access-Control-Allow-Methods",
                "POST, PUT, GET, DELETE, HEAD, OPTIONS");
    }

}
