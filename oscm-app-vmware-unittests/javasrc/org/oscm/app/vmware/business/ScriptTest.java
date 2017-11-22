/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2016
 *
 *  Creation Date: 2016-05-24
 *
 *******************************************************************************/

package org.oscm.app.vmware.business;

import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.oscm.app.v1_0.data.ProvisioningSettings;
import org.oscm.app.vmware.business.Script.OS;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.oscm.app.vmware.remote.bes.ServiceParamRetrieval;

public class ScriptTest {

    private Script script;

    private OS os;
    private VMPropertyHandler ph;

    @Before
    public void before() throws Exception {
        script = spy(new Script());
        script.os = os;
        script.ph = ph = initializePropertyHandler();
        ph.useMock(mock(DataAccessService.class));
        script.sp = new ServiceParamRetrieval();
        script.sp.setPh(ph);
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
    public void insertServiceParameter() throws Exception {
        // given
        String linuxScript = new String(Files.readAllBytes(
                Paths.get(ScriptTest.class.getResource("/linux.sh").toURI())));
        script.script = linuxScript;
        script.os = LINUX;

        // when
        String patchedScript = script.insertServiceParameter();

        // then
        System.out.println(patchedScript);
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
    public void insertServiceParameter_log() throws Exception {
        // given
        String linuxScript = new String(Files.readAllBytes(
                Paths.get(ScriptTest.class.getResource("/linux.sh").toURI())));
        script.script = linuxScript;
        script.os = LINUX;
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);

        // when
        script.insertServiceParameter();

        // then
        verify(script).logScript(captor.capture());
        Pattern pattern = compile("^.+?_PWD='(?!\\*{5}'$)", MULTILINE);
        assertFalse("each password has to be replaced with *****",
                pattern.matcher(captor.getValue()).find());
    }

    @Test
    public void testHidePasswordsLinux() throws Exception {
        // given
        String script = "#!/bin/bash" + OS.LINUX.getLineEnding()
                + "LINUX_ROOT_PWD='sunny'" + OS.LINUX.getLineEnding()
                + "DOMAIN_NAME='sunnyside.up.com'" + OS.LINUX.getLineEnding()
                + "INSTANCENAME='sunny'" + OS.LINUX.getLineEnding()
                + "REQUESTING_USER='Julia@DarkSide.com'"
                + OS.LINUX.getLineEnding()
                + "SCRIPT_URL='http://localhost:28880/test_script.sh'"
                + OS.LINUX.getLineEnding() + "SCRIPT_USERID='root'"
                + OS.LINUX.getLineEnding() + "SCRIPT_PWD='sunnyscript'"
                + OS.LINUX.getLineEnding() + "NIC1_DNS_SERVER=''"
                + OS.LINUX.getLineEnding() + "NIC1_DNS_SUFFIX=''"
                + OS.LINUX.getLineEnding() + "NIC1_GATEWAY=''"
                + OS.LINUX.getLineEnding() + "NIC1_IP_ADDRESS=''"
                + OS.LINUX.getLineEnding() + OS.LINUX.getLineEnding()
                + "shutdown -h now";

        List<String> passwords = new ArrayList<String>();
        passwords.add("sunny");
        passwords.add("sunnyscript");

        // when
        String changedScript = new Script().hidePasswords(script, passwords,
                OS.LINUX);

        // then
        assertTrue(
                changedScript.contains("LINUX_ROOT_PWD='" + Script.HIDDEN_PWD));
        assertTrue(changedScript.contains("SCRIPT_PWD='" + Script.HIDDEN_PWD));
        System.out.println(changedScript);
    }

    @Test
    public void testHidePasswordsLinux_EmptyPwd() throws Exception {
        // given
        String script = "#!/bin/bash" + OS.LINUX.getLineEnding()
                + "LINUX_ROOT_PWD=''" + OS.LINUX.getLineEnding()
                + "DOMAIN_NAME='sunnyside.up.com'" + OS.LINUX.getLineEnding()
                + "INSTANCENAME='sunny'" + OS.LINUX.getLineEnding()
                + "REQUESTING_USER='Julia@DarkSide.com'"
                + OS.LINUX.getLineEnding()
                + "SCRIPT_URL='http://localhost:28880/test_script.sh'"
                + OS.LINUX.getLineEnding() + "SCRIPT_USERID='root'"
                + OS.LINUX.getLineEnding() + "SCRIPT_PWD='sunnyscript'"
                + OS.LINUX.getLineEnding() + "NIC1_DNS_SERVER=''"
                + OS.LINUX.getLineEnding() + "NIC1_DNS_SUFFIX=''"
                + OS.LINUX.getLineEnding() + "NIC1_GATEWAY=''"
                + OS.LINUX.getLineEnding() + "NIC1_IP_ADDRESS=''"
                + OS.LINUX.getLineEnding() + OS.LINUX.getLineEnding()
                + "shutdown -h now";

        List<String> passwords = new ArrayList<String>();
        passwords.add("");
        passwords.add("sunnyscript");

        // when
        String changedScript = new Script().hidePasswords(script, passwords,
                OS.LINUX);

        // then
        System.out.println(changedScript);
        assertTrue(
                changedScript.contains("LINUX_ROOT_PWD='" + Script.HIDDEN_PWD));
        assertTrue(changedScript.contains("SCRIPT_PWD='" + Script.HIDDEN_PWD));
    }

    @Test
    public void testHidePasswordsWindows() throws Exception {
        // given
        String script = "set WINDOWS_LOCAL_ADMIN_PWD=sunny"
                + OS.WINDOWS.getLineEnding()
                + "set WINDOWS_DOMAIN_ADMIN_PWD=admin123"
                + OS.WINDOWS.getLineEnding()
                + "set DOMAIN_NAME=sunnyside.up.com"
                + OS.WINDOWS.getLineEnding() + "set INSTANCENAME=sunny"
                + OS.WINDOWS.getLineEnding()
                + "set REQUESTING_USER=Julia@DarkSide.com"
                + OS.WINDOWS.getLineEnding()
                + "set SCRIPT_URL=http://localhost:28880/test_script.sh"
                + OS.WINDOWS.getLineEnding() + "set SCRIPT_USERID=Administrator"
                + OS.WINDOWS.getLineEnding() + "set SCRIPT_PWD=sunnyscript"
                + OS.WINDOWS.getLineEnding() + "set NIC1_DNS_SERVER="
                + OS.WINDOWS.getLineEnding() + "set NIC1_DNS_SUFFIX="
                + OS.WINDOWS.getLineEnding() + "set NIC1_GATEWAY="
                + OS.WINDOWS.getLineEnding() + "set NIC1_IP_ADDRESS="
                + OS.WINDOWS.getLineEnding();

        List<String> passwords = new ArrayList<String>();
        passwords.add("sunny");
        passwords.add("sunnyscript");
        passwords.add("admin123");
        // when
        String changedScript = new Script().hidePasswords(script, passwords,
                OS.WINDOWS);

        // then
        assertTrue(changedScript
                .contains("WINDOWS_LOCAL_ADMIN_PWD=" + Script.HIDDEN_PWD));
        assertTrue(changedScript
                .contains("WINDOWS_DOMAIN_ADMIN_PWD=" + Script.HIDDEN_PWD));
        assertTrue(changedScript.contains("SCRIPT_PWD=" + Script.HIDDEN_PWD));
        System.out.println(changedScript);
    }

    @Test
    public void testHidePasswordsWindows_EmptyPwd() throws Exception {
        // given
        String script = "set WINDOWS_LOCAL_ADMIN_PWD="
                + OS.WINDOWS.getLineEnding()
                + "set WINDOWS_DOMAIN_ADMIN_PWD=admin123"
                + OS.WINDOWS.getLineEnding()
                + "set DOMAIN_NAME=sunnyside.up.com"
                + OS.WINDOWS.getLineEnding() + "set INSTANCENAME=sunny"
                + OS.WINDOWS.getLineEnding()
                + "set REQUESTING_USER=Julia@DarkSide.com"
                + OS.WINDOWS.getLineEnding()
                + "set SCRIPT_URL=http://localhost:28880/test_script.sh"
                + OS.WINDOWS.getLineEnding() + "set SCRIPT_USERID=Administrator"
                + OS.WINDOWS.getLineEnding() + "set SCRIPT_PWD=sunnyscript"
                + OS.WINDOWS.getLineEnding() + "set NIC1_DNS_SERVER="
                + OS.WINDOWS.getLineEnding() + "set NIC1_DNS_SUFFIX="
                + OS.WINDOWS.getLineEnding() + "set NIC1_GATEWAY="
                + OS.WINDOWS.getLineEnding() + "set NIC1_IP_ADDRESS="
                + OS.WINDOWS.getLineEnding();

        List<String> passwords = new ArrayList<String>();
        passwords.add("");
        passwords.add("sunnyscript");
        passwords.add("admin123");
        // when
        String changedScript = new Script().hidePasswords(script, passwords,
                OS.WINDOWS);

        // then
        assertTrue(changedScript
                .contains("WINDOWS_LOCAL_ADMIN_PWD=" + Script.HIDDEN_PWD));
        assertTrue(changedScript
                .contains("WINDOWS_DOMAIN_ADMIN_PWD=" + Script.HIDDEN_PWD));
        assertTrue(changedScript.contains("SCRIPT_PWD=" + Script.HIDDEN_PWD));
        System.out.println(changedScript);
    }

}
