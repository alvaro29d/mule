/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.module.db.integration.TestDbConfig.getDerbyResource;
import static org.mule.module.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.api.MuleEvent;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateJavaUdtTestCase extends AbstractDbIntegrationTestCase
{

    public UpdateJavaUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return getDerbyResource();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/update/update-udt-config.xml"};
    }

    @Test
    public void updatesWithStruct() throws Exception
    {
        MuleEvent response = runFlow("updateWithStruct", TEST_MESSAGE);

        assertThat(response.getMessage().getPayload(), Matchers.<Object>equalTo(SOUTHWEST_MANAGER.getContactDetails()));
    }
}