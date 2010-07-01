/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.source;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.FlowConstruct;
import org.mule.api.FlowConstructAware;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of {@link CompositeMessageSource} that propagates both injection of
 * {@link FlowConstruct} and lifecycle to nested {@link MessageSource}s.
 * <p>
 * <li>This message source cannot be started without a listener set.
 * <li>If sources are added when this composie is started they will be started as well.
 * <li>If a {@link MessageSource} is started in isolation when composite is stopped then
 * messages will be lost.
 * <li>Message will only be received from endpoints if the connector is also started.
 */
public class StartablePatternAwareCompositeMessageSource
    implements CompositeMessageSource, Startable, Stoppable, FlowConstructAware
{
    protected static final Log log = LogFactory.getLog(StartablePatternAwareCompositeMessageSource.class);

    protected MessageProcessor listener;
    protected AtomicBoolean started = new AtomicBoolean(false);
    private MessageProcessor internalListener = new InternalMessageProcessor();
    private List<MessageSource> sources = Collections.synchronizedList(new ArrayList<MessageSource>());
    private AtomicBoolean starting = new AtomicBoolean(false);
    private FlowConstruct flowConstruct;

    public void addSource(MessageSource source) throws MuleException
    {
        sources.add(source);
        source.setListener(internalListener);
        
        if (started.get())
        {
            if (source instanceof FlowConstructAware)
            {
                ((FlowConstructAware) source).setFlowConstruct(flowConstruct);
            }
            if (source instanceof Startable)
            {
                ((Startable) source).start();
            }
        }
    }

    public void removeSource(MessageSource source) throws MuleException
    {
        if (started.get() && (source instanceof Stoppable))
        {
            ((Stoppable) source).stop();
        }
        sources.remove(source);
    }

    public void start() throws MuleException
    {
        if (listener == null)
        {
            throw new LifecycleException(CoreMessages.objectIsNull("listener"), this);
        }
        
        synchronized (sources)
        {
            starting.set(true);
            for (MessageSource source : sources)
            {
                if (source instanceof FlowConstructAware)
                {
                    ((FlowConstructAware) source).setFlowConstruct(flowConstruct);
                }
                if (source instanceof Startable)
                {
                    ((Startable) source).start();
                }
            }
            
            started.set(true);
            starting.set(false);
        }
    }

    public void stop() throws MuleException
    {
        synchronized (sources)
        {
            for (MessageSource source : sources)
            {
                if (source instanceof Stoppable)
                {
                    ((Stoppable) source).stop();
                }
            }
            
            started.set(false);
        }
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

    public void setFlowConstruct(FlowConstruct pattern)
    {
        this.flowConstruct = pattern;

    }

    @Override
    public String toString()
    {
        return "StartableMessageSourceAgregator [listener=" + listener + ", sources=" + sources
               + ", started=" + started + "]";
    }

    private class InternalMessageProcessor implements MessageProcessor
    {
        public InternalMessageProcessor()
        {
            super();
        }
        
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (started.get() || starting.get())
            {
                return listener.process(event);
            }
            else
            {
                log.warn("Message " + event
                         + " was recieved from MessageSource, but message source " + this
                         + " is stopped.  Message will be discarded.");
                return null;
            }

        }
    }
}
