/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2017
 *
 *  Creation Date: 05.11.15 13:05
 *
 ******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

public class SchemaUpgrade_02_08_11_to_02_08_12_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_08_11_to_02_08_12_IT() {
        super(new DatabaseVersionInfo(2, 8, 11), new DatabaseVersionInfo(2, 8,
                12));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_08_11_to_02_08_12.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_08_11_to_02_08_12.xml");
    }

}
