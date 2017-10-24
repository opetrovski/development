/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: 23.10.2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.openstack.api;

import java.net.MalformedURLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.openstack4j.model.compute.AbsoluteLimit;
import org.oscm.app.openstack.OpenstackClient;
import org.oscm.app.openstack.controller.OpenStackController;
import org.oscm.app.openstack.controller.PropertyHandler;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kulle
 *
 */
@Path("/os")
public class QuotaService {

    private static final Logger LOG = LoggerFactory
            .getLogger(QuotaService.class);
    private static final String SIGNATURE = "signature";

    private APPlatformService app = APPlatformServiceFactory.getInstance();

    @GET
    @Path("limits/{signature}")
    public AbsoluteLimit getLimits(
            @PathParam(value = SIGNATURE) String signature,
            @QueryParam(value = "instanceId") String instanceId,
            @QueryParam(value = "organizationId") String organizationId,
            @QueryParam(value = "subscriptionId") String subscriptionId,
            @QueryParam(value = "timestamp") String epoch) {

        String token = buildToken(instanceId, organizationId, subscriptionId,
                epoch);
        verifySignature(token, signature);
        return loadLimits(instanceId, organizationId, subscriptionId);
    }

    private String buildToken(String instanceId, String organizationId,
            String subscriptionId, String epoch) {

        try {
            return instanceId + subscriptionId + organizationId
                    + Long.parseLong(epoch);
        } catch (NumberFormatException e) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
    }

    private void verifySignature(String token, String signature) {
        boolean valid = app.checkToken(token, signature);
        if (!valid) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
    }

    private AbsoluteLimit loadLimits(String instanceId, String organizationId,
            String subscriptionId) {
        try {
            ProvisioningSettings settings = app.getServiceInstanceDetails(
                    OpenStackController.ID, instanceId, subscriptionId,
                    organizationId);
            OpenstackClient client = new OpenstackClient(
                    new PropertyHandler(settings));
            return client.getLimits();
        } catch (MalformedURLException | APPlatformException e) {
            LOG.error("Couldn't get quota limits", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
}
