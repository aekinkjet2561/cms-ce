package com.enonic.cms.web.boot;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.enonic.cms.core.home.HomeResolver;
import com.enonic.cms.core.product.ProductVersion;

final class BootEnvironment
{
    private final static Logger LOG = LoggerFactory.getLogger( BootEnvironment.class );

    private final static String BANNER = "\n" +
        " _______ _______ _______ _______ _______ ______   ______ _______ _______ \n" +
        "|    ___|    |  |       |    |  |_     _|      | |      |   |   |     __|\n" +
        "|    ___|       |   -   |       |_|   |_|   ---| |   ---|       |__     |\n" +
        "|_______|__|____|_______|__|____|_______|______| |______|__|_|__|_______|\n\n";

    public void initialize()
    {
        logBanner();
        resolveHomeDir();
    }

    public void destroy()
    {
        // Do nothing for now
    }

    private void resolveHomeDir()
    {
        final HomeResolver resolver = new HomeResolver();
        resolver.resolve();
    }

    private void logBanner()
    {
        final StringBuilder str = new StringBuilder( BANNER );
        str.append( "  # " ).append( ProductVersion.getFullTitleAndVersion() ).append( "\n" );
        str.append( "  # " ).append( getFormattedJvmInfo() ).append( "\n" );
        str.append( "  # " ).append( getFormattedOsInfo() ).append( "\n" );

        LOG.info( str.toString() );
    }

    private String getFormattedJvmInfo()
    {
        final StringBuilder str = new StringBuilder();
        str.append( SystemUtils.JAVA_RUNTIME_NAME ).append( " " ).append( SystemUtils.JAVA_RUNTIME_VERSION ).append( " (" ).append(
            SystemUtils.JAVA_VENDOR ).append( ")" );
        return str.toString();
    }

    private String getFormattedOsInfo()
    {
        final StringBuilder str = new StringBuilder();
        str.append( SystemUtils.OS_NAME ).append( " " ).append( SystemUtils.OS_VERSION ).append( " (" ).append(
            SystemUtils.OS_ARCH ).append( ")" );
        return str.toString();
    }
}