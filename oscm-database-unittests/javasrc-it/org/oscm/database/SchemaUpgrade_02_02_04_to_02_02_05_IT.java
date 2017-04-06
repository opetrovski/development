/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

public class SchemaUpgrade_02_02_04_to_02_02_05_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_02_04_to_02_02_05_IT() {
        super(new DatabaseVersionInfo(2, 2, 4),
                new DatabaseVersionInfo(2, 2, 5));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_02_04_to_02_02_05.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_02_04_to_02_02_05.xml");
    }
}
