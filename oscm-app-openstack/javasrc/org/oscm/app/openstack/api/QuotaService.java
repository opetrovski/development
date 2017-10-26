/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: 23.10.2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.openstack.api;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.oscm.app.openstack.OpenstackClient;
import org.oscm.app.openstack.controller.OpenStackController;
import org.oscm.app.openstack.controller.PropertyHandler;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    public Response getLimits(@PathParam(value = SIGNATURE) String signature,
            @QueryParam(value = "instanceId") String instanceId,
            @QueryParam(value = "organizationId") String organizationId,
            @QueryParam(value = "subscriptionId") String subscriptionId,
            @QueryParam(value = "timestamp") String epoch) {

        String token = buildToken(instanceId, organizationId, subscriptionId,
                epoch);
        verifySignature(token, signature);
        return Response
                .ok(loadLimits(instanceId, organizationId, subscriptionId))
                .header(CONTENT_TYPE, APPLICATION_JSON).build();
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

    private String loadLimits(String instanceId, String organizationId,
            String subscriptionId) {
        try {
            ProvisioningSettings settings = app.getServiceInstanceDetails(
                    OpenStackController.ID,
                    new String(new Base64().decode(instanceId), "UTF-8"),
                    new String(new Base64().decode(subscriptionId), "UTF-8"),
                    new String(new Base64().decode(organizationId), "UTF-8"));
            OpenstackClient client = new OpenstackClient(
                    new PropertyHandler(settings));
            return (new com.fasterxml.jackson.databind.ObjectMapper())
                    .writeValueAsString(client.getLimits());
        } catch (MalformedURLException | APPlatformException
                | UnsupportedEncodingException | JsonProcessingException e) {
            LOG.error("Couldn't get quota limits", e);
            throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
        }
    }
}
