package com.enonic.cms.core.search.index;

public class ContentIndexNumberValueResolver
{

    public static Double resolveNumberValue( Object value )
    {
        try
        {
            final Double doubleValue = Double.parseDouble( value.toString() );
            return doubleValue;
        }
        catch ( NumberFormatException e )
        {
            return null;
        }
    }

}
