package com.raverun.im.bootstrap;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.raverun.im.infrastructure.persistence.CacheOfLoginsService;

/**
 * Bootstrap for Guice
 * 
 * @author Gavin Bong
 */
public class GuiceServletContextListener implements ServletContextListener
{
    public final static String KEY = Injector.class.getName();

    public final static String CONFIG_DB = "db";
    public final static String CONFIG_FILE = "file";

    private static final Logger logger = Logger
        .getLogger( GuiceServletContextListener.class );

    public void contextDestroyed( ServletContextEvent sce )
    {
        shutdown( sce.getServletContext() );
        sce.getServletContext().removeAttribute( KEY );
        sce.getServletContext().log( "context destroyed" );
    }

    public void contextInitialized( ServletContextEvent sce )
    {
        URI uriOfVoipProperties = null;

        try
        {
            uriOfVoipProperties = getVoipProperties();
            logger.info( "path: " + uriOfVoipProperties.getPath() );
            logger.info( "rawpath: " + uriOfVoipProperties.getRawPath() );
        }
        catch( URISyntaxException e )
        {
            throw new RuntimeException( "im.properties not found in this webapp" );
        }

        final Injector injector = getInjector( uriOfVoipProperties.getPath() );
        sce.getServletContext().setAttribute( KEY, injector );

    // Reset logins cache
        CacheOfLoginsService cacheOfLogins = injector.getInstance( CacheOfLoginsService.class );
        cacheOfLogins.purgeAll();

        sce.getServletContext().log( "context ready" );
    }

    /**
     * Entry point for shutting down services with life cycle
     */
    private void shutdown( ServletContext context )
    {
        try
        {
            Injector injector = (Injector)context.getAttribute( KEY );
            if( injector != null )
            {
                context.log( "Begin shutdown of ExecutorService.." );
                Provider<ExecutorService> executorPovider = injector.getInstance( 
                    Key.get( new TypeLiteral<Provider<ExecutorService>> () {} )
                );
                ExecutorService service = executorPovider.get();
                context.log( "\tExecutorService " + service.toString() + " is about to die.." );
                service.shutdownNow();
                service.awaitTermination( 10000, TimeUnit.MILLISECONDS );
                context.log( "\tExecutorService " + service.toString() + " is dead." );
            }
        }
        catch( InterruptedException e )
        {
            context.log( "swallowing InterruptedException during shutdown", e );
        }
    }

    private final URI getVoipProperties() throws URISyntaxException
    {
        URL url = getClass().getClassLoader().getResource( "/im.properties" );
        logger.info(  url.toString() );
        return url.toURI();
    }

    private Injector getInjector( String path )
    {
        Injector injector = Guice.createInjector(
            new AbstractModule[] { new IMModule( path ) } );
        logger.info( "Guice injector created" );
        return injector;
    }}

