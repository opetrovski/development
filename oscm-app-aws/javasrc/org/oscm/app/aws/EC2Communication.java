/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2016                                        
 *                                                                              
 *  Creation Date: 2013-10-17                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.oscm.app.aws.controller.PropertyHandler;
import org.oscm.app.aws.i18n.Messages;
import org.oscm.app.v1_0.exceptions.APPlatformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

public class EC2Communication {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EC2Communication.class);

    // EC2 client stub for unit testing
    private static AmazonEC2Client ec2_stub;

    private final PropertyHandler ph;
    private final AWSCredentialsProvider credentialsProvider;
    private AmazonEC2Client ec2;

    private static final String ENDPOINT_PREFIX = "ec2.";
    private static final String ENDPOINT_SUFFIX = ".amazonaws.com";
    private static final String HTTPS_PROXY_HOST = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT = "https.proxyPort";
    private static final String HTTPS_PROXY_USER = "https.proxyUser";
    private static final String HTTPS_PROXY_PASSWORD = "https.proxyPassword";
    private static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";

    /**
     * Constructor
     * 
     * @param PropertyHandler
     *            ph
     */
    public EC2Communication(PropertyHandler ph) {
        this.ph = ph;
        final String secretKey = ph.getSecretKey();
        final String accessKeyId = ph.getAccessKeyId();
        credentialsProvider = new AWSCredentialsProvider() {

            @Override
            public void refresh() {
            }

            @Override
            public AWSCredentials getCredentials() {

                return new AWSCredentials() {

                    @Override
                    public String getAWSSecretKey() {
                        return secretKey;
                    }

                    @Override
                    public String getAWSAccessKeyId() {
                        return accessKeyId;
                    }
                };
            }
        };
    }

    /**
     * Return amazon interface
     * 
     * @return AmazonEC2Client ec2
     */
    AmazonEC2Client getEC2() {
        if (ec2 == null) {
            String endpoint = ENDPOINT_PREFIX + ph.getRegion()
                    + ENDPOINT_SUFFIX;
            String proxyHost = System.getProperty(HTTPS_PROXY_HOST);
            String proxyPort = System.getProperty(HTTPS_PROXY_PORT);
            String proxyUser = System.getProperty(HTTPS_PROXY_USER);
            String proxyPassword = System.getProperty(HTTPS_PROXY_PASSWORD);

            int proxyPortInt = 0;
            try {
                proxyPortInt = Integer.parseInt(proxyPort);
            } catch (NumberFormatException e) {
                // ignore
            }
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            if (!isNonProxySet(endpoint)) {
                if (proxyHost != null) {
                    clientConfiguration.setProxyHost(proxyHost);
                }
                if (proxyPortInt > 0) {
                    clientConfiguration.setProxyPort(proxyPortInt);
                }
                if (proxyUser != null && proxyUser.length() > 0) {
                    clientConfiguration.setProxyUsername(proxyUser);
                }
                if (proxyPassword != null && proxyPassword.length() > 0) {
                    clientConfiguration.setProxyPassword(proxyPassword);
                }
            }
            ec2 = getEC2(credentialsProvider, clientConfiguration);
            ec2.setEndpoint(endpoint);
        }
        return ec2;
    }

    /**
     * Define AWS mockup for unit tests
     * 
     * @param AmazonEC2Client
     *            ec2
     */
    public static void useMock(AmazonEC2Client ec2) {
        ec2_stub = ec2;
    }

    /**
     * Allow mocking of EC2 client by having it in separate creation method
     * 
     * @param AWSCredentialsProvider
     * @param ClientConfiguration
     */
    AmazonEC2Client getEC2(AWSCredentialsProvider credentialsProvider,
            ClientConfiguration clientConfiguration) {
        if (ec2 == null) {
            ec2 = (ec2_stub != null) ? ec2_stub
                    : new AmazonEC2Client(credentialsProvider,
                            clientConfiguration);
        }
        return ec2;
    }

    /**
     * Checks whether system proxy settings tell to omit proxying for given
     * endpoint.
     * 
     * @param endpoint
     * @return <code>true</code> if the endpoint matches one of the nonProxy
     *         settings
     */
    boolean isNonProxySet(String endpoint) {
        String nonProxy = System.getProperty(HTTP_NON_PROXY_HOSTS);
        if (nonProxy != null) {
            String[] split = nonProxy.split("\\|");
            for (int i = 0; i < split.length; i++) {
                String np = split[i].trim();
                if (np.length() > 0) {
                    boolean wcStart = np.startsWith("*");
                    boolean wcEnd = np.endsWith("*");
                    if (wcStart) {
                        np = np.substring(1);
                    }
                    if (wcEnd) {
                        np = np.substring(0, np.length() - 1);
                    }
                    if (wcStart && wcEnd && endpoint.contains(np)) {
                        return true;
                    }
                    if (wcStart && endpoint.endsWith(np)) {
                        return true;
                    }
                    if (wcEnd && endpoint.startsWith(np)) {
                        return true;
                    }
                    if (np.equals(endpoint)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String resolveAMI(String imageName) throws APPlatformException {

        LOGGER.debug("resolveAMI('{}') entered", imageName);
        DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
        Collection<Filter> filters = new ArrayList<>();
        filters.add(new Filter("name").withValues(imageName));

        DescribeImagesResult result = getEC2()
                .describeImages(describeImagesRequest.withFilters(filters));
        List<Image> images = result.getImages();
        LOGGER.debug("  number of images found: {}",
                Integer.valueOf(images.size()));
        for (Image image : images) {
            LOGGER.debug("  return image with id {}", image.getImageId());
            return image.getImageId();
        }
        throw new APPlatformException(
                Messages.getAll("error_invalid_image", imageName));
    }

    public void createInstance(String imageId) throws APPlatformException {

        LOGGER.debug("createInstance('{}') entered", imageId);

        Integer count = ph.getCreateInstanceCount();
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withInstanceType(ph.getInstanceType()).withImageId(imageId)
                .withMinCount(count).withMaxCount(count)
                .withKeyName(ph.getKeyPairName());
        String userData = ph.getUserData();
        if (userData != null && userData.trim().length() > 0) {
            runInstancesRequest.setUserData(getTextBASE64(userData));
        }
        Collection<String> securityGroups = ph.getSecurityGroups();
        if (securityGroups.size() > 0) {
            runInstancesRequest.setSecurityGroups(securityGroups);
        }

        RunInstancesResult result = getEC2().runInstances(runInstancesRequest);
        List<Instance> reservedInstances = result.getReservation()
                .getInstances();

        if (reservedInstances.isEmpty()) {
            throw new APPlatformException(
                    Messages.getAll("error_no_reserved_instance"));
        }

        int numInstance = 0;
        for (Instance instance : reservedInstances) {
            String instanceId = instance.getInstanceId();
            if (numInstance == 0) {
                ph.setAWSInstanceId(instanceId);
                createTags(ph);
                numInstance++;
            } else {
                String instanceName = ph.getInstanceName() + " "
                        + Integer.toString(numInstance);
                createCopyTags(instanceName,
                        ph.getSettings().getOrganizationId(), instanceId);
                numInstance++;
            }
        }
    }

    public void modifyInstance() throws APPlatformException {
        createTags(ph);
    }

    public void terminateInstance(String instanceId) {
        getEC2().terminateInstances(
                new TerminateInstancesRequest().withInstanceIds(instanceId));
    }

    public void startInstance(String instanceId) {
        getEC2().startInstances(
                new StartInstancesRequest().withInstanceIds(instanceId));
    }

    public void stopInstance(String instanceId) {
        getEC2().stopInstances(
                new StopInstancesRequest().withInstanceIds(instanceId));
    }

    public String getInstanceState(String instanceId) {
        LOGGER.debug("getInstanceState('{}') entered", instanceId);
        DescribeInstancesResult result = getEC2().describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceId));
        List<Reservation> reservations = result.getReservations();
        Set<Instance> instances = new HashSet<>();

        for (Reservation reservation : reservations) {
            instances.addAll(reservation.getInstances());
            if (instances.size() > 0) {
                String state = instances.iterator().next().getState().getName();
                LOGGER.debug("  InstanceState: {}", state);
                return state;
            }
        }
        LOGGER.debug("getInstanceState('{}') left", instanceId);
        return null;
    }

    public boolean isInstanceReady(String instanceId) {
        LOGGER.debug("isInstanceReady('{}') entered", instanceId);
        DescribeInstanceStatusResult result = getEC2()
                .describeInstanceStatus(new DescribeInstanceStatusRequest()
                        .withInstanceIds(instanceId));
        List<InstanceStatus> statusList = result.getInstanceStatuses();
        boolean instanceStatus = false;
        boolean systemStatus = false;

        for (InstanceStatus status : statusList) {
            LOGGER.debug("  InstanceState:    {}", status.getInstanceState());
            LOGGER.debug("  InstanceStatus:   {}",
                    status.getInstanceStatus().getStatus());
            LOGGER.debug("  SystemStatus:     {}",
                    status.getSystemStatus().getStatus());
            LOGGER.debug("  AvailabilityZone: {}",
                    status.getAvailabilityZone());

            instanceStatus = ("ok"
                    .equals(status.getInstanceStatus().getStatus()));
            systemStatus = ("ok".equals(status.getSystemStatus().getStatus()));
        }
        LOGGER.debug("isInstanceReady('{}') left", instanceId);
        return instanceStatus && systemStatus;
    }

    public String getPublicDNS(String instanceId) {
        DescribeInstancesResult result = getEC2().describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceId));
        List<Reservation> reservations = result.getReservations();
        Set<Instance> instances = new HashSet<>();

        for (Reservation reservation : reservations) {
            instances.addAll(reservation.getInstances());
            if (instances.size() > 0) {
                return instances.iterator().next().getPublicDnsName();
            }
        }
        return null;
    }

    private String getTextBASE64(String url) throws APPlatformException {
        InputStream cin = null;
        try {
            URL source = new URL(url);
            URLConnection connection = source.openConnection();
            cin = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(cin));

            StringBuilder response = new StringBuilder();
            char[] buffer = new char[1024];
            int n = 0;
            while (-1 != (n = in.read(buffer))) {
                response.append(buffer, 0, n);
            }

            in.close();
            LOGGER.debug(response.toString());
            return Base64
                    .encodeBase64String(response.toString().getBytes("UTF-8"));

        } catch (MalformedURLException e) {
            throw new APPlatformException(
                    "Reading userdata failed: " + e.getMessage());
        } catch (IOException e) {
            throw new APPlatformException(
                    "Reading userdata failed: " + e.getMessage());
        } finally {
            if (cin != null) {
                try {
                    cin.close();
                } catch (IOException e) {
                    // ignore, wanted to close anyway
                }

            }
        }
    }

    private void createTags(PropertyHandler ph) throws APPlatformException {
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag(PropertyHandler.TAG_NAME, ph.getInstanceName()));
        tags.add(new Tag(PropertyHandler.TAG_SUBSCRIPTION_ID,
                ph.getSettings().getSubscriptionId()));
        tags.add(new Tag(PropertyHandler.TAG_ORGANIZATION_ID,
                ph.getSettings().getOrganizationId()));
        CreateTagsRequest ctr = new CreateTagsRequest();
        LOGGER.debug("attach tags to resource " + ph.getAWSInstanceId());
        ctr.withResources(ph.getAWSInstanceId()).setTags(tags);
        getEC2().createTags(ctr);
    }

    private void createCopyTags(String instanceName, String orgId,
            String instanceId) throws APPlatformException {
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag(PropertyHandler.TAG_NAME, instanceName));
        tags.add(new Tag(PropertyHandler.TAG_ORGANIZATION_ID, orgId));
        CreateTagsRequest ctr = new CreateTagsRequest();
        LOGGER.debug("attach tags to resource " + instanceId);
        ctr.withResources(instanceId).setTags(tags);
        getEC2().createTags(ctr);
    }

}
