/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

public class SchemaUpgrade_02_03_04_to_02_03_05_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_03_04_to_02_03_05_IT() {
        super(new DatabaseVersionInfo(2, 3, 04), new DatabaseVersionInfo(2, 3,
                05));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_03_04_to_02_03_05.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_03_04_to_02_03_05.xml");
    }

}
