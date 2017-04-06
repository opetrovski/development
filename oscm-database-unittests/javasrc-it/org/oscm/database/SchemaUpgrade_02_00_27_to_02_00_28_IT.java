/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

/**
 * Test case for the "visible in catalog" flag/migration
 * 
 * @author Florian Walker
 */
public class SchemaUpgrade_02_00_27_to_02_00_28_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_00_27_to_02_00_28_IT() {
        super(new DatabaseVersionInfo(2, 0, 27), new DatabaseVersionInfo(2, 0,
                28));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_00_27_to_02_00_28.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_00_27_to_02_00_28.xml");
    }
}
