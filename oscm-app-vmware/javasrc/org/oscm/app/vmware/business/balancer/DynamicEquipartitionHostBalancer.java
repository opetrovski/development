/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2016
 *
 *  Creation Date: 2016-05-24
 *
 *******************************************************************************/

package org.oscm.app.vmware.business.balancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.oscm.app.v1_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.business.model.VMwareHost;
import org.oscm.app.vmware.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Balancer implementation determining dynamically from vSphere the host with
 * the lowest number of VMs running on it.
 *
 * @author Oliver Petrovski
 *
 */
public class DynamicEquipartitionHostBalancer extends HostBalancer {

    private static final Logger logger = LoggerFactory
            .getLogger(DynamicEquipartitionHostBalancer.class);
    private static final String ELEMENT_BLACKLIST_HOST = "blacklisthost";
    List<String> blacklistHosts;

    @Override
    public void setConfiguration(HierarchicalConfiguration xmlConfig) {
        super.setConfiguration(xmlConfig);
        List<Object> hosts = xmlConfig.getList(ELEMENT_BLACKLIST_HOST);
        blacklistHosts = new ArrayList<>(hosts.size());
        for (Object blhost : hosts) {
            blacklistHosts.add(blhost.toString().toLowerCase());
        }

    }

    @Override
    public VMwareHost next(VMPropertyHandler properties)
            throws APPlatformException {

        VMwareHost selectedHost = null;
        double maxRAM = 0.0;

        Collection<VMwareHost> hosts = inventory.getHosts();

        for (VMwareHost host : hosts) {

            double freeRAM = host.getMemorySizeMB()
                    - host.getAllocatedMemoryMB();

            logger.debug("Evaluate Host:" + host.getName() + "  free RAM: "
                    + freeRAM + "  current max RAM: " + maxRAM);

            if (blacklistHosts.contains(host.getName().toLowerCase())) {
                logger.debug("Blacklisted Host: " + host.getName());
                continue;
            }

            if (freeRAM > maxRAM) {
                logger.debug("New preferred Host: " + host.getName());
                selectedHost = host;
                maxRAM = freeRAM;
            }
        }

        if (selectedHost == null) {
            throw new APPlatformException(Messages.getAll("error_outof_host"));
        }

        return selectedHost;
    }
}
