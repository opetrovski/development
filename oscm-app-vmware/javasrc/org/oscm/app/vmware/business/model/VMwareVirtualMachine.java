/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2016
 *
 *  Creation Date: 2016-05-24
 *
 *******************************************************************************/

package org.oscm.app.vmware.business.model;

/**
 * @author Dirk Bernsau
 *
 */
public class VMwareVirtualMachine {

    private String name;
    private String hostName;
    private int numCpu;
    private int memorySizeMB;
    private boolean isRunning;
    private boolean isTemplate;

    public boolean isTemplate() {
        return isTemplate;
    }

    public void setTemplate(boolean isTemplate) {
        this.isTemplate = isTemplate;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getNumCpu() {
        return numCpu;
    }

    public void setNumCpu(int numCpu) {
        this.numCpu = numCpu;
    }

    public int getMemorySizeMB() {
        return memorySizeMB;
    }

    public void setMemorySizeMB(int memorySizeMB) {
        this.memorySizeMB = memorySizeMB;
    }
}
