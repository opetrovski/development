/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2016
 *                                                                              
 *  Author: weiser                                                   
 *                                                                              
 *  Creation Date: 13.12.2011                                                      
 *                                                                              
 *  Completion Time: 13.12.2011                                              
 *                                                                              
 *******************************************************************************/

package org.oscm.ess.ws.v1_7;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.oscm.ess.ws.v1_7.base.ServiceFactory;
import org.oscm.ess.ws.v1_7.base.WebserviceTestBase;
import org.oscm.ess.ws.v1_7.base.WebserviceTestSetup;

import org.oscm.string.Strings;
import com.fujitsu.bss.intf.IdentityService;
import com.fujitsu.bss.types.exceptions.BulkUserImportException;
import com.fujitsu.bss.types.exceptions.BulkUserImportException.Reason;
import com.fujitsu.bss.types.exceptions.ObjectNotFoundException;
import com.fujitsu.bss.vo.VOOrganization;
import com.fujitsu.bss.vo.VOUserDetails;

/**
 * Tests for {@link IdentityService} web service.
 * 
 * @author weiser
 * 
 */
public class IdentityServiceWSPlatformOperatorTest {
    private static WebserviceTestSetup setup;
    private static IdentityService is;
    private static VOOrganization supplier1;

    @BeforeClass
    public static void setUp() throws Exception {
        WebserviceTestBase.getMailReader().deleteMails();
        WebserviceTestBase.getOperator().addCurrency("EUR");

        setup = new WebserviceTestSetup();
        supplier1 = setup.createSupplier("Supplier1");
        is = ServiceFactory.getDefault().getIdentityService(
                WebserviceTestBase.getPlatformOperatorKey(),
                WebserviceTestBase.getPlatformOperatorPassword());
        VOUserDetails userDetails = is.getCurrentUserDetails();
        userDetails.setEMail(WebserviceTestBase.getMailReader()
                .getMailAddress());
        is.updateUser(userDetails);

        WebserviceTestBase.getMailReader().deleteMails();
    }

    @Test
    public void importUsers() throws Exception {
        // given
        byte[] csvData = bytes("user_" + System.currentTimeMillis() + ","
                + "user1@org.com,en,MR,John,Doe,SERVICE_MANAGER");

        // when
        is.importUsers(csvData, supplier1.getOrganizationId(), "");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void importUsers_InvalidOrganizationId() throws Exception {
        // given
        String orgId = "not_existing_id";
        byte[] csvData = bytes("user_" + System.currentTimeMillis() + ","
                + "user1@org.com,en,MR,John,Doe,ORGANIZATION_ADMIN");

        // when
        is.importUsers(csvData, orgId, "");
    }

    void validateException(BulkUserImportException e, Reason reason) {
        assertEquals("ex.BulkUserImportException."
                + e.getFaultInfo().getReason().name(), e.getMessageKey());
        assertEquals(reason, e.getFaultInfo().getReason());
    }

    private byte[] bytes(String value) {
        return Strings.toBytes(value);
    }
}