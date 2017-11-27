/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: 27.11.2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.vmware.business;

import static java.lang.String.format;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static org.oscm.app.vmware.business.VMPropertyHandler.MANAGER_HOST;
import static org.oscm.app.vmware.business.VMPropertyHandler.REQUESTING_USER;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_DOMAIN_NAME;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_INSTANCENAME;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_LINUX_ROOT_PWD;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_NIC1_DNS_SERVER;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_NIC1_DNS_SUFFIX;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_NIC1_GATEWAY;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_NIC1_IP_ADDRESS;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_NIC1_SUBNET_MASK;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_NUMBER_OF_NICS;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_SCRIPT_PWD;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_SCRIPT_URL;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_SCRIPT_USERID;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN_PWD;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_DOMAIN_JOIN;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_WORKGROUP;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oscm.app.vmware.business.Script.OS;
import org.oscm.app.vmware.business.model.VCenter;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.oscm.app.vmware.remote.bes.ServiceParamRetrieval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kulle
 *
 */
public class ScriptParameter {

    private static final Logger LOG = LoggerFactory
            .getLogger(ScriptParameter.class);

    private static final String EOL_DOS = "\r\n";
    private static final String EOL_MACINTOSH = "\r";
    private static final String EOL_UNIX = "\n";

    String script;
    OS os;
    ServiceParamRetrieval sp;
    VMPropertyHandler ph;

    public ScriptParameter() {

    }

    public ScriptParameter(VMPropertyHandler ph, OS os) {
        this.ph = ph;
        this.os = os;
        sp = new ServiceParamRetrieval(ph);
    }

    /**
     * Inserts service parameters and configuration settings into the script
     * 
     */
    public void insertParameters(String script) throws Exception {
        LOG.debug("Script before patching:\n" + script);
        this.script = script;

        String[] lines = splitScriptAfterFirstLine();
        String firstLine = lines[0];
        String rest = lines[1];

        StringBuffer parameters = new StringBuffer();
        addOsIndependetServiceParameters(parameters);
        addManagedHost(parameters);
        if (os == OS.WINDOWS) {
            addServiceParametersForWindowsVms(parameters);
            script = parameters.toString() + os.getLineEnding() + firstLine
                    + os.getLineEnding() + rest;
        } else {
            addServiceParametersForLinuxVms(parameters);
            script = firstLine + os.getLineEnding() + parameters.toString()
                    + os.getLineEnding() + rest;
        }

        logScriptWithoutPasswords();
    }

    String[] splitScriptAfterFirstLine() {
        return script
                .split(format("%s|%s|%s", EOL_DOS, EOL_MACINTOSH, EOL_UNIX), 2);
    }

    private void addManagedHost(StringBuffer parameters) {
        try {
            DataAccessService das = new DataAccessService(ph.getLocale());
            VCenter vcenter = das.getVCenterByName(ph.getTargetVCenterServer());
            parameters.append(buildParameterCommand(MANAGER_HOST,
                    vcenter.getManagerHost()));
        } catch (Exception e) {
            LOG.warn(String.format(
                    "Couldn't load and add MANAGER_HOST to script because VCenter %s was not found in VMware database",
                    ph.getTargetVCenterServer()));
        }
    }

    private void addServiceParametersForWindowsVms(StringBuffer sb)
            throws Exception {

        sb.append(buildParameterCommand(TS_WINDOWS_DOMAIN_ADMIN));
        sb.append(buildParameterCommand(TS_WINDOWS_DOMAIN_ADMIN_PWD));
        sb.append(buildParameterCommand(TS_WINDOWS_DOMAIN_JOIN));
        sb.append(buildParameterCommand(TS_DOMAIN_NAME));
        sb.append(buildParameterCommand(TS_WINDOWS_LOCAL_ADMIN_PWD));
        sb.append(buildParameterCommand(TS_WINDOWS_WORKGROUP));
    }

    private void addServiceParametersForLinuxVms(StringBuffer sb)
            throws Exception {

        sb.append(buildParameterCommand(TS_WINDOWS_DOMAIN_ADMIN));
        sb.append(buildParameterCommand(TS_WINDOWS_DOMAIN_ADMIN_PWD));
        sb.append(buildParameterCommand(TS_LINUX_ROOT_PWD));
        sb.append(buildParameterCommand(TS_DOMAIN_NAME));
    }

    private void addOsIndependetServiceParameters(StringBuffer sb)
            throws Exception {

        sb.append(buildParameterCommand(TS_INSTANCENAME, ph.getInstanceName()));
        sb.append(buildParameterCommand(REQUESTING_USER));
        addScriptParameters(sb);
        addDataDiskParameters(sb);
        addNetworkServiceParameters(sb);
    }

    private void addScriptParameters(StringBuffer sb) throws Exception {
        sb.append(buildParameterCommand(TS_SCRIPT_URL));
        sb.append(buildParameterCommand(TS_SCRIPT_USERID));
        sb.append(buildParameterCommand(TS_SCRIPT_PWD));
    }

    private void addDataDiskParameters(StringBuffer sb) throws Exception {
        for (String key : ph.getDataDiskMountPointParameterKeys()) {
            sb.append(buildParameterCommand(key));
        }
        for (String key : ph.getDataDiskSizeParameterKeys()) {
            sb.append(buildParameterCommand(key));
        }
    }

    private void addNetworkServiceParameters(StringBuffer sb) throws Exception {
        int numNics = Integer.parseInt(sp.getServiceSetting(TS_NUMBER_OF_NICS));
        while (numNics > 0) {
            String param = getIndexedParam(TS_NIC1_DNS_SERVER, numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(TS_NIC1_DNS_SUFFIX, numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(TS_NIC1_GATEWAY, numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(TS_NIC1_IP_ADDRESS, numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(TS_NIC1_NETWORK_ADAPTER, numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(TS_NIC1_SUBNET_MASK, numNics);
            sb.append(buildParameterCommand(param));

            numNics--;
        }
    }

    private String getIndexedParam(String param, int index) {
        return param.replace('1', Integer.toString(index).charAt(0));
    }

    private String buildParameterCommand(String key) throws Exception {
        return buildParameterCommand(key, sp.getServiceSetting(key));
    }

    private String buildParameterCommand(String key, String value) {
        switch (os) {
        case LINUX:
            return key + "='" + value + "'" + os.getLineEnding();
        case WINDOWS:
            return "set " + key + "=" + value + os.getLineEnding();
        default:
            throw new IllegalStateException("OS type" + os.name()
                    + " not supported by Script execution");
        }
    }

    private void logScriptWithoutPasswords() {
        LOG.debug("Patched script:\n" + replacePasswords());
    }

    String replacePasswords() {
        StringBuffer sb = new StringBuffer();

        Pattern pattern = compile("(^.+?_PWD=')(.*)('$)", MULTILINE);
        Matcher matcher = pattern.matcher(script);
        while (matcher.find()) {
            matcher.appendReplacement(sb, "$1" + "*****" + "$3");

        }
        matcher.appendTail(sb);

        return sb.toString();
    }

}
