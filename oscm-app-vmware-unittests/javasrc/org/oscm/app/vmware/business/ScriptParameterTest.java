/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2017                                           
 *                                                                                                                                 
 *  Creation Date: 27.11.2017                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.vmware.business;

import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.oscm.app.vmware.business.Script.OS.LINUX;
import static org.oscm.app.vmware.business.VMPropertyHandler.BSS_USER_KEY;
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.oscm.app.v1_0.data.ProvisioningSettings;
import org.oscm.app.vmware.business.Script.OS;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.oscm.app.vmware.remote.bes.ServiceParamRetrieval;

/**
 * @author kulle
 *
 */
public class ScriptParameterTest {

    private ScriptParameter scriptParameter;

    private OS os;
    private VMPropertyHandler ph;

    @Before
    public void before() throws Exception {
        scriptParameter = spy(new ScriptParameter());
        scriptParameter.os = os;
        scriptParameter.ph = ph = initializePropertyHandler();
        ph.useMock(mock(DataAccessService.class));
        scriptParameter.sp = new ServiceParamRetrieval();
        scriptParameter.sp.setPh(ph);
    }

    private VMPropertyHandler initializePropertyHandler() {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(TS_INSTANCENAME, "ts_instancename");
        parameters.put(REQUESTING_USER, "requesting_user");
        parameters.put(TS_LINUX_ROOT_PWD, "ts_linux_root_pwd");
        parameters.put(TS_WINDOWS_DOMAIN_ADMIN, "ts_windows_domain_admin");
        parameters.put(TS_WINDOWS_DOMAIN_ADMIN_PWD,
                "ts_windows_domain_admin_pwd");
        parameters.put(TS_DOMAIN_NAME, "ts_domain_name");
        parameters.put(TS_WINDOWS_LOCAL_ADMIN_PWD,
                "ts_windows_local_admin_pwd");
        parameters.put(TS_WINDOWS_DOMAIN_JOIN, "ts_windows_domain_join");
        parameters.put(TS_WINDOWS_WORKGROUP, "ts_windows_workgroup");
        parameters.put(TS_SCRIPT_URL, "ts_script_url");
        parameters.put(TS_SCRIPT_USERID, "instts_script_useridancename");
        parameters.put(TS_SCRIPT_PWD, "ts_script_pwd");
        parameters.put(TS_NUMBER_OF_NICS, "1");
        parameters.put(TS_NIC1_DNS_SERVER, "ts_nic1_dns_server");
        parameters.put(TS_NIC1_DNS_SUFFIX, "ts_nic1_dns_suffix");
        parameters.put(TS_NIC1_GATEWAY, "ts_nic1_gateway");
        parameters.put(TS_NIC1_IP_ADDRESS, "ts_nic1_ip_address");
        parameters.put(TS_NIC1_NETWORK_ADAPTER, "ts_nic1_network_adapter");
        parameters.put(TS_NIC1_SUBNET_MASK, "ts_nic1_subnet_mask");

        HashMap<String, String> configSettings = new HashMap<>();
        configSettings.put(BSS_USER_KEY, "1000");

        ProvisioningSettings settings = new ProvisioningSettings(parameters,
                configSettings, "en");
        return new VMPropertyHandler(settings);
    }

    @Test
    public void splitScriptAfterFirstLine_dos() throws Exception {
        // given
        scriptParameter.script = "line1\r\nline2\r\nline3";

        // when
        String[] lines = scriptParameter.splitScriptAfterFirstLine();

        // then
        assertEquals("line1", lines[0]);
        assertEquals("line2\r\nline3", lines[1]);
    }

    @Test
    public void splitScriptAfterFirstLine_macintosh() throws Exception {
        // given
        scriptParameter.script = "line1\rline2\rline3";

        // when
        String[] lines = scriptParameter.splitScriptAfterFirstLine();

        // then
        assertEquals("line1", lines[0]);
        assertEquals("line2\rline3", lines[1]);
    }

    @Test
    public void splitScriptAfterFirstLine_unix() throws Exception {
        // given
        scriptParameter.script = "line1\nline2\nline3";

        // when
        String[] lines = scriptParameter.splitScriptAfterFirstLine();

        // then
        assertEquals("line1", lines[0]);
        assertEquals("line2\nline3", lines[1]);
    }

    @Test
    public void splitScriptAfterFirstLine_mixed() throws Exception {
        // given
        scriptParameter.script = "line1\nline2\r\nline3";

        // when
        String[] lines = scriptParameter.splitScriptAfterFirstLine();

        // then
        assertEquals("line1", lines[0]);
        assertEquals("line2\r\nline3", lines[1]);
    }

    @Test
    public void splitScriptAfterFirstLine_mixed2() throws Exception {
        // given
        scriptParameter.script = "line1\n\r\nline2\r\nline3";

        // when
        String[] lines = scriptParameter.splitScriptAfterFirstLine();

        // then
        assertEquals("line1", lines[0]);
        assertEquals("\r\nline2\r\nline3", lines[1]);
    }

    @Test
    public void insertParameters() throws Exception {
        // given
        String linuxScript = new String(Files.readAllBytes(Paths.get(
                ScriptParameterTest.class.getResource("/linux.sh").toURI())));
        scriptParameter.script = linuxScript;
        scriptParameter.os = LINUX;

        // when
        scriptParameter.insertParameters(scriptParameter.script);
        String patchedScript = scriptParameter.script;

        // then
        assertTrue(patchedScript.contains("INSTANCENAME='ts_instancename'"));
        assertTrue(patchedScript.contains("REQUESTING_USER='requesting_user'"));
        assertTrue(
                patchedScript.contains("NIC1_DNS_SERVER='ts_nic1_dns_server'"));
        assertTrue(
                patchedScript.contains("NIC1_DNS_SUFFIX='ts_nic1_dns_suffix'"));
        assertTrue(patchedScript.contains("NIC1_GATEWAY='ts_nic1_gateway'"));
        assertTrue(
                patchedScript.contains("NIC1_IP_ADDRESS='ts_nic1_ip_address'"));
        assertTrue(patchedScript
                .contains("NIC1_NETWORK_ADAPTER='ts_nic1_network_adapter'"));
        assertTrue(patchedScript
                .contains("NIC1_SUBNET_MASK='ts_nic1_subnet_mask'"));
        assertTrue(patchedScript.contains("SCRIPT_URL='ts_script_url'"));
        assertTrue(patchedScript
                .contains("SCRIPT_USERID='instts_script_useridancename'"));
        assertTrue(patchedScript.contains("SCRIPT_PWD='ts_script_pwd'"));
        assertTrue(patchedScript
                .contains("WINDOWS_DOMAIN_ADMIN='ts_windows_domain_admin'"));
        assertTrue(patchedScript.contains(
                "WINDOWS_DOMAIN_ADMIN_PWD='ts_windows_domain_admin_pwd'"));
        assertTrue(
                patchedScript.contains("LINUX_ROOT_PWD='ts_linux_root_pwd'"));
        assertTrue(patchedScript.contains("DOMAIN_NAME='ts_domain_name'"));
    }

    @Test
    public void replacePasswords_linux() throws Exception {
        // given
        String linuxScript = new String(
                Files.readAllBytes(Paths.get(ScriptParameterTest.class
                        .getResource("/linux-with-passwords.sh").toURI())));
        scriptParameter.script = linuxScript;
        scriptParameter.os = LINUX;

        // when
        String withoutPasswords = scriptParameter.replacePasswords();

        // then
        Pattern pattern = compile("^.+?_PWD='(?!\\*{5}'$)", MULTILINE);
        assertFalse("each password has to be replaced with *****",
                pattern.matcher(withoutPasswords).find());
    }

}