/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

public class SchemaUpgrade_02_01_01_to_02_01_02_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_01_01_to_02_01_02_IT() {
        super(new DatabaseVersionInfo(2, 1, 1),
                new DatabaseVersionInfo(2, 1, 2));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_01_01_to_02_01_02.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_01_01_to_02_01_02.xml");
    }
}
