package com.enonic.cms.core.portal.datasource.handler.context;

import org.jdom.Document;
import org.springframework.stereotype.Component;

import com.enonic.cms.core.portal.datasource.handler.DataSourceRequest;
import com.enonic.cms.core.portal.datasource.handler.base.ParamDataSourceHandler;

@Component("ds.GetCookieContextHandler")
public final class GetCookieContextHandler
    extends ParamDataSourceHandler
{
    public GetCookieContextHandler()
    {
        super( "getCookieContext" );
    }

    @Override
    public Document handle( final DataSourceRequest req )
        throws Exception
    {
        return null;
    }
}