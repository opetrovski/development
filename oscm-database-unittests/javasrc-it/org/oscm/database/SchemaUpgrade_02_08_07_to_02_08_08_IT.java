/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.database;

import java.net.URL;

import org.oscm.setup.DatabaseVersionInfo;

/*******************************************************************************
 *
 * COPYRIGHT (C) 2015 FUJITSU Limited - ALL RIGHTS RESERVED.
 *
 * Creation Date: 2015-07-17
 *
 *******************************************************************************/

public class SchemaUpgrade_02_08_07_to_02_08_08_IT extends
        SchemaUpgradeTestBase {

    public SchemaUpgrade_02_08_07_to_02_08_08_IT() {
        super(new DatabaseVersionInfo(2, 8, 7),
                new DatabaseVersionInfo(2, 8, 8));
    }

    @Override
    protected URL getSetupDataset() {
        return getClass().getResource("/setup_02_08_07_to_02_08_08.xml");
    }

    @Override
    protected URL getExpectedDataset() {
        return getClass().getResource("/expected_02_08_07_to_02_08_08.xml");
    }

}
