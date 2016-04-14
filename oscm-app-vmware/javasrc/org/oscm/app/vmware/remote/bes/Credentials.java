/*******************************************************************************
 *                                                                              
 *  COPYRIGHT (C) 2013 FUJITSU Limited - ALL RIGHTS RESERVED.                  
 *                                                                              
 *  Creation Date: 22.04.2013                                                      
 *                                                                              
 *******************************************************************************/
package org.oscm.app.vmware.remote.bes;

import org.oscm.app.v1_0.data.PasswordAuthentication;

/**
 * Object representing BES user credentials.
 */
public class Credentials {

    private boolean isSSO;
    private long userKey;
    private String userId;
    private String password;
    private String orgId;

    public Credentials(boolean isSSO) {
        this.isSSO = isSSO;
    }

    public Credentials(boolean isSSO, String userId, String password) {
        this.isSSO = isSSO;
        this.userId = userId;
        this.password = password;
    }

    public Credentials(boolean isSSO, long userKey, String password) {
        this.isSSO = isSSO;
        this.userKey = userKey;
        this.password = password;
    }

    public long getUserKey() {
        return userKey;
    }

    public void setUserKey(long userKey) {
        this.userKey = userKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public PasswordAuthentication getPasswordAuthentication() {
        PasswordAuthentication pa = (isSSO)
                ? new PasswordAuthentication(userId, password)
                : new PasswordAuthentication(Long.toString(userKey), password);
        return pa;
    }

}
