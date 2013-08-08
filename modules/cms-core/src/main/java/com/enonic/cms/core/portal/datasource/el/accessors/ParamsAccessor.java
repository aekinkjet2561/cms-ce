/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.portal.datasource.el.accessors;

import com.enonic.cms.core.RequestParameters;

public final class ParamsAccessor
    implements Accessor<String[]>
{
    private final RequestParameters requestParameters;

    public ParamsAccessor( final RequestParameters requestParameters )
    {
        this.requestParameters = requestParameters;
    }

    @Override
    public String[] getValue( final String name )
    {
        if ( this.requestParameters != null )
        {
            final RequestParameters.Param parameter = this.requestParameters.getParameter( name );

            if ( parameter != null )
            {
                return parameter.getValues();
            }
        }

        return null;
    }

}
