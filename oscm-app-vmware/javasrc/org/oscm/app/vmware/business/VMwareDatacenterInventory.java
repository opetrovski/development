/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2016
 *
 *  Creation Date: 2016-05-24
 *
 *******************************************************************************/

package org.oscm.app.vmware.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.oscm.app.vmware.business.VMwareValue.Unit;
import org.oscm.app.vmware.business.model.VMwareHost;
import org.oscm.app.vmware.business.model.VMwareStorage;
import org.oscm.app.vmware.business.model.VMwareVirtualMachine;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualMachinePowerState;

/**
 * The data center inventory contains information about all resources available
 * in the vCenter. The inventory is filled by adding property sets obtained from
 * a VMware property collector.
 *
 * @author Dirk Bernsau
 *
 */
public class VMwareDatacenterInventory {

    private static final Logger logger = LoggerFactory
            .getLogger(VMwareDatacenterInventory.class);

    private HashMap<String, VMwareStorage> storages = new HashMap<>();
    private HashMap<String, List<VMwareStorage>> storageByHost = new HashMap<>();
    private Collection<VMwareVirtualMachine> vms = new ArrayList<>();
    private HashMap<String, VMwareHost> hostsSystems = new HashMap<>();

    private HashMap<Object, String> hostCache = new HashMap<>();

    /**
     * Adds a storage instance to the inventory based on given properties.
     *
     * @return the created storage instance
     */
    public VMwareStorage addStorage(String host,
            List<DynamicProperty> properties) {

        if (properties == null || properties.size() == 0) {
            return null;
        }

        VMwareStorage result = new VMwareStorage();
        for (DynamicProperty dp : properties) {
            String key = dp.getName();
            if ("summary.name".equals(key) && dp.getVal() != null) {
                result.setName(dp.getVal().toString());
            } else if ("summary.capacity".equals(key) && dp.getVal() != null) {
                result.setCapacity(VMwareValue
                        .fromBytes(Long.parseLong(dp.getVal().toString())));
            } else if ("summary.freeSpace".equals(key) && dp.getVal() != null) {
                result.setFreeStorage(VMwareValue
                        .fromBytes(Long.parseLong(dp.getVal().toString())));
            }
        }
        storages.put(result.getName(), result);

        if (storageByHost.containsKey(host)) {
            storageByHost.get(host).add(result);
        } else {
            List<VMwareStorage> storage = new ArrayList<>();
            storage.add(result);
            storageByHost.put(host, storage);
        }
        return result;
    }

    /**
     * Adds a host instance to the inventory based on given properties.
     *
     * @return the created host instance
     */
    public VMwareHost addHostSystem(List<DynamicProperty> properties) {

        if (properties == null || properties.size() == 0) {
            return null;
        }

        VMwareHost result = new VMwareHost(this);
        for (DynamicProperty dp : properties) {
            String key = dp.getName();
            if ("name".equals(key) && dp.getVal() != null) {
                result.setName(dp.getVal().toString());
            } else if ("summary.hardware.memorySize".equals(key)
                    && dp.getVal() != null) {
                result.setMemorySizeMB(VMwareValue
                        .fromBytes(Long.parseLong(dp.getVal().toString()))
                        .getValue(Unit.MB));
            } else if ("summary.hardware.numCpuCores".equals(key)
                    && dp.getVal() != null) {
                result.setCpuCores(Integer.parseInt(dp.getVal().toString()));
            } else if ("summary.quickStats.overallMemoryUsage".equals(key)
                    && dp.getVal() != null) {
                result.setMemoryUsageMB(Long.parseLong(dp.getVal().toString()));
            } else if ("systemResources.config.memoryAllocation.reservation"
                    .equals(key) && dp.getVal() != null) {
                // This number is not dynamic reflecting the memory usage by
                // virtual machines but rather the memory reserved for virtual
                // machines.
                result.setMemoryAllocationReservationMB(
                        Long.parseLong(dp.getVal().toString()));
            }
        }
        hostsSystems.put(result.getName(), result);
        logger.trace("add Host " + result.getName() + " "
                + result.getMemorySizeMB());
        return result;
    }

    /**
     * Adds a VM instance to the inventory based on given properties.
     *
     * @return the created VM instance
     */
    public VMwareVirtualMachine addVirtualMachine(
            List<DynamicProperty> properties, ManagedObjectAccessor serviceUtil)
            throws Exception {

        if (properties == null || properties.size() == 0) {
            return null;
        }

        VMwareVirtualMachine result = new VMwareVirtualMachine();
        for (DynamicProperty dp : properties) {
            String key = dp.getName();
            if ("name".equals(key) && dp.getVal() != null) {
                result.setName(dp.getVal().toString());
            } else if ("summary.config.memorySizeMB".equals(key)
                    && dp.getVal() != null) {
                result.setMemorySizeMB(
                        Integer.parseInt(dp.getVal().toString()));
            } else if ("summary.config.numCpu".equals(key)
                    && dp.getVal() != null) {
                result.setNumCpu(Integer.parseInt(dp.getVal().toString()));
            } else if ("runtime.host".equals(key)) {
                ManagedObjectReference mor = (ManagedObjectReference) dp
                        .getVal();
                Object cacheKey = mor == null ? null : mor.getValue();
                if (!hostCache.containsKey(cacheKey)) {
                    Object name = serviceUtil.getDynamicProperty(mor, "name");
                    if (name != null) {
                        hostCache.put(cacheKey, name.toString());
                    }
                }
                result.setHostName(hostCache.get(cacheKey));
            } else if ("runtime.powerState".equals(key)) {
                boolean isRunning = !VirtualMachinePowerState.POWERED_OFF
                        .equals(dp.getVal());
                result.setRunning(isRunning);
            } else if ("summary.config.template".equals(key)) {
                boolean isTemplate = Boolean
                        .parseBoolean(dp.getVal().toString());
                result.setTemplate(isTemplate);
            }
        }
        if (result.getHostName() != null) {
            vms.add(result);
        } else {
            logger.warn("Cannot determine host system for VM '"
                    + result.getName()
                    + "'. Check whether configured VMware API user has rights to access the host system.");
        }
        return result;
    }

    /**
     * Initializes the allocation data of the host by summing up all configured
     * (not the actual used) resources of all VMs deployed on each host.
     *
     */
    public void initialize() {
        for (VMwareHost hostSystem : hostsSystems.values()) {
            hostSystem.setAllocatedMemoryMB(0);
            hostSystem.setAllocatedCPUs(0);
            hostSystem.setAllocatedVMs(0);
        }
        for (VMwareVirtualMachine vm : vms) {
            VMwareHost hostSystem = hostsSystems.get(vm.getHostName());
            if (hostSystem != null) {
                long vmMemMBytes = vm.getMemorySizeMB();

                if (!vm.isTemplate() && vm.isRunning()) {
                    hostSystem.setAllocatedMemoryMB(
                            hostSystem.getAllocatedMemoryMB() + vmMemMBytes);
                    hostSystem.setAllocatedCPUs(
                            hostSystem.getAllocatedCPUs() + vm.getNumCpu());
                }
                hostSystem.setAllocatedVMs(hostSystem.getAllocatedVMs() + 1);
            }
        }

        logTree();
    }

    public void logTree() {

        if (!logger.isTraceEnabled()) {
            return;
        }
        String indent = "  ";
        String indent2 = "     ";
        String nl = "\r\n";
        StringBuffer sb = new StringBuffer();
        for (VMwareHost host : hostsSystems.values()) {
            sb.append(host.getName()).append(nl);
            sb.append(indent).append("allocated memory [MB]: ")
                    .append(host.getAllocatedMemoryMB()).append(nl);
            sb.append(indent).append("reserved memory [MB]: ")
                    .append(host.getMemoryAllocationReservationMB()).append(nl);
            sb.append(indent).append("total physical memory [MB]: ")
                    .append(host.getMemorySizeMB()).append(nl);
            sb.append(indent).append("used physical memory [MB]: ")
                    .append(host.getMemoryUsageMB()).append(nl);

            sb.append(indent).append("Virtual Machines:").append(nl);

            for (VMwareVirtualMachine vm : vms) {
                if (vm.getHostName().equals(host.getName())) {
                    sb.append(indent2).append("Name: ").append(vm.getName())
                            .append(" ");
                    sb.append(indent2).append("RAM: ")
                            .append(vm.getMemorySizeMB()).append(" MB ");
                    if (vm.isTemplate()) {
                        sb.append(indent2).append("Template").append(nl);
                    } else {
                        if (vm.isRunning()) {
                            sb.append(indent2).append("running").append(nl);
                        } else {
                            sb.append(indent2).append("stopped").append(nl);
                        }
                    }
                }
            }

            sb.append(indent).append("Storage:").append(nl);

            for (VMwareStorage st : storageByHost.get(host.getName())) {
                sb.append(indent2).append("Name: ").append(st.getName())
                        .append(" ");
                sb.append(indent2).append("Capacity: ").append(st.getCapacity())
                        .append(" ");
                sb.append(indent2).append("Free: ").append(st.getFree())
                        .append(nl);
            }

        }

        logger.trace(sb.toString());

    }

    public VMwareStorage getStorage(String name) {
        return storages.get(name);
    }

    public List<VMwareStorage> getStorageByHost(String host) {
        return storageByHost.get(host);
    }

    public VMwareHost getHost(String name) {
        return hostsSystems.get(name);
    }

    public Collection<VMwareHost> getHosts() {
        return new ArrayList<>(hostsSystems.values());
    }

    /**
     * Reset the enabling information for all hosts and storages within the
     * inventory. The resources have late to be enabled one by when when reading
     * the configuration.
     */
    public void disableHostsAndStorages() {
        for (VMwareHost host : hostsSystems.values()) {
            host.setEnabled(false);
        }
        for (VMwareStorage storage : storages.values()) {
            storage.setEnabled(false);
        }
    }
}
