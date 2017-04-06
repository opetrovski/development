/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

public class SchemaUpgrade_02_05_01_to_02_05_02_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_05_01_to_02_05_02_IT() {
        super(new DatabaseVersionInfo(2, 5, 1),
                new DatabaseVersionInfo(2, 5, 2));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_05_01_to_02_05_02.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_05_01_to_02_05_02.xml");
    }
}
