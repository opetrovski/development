/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

/**
 * Db tests for new user role: "marketplace owner"
 * 
 * @author brandstetter
 */
public class SchemaUpgrade_02_00_44_to_02_00_45_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_00_44_to_02_00_45_IT() {
        super(new DatabaseVersionInfo(2, 0, 44), new DatabaseVersionInfo(2, 0,
                45));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_00_44_to_02_00_45.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_00_44_to_02_00_45.xml");
    }
}
