/*
 * Copyright 2000-2011 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.framework.cache.standard;

import java.util.HashMap;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.framework.cache.CacheFacade;
import com.enonic.cms.framework.cache.CacheManager;
import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.boot.ConfigProperties;

/**
 * This class implements the cache.
 */
@Component
public final class StandardCacheManager
    implements CacheManager
{
    /**
     * Map of caches.
     */
    private final Map<String, StandardCacheFacade> cacheMap;

    /**
     * Cache manager config.
     */
    private CacheManagerConfig config;

    public StandardCacheManager()
    {
        this.cacheMap = new HashMap<String, StandardCacheFacade>();
    }

    @Autowired
    public void setProperties( final ConfigProperties properties )
    {
        this.config = new CacheManagerConfig( properties );
    }

    public CacheFacade getCache( final String name )
    {
        synchronized ( this.cacheMap )
        {
            return this.cacheMap.get( name );
        }
    }

    public CacheFacade getOrCreateCache( final String name )
    {
        synchronized ( this.cacheMap )
        {
            StandardCacheFacade cache = this.cacheMap.get( name );
            if ( cache == null )
            {
                cache = doCreateCache( name );
                this.cacheMap.put( name, cache );
            }

            return cache;
        }
    }

    private StandardCacheFacade doCreateCache( final String name )
    {
        return doCreateCache( name, this.config.getCacheConfig( name ) );
    }

    private StandardCacheFacade doCreateCache( final String name, final CacheConfig config )
    {
        StandardCache cache = new StandardCache( config.getMemoryCapacity() );
        return new StandardCacheFacade( name, cache, config );
    }

    public XMLDocument getInfoAsXml()
    {
        final Element root = new Element( "caches" );
        for ( final StandardCacheFacade cache : this.cacheMap.values() )
        {
            root.addContent( cache.getInfoAsXml().getAsJDOMDocument().getRootElement().detach() );
        }

        return XMLDocumentFactory.create( new Document( root ) );
    }
}
