/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

/*******************************************************************************
 *                                                                              
 *  COPYRIGHT (C) 2014 FUJITSU Limited - ALL RIGHTS RESERVED.                  
 *                                                                                                                                 
 *  Creation Date: 2014-05-07                                                      
 *                                                                              
 *******************************************************************************/

/**
 * @author goebel
 * 
 */
public class SchemaUpgrade_02_05_09_to_02_05_10_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_05_09_to_02_05_10_IT() {
        super(new DatabaseVersionInfo(2, 5, 9),
                new DatabaseVersionInfo(2, 5, 10));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_05_09_to_02_05_10.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_05_09_to_02_05_10.xml");
    }

}
