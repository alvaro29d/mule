/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.listener;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

@RunWith(Parameterized.class)
public class MultipleListenerTestCase extends FunctionalTestCase
{

    private static final String path1 = "api/sys/ref/batterytype";
    private static final String path2 = "api/sys/ref/batterytype/1";
    private static final String response1 = "Response via /api/*";
    private static final String URL = "http://localhost:%s/%s";
    private final String path;
    private final String response;

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");


    public MultipleListenerTestCase(String path, String response)
    {
        this.path = path;
        this.response = response;
    }

    @Override
    protected String getConfigFile()
    {
        return "http-listener-multiple-listeners.xml";
    }

    @Parameters
    public static Collection<Object[]> data()
    {
        return asList(new Object[][] {{path1, response1}, {path2, response1}});
    }

    @Test
    public void testPath() throws Exception
    {
        final String url = String.format(URL, listenPort.getNumber(), path);
        final Response httpResponse = Request.Get(url).execute();
        assertThat(httpResponse.returnContent().asString(), is(response));
    }

}
