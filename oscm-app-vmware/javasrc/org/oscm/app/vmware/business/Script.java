/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2016
 *
 *  Creation Date: 2016-05-24
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
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_TARGET_VCENTER_SERVER;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN_PWD;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_DOMAIN_JOIN;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD;
import static org.oscm.app.vmware.business.VMPropertyHandler.TS_WINDOWS_WORKGROUP;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.io.IOUtils;
import org.oscm.app.vmware.business.model.VCenter;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.oscm.app.vmware.remote.bes.ServiceParamRetrieval;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.FileTransferInformation;
import com.vmware.vim25.GuestPosixFileAttributes;
import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.GuestWindowsFileAttributes;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.VimPortType;

public class Script {

    private static final Logger LOG = LoggerFactory.getLogger(Script.class);

    private static final String POWERSHELL_EXE = "C:\\Windows\\SysWOW64\\WindowsPowerShell\\v1.0\\powershell.exe";
    private static final String LINUX_SCRIPT_DIR = "/tmp/";
    private static final String WINDOWS_SCRIPT_DIR = "C:\\Windows\\Temp\\";

    private static final String EOL_DOS = "\r\n";
    private static final String EOL_MACINTOSH = "\r";
    private static final String EOL_UNIX = "\n";

    OS os;
    VMPropertyHandler ph;
    ServiceParamRetrieval sp;
    String script;

    private String guestUserId;
    private String guestPassword;
    private String scriptFilename;
    boolean isPowerShellScript = false;
    boolean isCmdShellScript = false;
    boolean isLinuxShellScript = false;

    public enum OS {
        LINUX("\n"), WINDOWS("\r\n");

        private String lineEnding;

        private OS(String lineEnding) {
            this.lineEnding = lineEnding;
        }

        public String getLineEnding() {
            return lineEnding;
        }
    }

    private class TrustAllTrustManager implements javax.net.ssl.TrustManager,
            javax.net.ssl.X509TrustManager {

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @SuppressWarnings("unused")
        public boolean isClientTrusted(
                java.security.cert.X509Certificate[] certs) {
            return true;
        }

        @Override
        public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        @Override
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }
    }

    public Script() {

    }

    public Script(VMPropertyHandler ph, OS os) throws Exception {
        this.ph = ph;
        this.os = os;

        sp = new ServiceParamRetrieval(ph);
        guestUserId = ph.getServiceSetting(VMPropertyHandler.TS_SCRIPT_USERID);
        guestPassword = ph.getServiceSetting(VMPropertyHandler.TS_SCRIPT_PWD);

        // TODO load certificate from vSphere host and install somehow
        disableSSL();

        downloadScriptFile(ph.getServiceSetting(TS_SCRIPT_URL));
    }

    /**
     * Declare a host name verifier that will automatically enable the
     * connection. The host name verifier is invoked during the SSL handshake.
     */
    private void disableSSL() throws Exception {
        javax.net.ssl.HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };

        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager trustManager = new TrustAllTrustManager();
        trustAllCerts[0] = trustManager;

        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
                .getInstance("SSL");
        javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();
        sslsc.setSessionTimeout(0);
        sc.init(null, trustAllCerts, null);

        javax.net.ssl.HttpsURLConnection
                .setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(verifier);
    }

    void downloadScriptFile(String url) throws Exception {
        HttpURLConnection conn = null;
        int returnErrorCode = HttpURLConnection.HTTP_OK;
        StringWriter writer = new StringWriter();
        try {
            URL urlSt = new URL(url);
            conn = (HttpURLConnection) urlSt.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestMethod("GET");
            try (InputStream in = conn.getInputStream();) {
                IOUtils.copy(in, writer, "UTF-8");
            }
            returnErrorCode = conn.getResponseCode();
        } catch (Exception e) {
            LOG.error("Failed to download script file " + url, e);
            throw new Exception("Failed to download script file " + url);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        if (HttpURLConnection.HTTP_OK != returnErrorCode) {
            throw new Exception("Failed to download script file " + url);
        }

        script = writer.toString();
        scriptFilename = url.substring(url.lastIndexOf("/") + 1, url.length());

        isPowerShellScript = url.endsWith("ps1");
        isCmdShellScript = url.endsWith("bat");
        isLinuxShellScript = url.endsWith("sh");
    }

    String downloadScriptOutputFile(String url) throws Exception {
        HttpURLConnection conn = null;
        int returnErrorCode = HttpURLConnection.HTTP_OK;
        StringWriter writer = new StringWriter();
        try {
            URL urlSt = new URL(url);
            conn = (HttpURLConnection) urlSt.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestMethod("GET");
            try (InputStream in = conn.getInputStream();) {
                IOUtils.copy(in, writer, "UTF-8");
            }
            returnErrorCode = conn.getResponseCode();
        } catch (Exception e) {
            LOG.error("Failed to download script output file " + url, e);
            throw new Exception("Failed to download script output file " + url);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        if (HttpURLConnection.HTTP_OK != returnErrorCode) {
            throw new Exception("Failed to download script output file " + url);
        }

        return writer.toString();
    }

    private void uploadScriptFileToVM(VimPortType vimPort,
            ManagedObjectReference vmwInstance,
            ManagedObjectReference fileManagerRef,
            NamePasswordAuthentication auth, String script, String hostname)
            throws Exception {

        String fileUploadUrl = null;
        if (os == OS.WINDOWS) {
            GuestWindowsFileAttributes guestFileAttributes = new GuestWindowsFileAttributes();
            guestFileAttributes.setAccessTime(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(new GregorianCalendar()));
            guestFileAttributes
                    .setModificationTime(DatatypeFactory.newInstance()
                            .newXMLGregorianCalendar(new GregorianCalendar()));
            fileUploadUrl = vimPort.initiateFileTransferToGuest(fileManagerRef,
                    vmwInstance, auth, WINDOWS_SCRIPT_DIR + scriptFilename,
                    guestFileAttributes, script.length(), true);
        } else {
            GuestPosixFileAttributes guestFileAttributes = new GuestPosixFileAttributes();
            guestFileAttributes.setPermissions(Long.valueOf(500));
            guestFileAttributes.setAccessTime(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(new GregorianCalendar()));
            guestFileAttributes
                    .setModificationTime(DatatypeFactory.newInstance()
                            .newXMLGregorianCalendar(new GregorianCalendar()));
            fileUploadUrl = vimPort.initiateFileTransferToGuest(fileManagerRef,
                    vmwInstance, auth, LINUX_SCRIPT_DIR + scriptFilename,
                    guestFileAttributes, script.length(), true);
        }

        fileUploadUrl = fileUploadUrl.replaceAll("\\*", hostname);
        LOG.debug("Uploading the file to :" + fileUploadUrl);

        HttpURLConnection conn = null;
        int returnErrorCode = HttpURLConnection.HTTP_OK;

        try {
            URL urlSt = new URL(fileUploadUrl);
            conn = (HttpURLConnection) urlSt.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Length",
                    Long.toString(script.length()));
            try (OutputStream out = conn.getOutputStream();) {
                out.write(script.getBytes());
            }
            returnErrorCode = conn.getResponseCode();
        } catch (Exception e) {
            LOG.error("Failed to upload file.", e);
            throw new Exception("Failed to upload file.");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        if (HttpURLConnection.HTTP_OK != returnErrorCode) {
            throw new Exception("Failed to upload file. HTTP response code: "
                    + returnErrorCode);
        }
    }

    void insertParametersIntoScript() throws Exception {
        LOG.debug("Script before patching:\n" + script);

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

    public void execute(VMwareClient vmw, ManagedObjectReference vmwInstance)
            throws Exception {

        if (script == null || script.trim().length() == 0) {
            LOG.info("Empty script, skipped execution");
            return;
        }

        String vcenter = ph.getServiceSetting(TS_TARGET_VCENTER_SERVER);
        VimPortType vimPort = vmw.getConnection().getService();
        ServiceConnection conn = new ServiceConnection(vimPort,
                vmw.getConnection().getServiceContent());
        ManagedObjectAccessor moa = new ManagedObjectAccessor(conn);
        ManagedObjectReference guestOpManger = vmw.getConnection()
                .getServiceContent().getGuestOperationsManager();
        ManagedObjectReference fileManagerRef = (ManagedObjectReference) moa
                .getDynamicProperty(guestOpManger, "fileManager");
        ManagedObjectReference processManagerRef = (ManagedObjectReference) moa
                .getDynamicProperty(guestOpManger, "processManager");

        NamePasswordAuthentication auth = new NamePasswordAuthentication();
        auth.setUsername(guestUserId);
        auth.setPassword(guestPassword);
        auth.setInteractiveSession(false);

        insertParametersIntoScript();

        DataAccessService das = new DataAccessService(ph.getLocale());
        URL vSphereURL = new URL(das.getCredentials(vcenter).getURL());

        uploadScriptFileToVM(vimPort, vmwInstance, fileManagerRef, auth, script,
                vSphereURL.getHost());
        LOG.debug("Executing CreateTemporaryFile guest operation");
        String scriptOutputTextfile = vimPort.createTemporaryFileInGuest(
                fileManagerRef, vmwInstance, auth, "", "", "");
        LOG.debug("Successfully created a temporary file at: "
                + scriptOutputTextfile + " inside the guest");

        GuestProgramSpec spec = new GuestProgramSpec();
        if (os == OS.WINDOWS) {
            if (isPowerShellScript) {
                spec.setProgramPath(POWERSHELL_EXE);
                spec.setArguments("-command \"" + WINDOWS_SCRIPT_DIR
                        + scriptFilename + "\" > " + scriptOutputTextfile);
            } else if (isCmdShellScript) {
                spec.setProgramPath(WINDOWS_SCRIPT_DIR + scriptFilename);
                spec.setArguments(" > " + scriptOutputTextfile);
            } else {
                throw new Exception(
                        "Unknown script type. Filename must end with ps1 or bat.");
            }

            spec.setWorkingDirectory(WINDOWS_SCRIPT_DIR);
        } else {
            spec.setProgramPath(LINUX_SCRIPT_DIR + scriptFilename);
            spec.setArguments(" > " + scriptOutputTextfile + " 2>&1");
            spec.setWorkingDirectory(LINUX_SCRIPT_DIR);
        }

        LOG.debug("Starting the specified program inside the guest");
        long pid = vimPort.startProgramInGuest(processManagerRef, vmwInstance,
                auth, spec);
        LOG.debug("Process ID of the program started is: " + pid + "");

        List<GuestProcessInfo> procInfo = null;
        List<Long> pidsList = new ArrayList<>();
        pidsList.add(Long.valueOf(pid));
        do {
            LOG.debug("Waiting for the process to finish running.");
            try {
                procInfo = vimPort.listProcessesInGuest(processManagerRef,
                        vmwInstance, auth, pidsList);
            } catch (Exception e) {
                LOG.warn(
                        "listProcessesInGuest() failed. setting new Linux root password for authentication",
                        e);

                if (os == OS.WINDOWS) {
                    auth.setPassword(ph.getServiceSetting(
                            VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD));
                } else {
                    auth.setPassword(ph.getServiceSetting(
                            VMPropertyHandler.TS_LINUX_ROOT_PWD));
                }
            }
            Thread.sleep(5 * 1000);
        } while (procInfo != null && procInfo.get(0).getEndTime() == null);

        if (procInfo != null && procInfo.get(0).getExitCode().intValue() != 0) {
            LOG.error("Script return code: " + procInfo.get(0).getExitCode());
            FileTransferInformation fileTransferInformation = null;
            fileTransferInformation = vimPort.initiateFileTransferFromGuest(
                    fileManagerRef, vmwInstance, auth, scriptOutputTextfile);
            String fileDownloadUrl = fileTransferInformation.getUrl()
                    .replaceAll("\\*", vSphereURL.getHost());
            LOG.debug("Downloading the output file from :" + fileDownloadUrl);
            String scriptOutput = downloadScriptOutputFile(fileDownloadUrl);
            LOG.error("Script execution output: " + scriptOutput);
        }

    }
}
