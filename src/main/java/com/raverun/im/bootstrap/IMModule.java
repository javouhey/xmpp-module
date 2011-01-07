package com.raverun.im.bootstrap;

import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import com.google.common.base.Predicate;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;
import com.google.inject.name.Names;
import com.raverun.cache.CacheClient;
import com.raverun.cache.annotation.Ehcache;
import com.raverun.cache.ehcache.DefaultCacheManagerProvider;
import com.raverun.cache.ehcache.EhcacheClientFacade;
import com.raverun.im.application.AccountService;
import com.raverun.im.application.IMTypeTransportMapper;
import com.raverun.im.application.JIDMapper;
import com.raverun.im.application.impl.AccountServiceImpl;
import com.raverun.im.application.impl.IMTypeTransportMapperImpl;
import com.raverun.im.application.impl.JIDMapperImpl;
import com.raverun.im.common.GoogleEmailPredicate;
import com.raverun.im.common.MSNEmailPredicate;
import com.raverun.im.common.QQIdPredicate;
import com.raverun.im.common.UserExistsPredicate;
import com.raverun.im.common.YahooOriginalEmailPredicate;
import com.raverun.im.common.YahooRocketmailEmailPredicate;
import com.raverun.im.common.YahooYmailEmailPredicate;
import com.raverun.im.domain.DomainUtility;
import com.raverun.im.domain.IMConfiguration;
import com.raverun.im.domain.IMConnectionFactory;
import com.raverun.im.domain.IMSessionFactory;
import com.raverun.im.domain.IMUserSettingFactory;
import com.raverun.im.domain.IMUserSettingFactory2;
import com.raverun.im.domain.PacketListenerForIQFactory;
import com.raverun.im.domain.PacketListenerForMessageFactory;
import com.raverun.im.domain.PacketListenerForPresenceFactory;
import com.raverun.im.domain.PacketListenerForRosterFactory;
import com.raverun.im.domain.RosterListenerFactory;
import com.raverun.im.domain.SignInResultConjoiner;
import com.raverun.im.domain.impl.DomainUtilityImpl;
import com.raverun.im.domain.impl.IMConfigurationImpl;
import com.raverun.im.domain.impl.IMConnectionImpl;
import com.raverun.im.domain.impl.IMSessionImpl;
import com.raverun.im.domain.impl.IMUserSettingFactory2Impl;
import com.raverun.im.domain.impl.IMUserSettingImpl;
import com.raverun.im.domain.impl.PacketListenerForIQ;
import com.raverun.im.domain.impl.PacketListenerForMessage;
import com.raverun.im.domain.impl.PacketListenerForPresence;
import com.raverun.im.domain.impl.PacketListenerForRosterEntries;
import com.raverun.im.domain.impl.RosterListenerImpl;
import com.raverun.im.domain.impl.SignInResultConjoinerImpl;
import com.raverun.im.infrastructure.persistence.CacheOfLoginsService;
import com.raverun.im.infrastructure.persistence.JDBC2JPAExceptionTranslator;
import com.raverun.im.infrastructure.persistence.MessageService;
import com.raverun.im.infrastructure.persistence.OfflineService;
import com.raverun.im.infrastructure.persistence.PresenceService;
import com.raverun.im.infrastructure.persistence.SettingsService;
import com.raverun.im.infrastructure.persistence.SubscriptionService;
import com.raverun.im.infrastructure.persistence.TransportMapper;
import com.raverun.im.infrastructure.persistence.UserService;
import com.raverun.im.infrastructure.persistence.jdbc.CacheOfLoginsDao;
import com.raverun.im.infrastructure.persistence.jdbc.CacheOfLoginsDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.IMMessageChatDao;
import com.raverun.im.infrastructure.persistence.jdbc.IMMessageChatDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.IMMessageHeadlineDao;
import com.raverun.im.infrastructure.persistence.jdbc.IMMessageHeadlineDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.IMOfflineDao;
import com.raverun.im.infrastructure.persistence.jdbc.IMOfflineDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.IMPresenceDao;
import com.raverun.im.infrastructure.persistence.jdbc.IMPresenceDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.IMSubscriptionRequestDao;
import com.raverun.im.infrastructure.persistence.jdbc.IMSubscriptionRequestDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.IMUserSettingDao;
import com.raverun.im.infrastructure.persistence.jdbc.IMUserSettingDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.IMUserXmppDao;
import com.raverun.im.infrastructure.persistence.jdbc.IMUserXmppDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.JDBC2JPAExceptionTranslatorImpl;
import com.raverun.im.infrastructure.persistence.jdbc.JDBCCacheOfLoginsService;
import com.raverun.im.infrastructure.persistence.jdbc.JDBCMessageService;
import com.raverun.im.infrastructure.persistence.jdbc.JDBCOfflineService;
import com.raverun.im.infrastructure.persistence.jdbc.JDBCPresenceService;
import com.raverun.im.infrastructure.persistence.jdbc.JDBCSettingsService;
import com.raverun.im.infrastructure.persistence.jdbc.JDBCSubscriptionService;
import com.raverun.im.infrastructure.persistence.jdbc.JDBCTransportMapper;
import com.raverun.im.infrastructure.persistence.jdbc.JDBCUserService;
import com.raverun.im.infrastructure.persistence.jdbc.TransportDao;
import com.raverun.im.infrastructure.persistence.jdbc.TransportDaoFactory;
import com.raverun.im.infrastructure.persistence.jdbc.UserDao;
import com.raverun.im.infrastructure.persistence.jdbc.UserDaoFactory;
import com.raverun.im.infrastructure.system.CacheKeyGenerator;
import com.raverun.im.infrastructure.system.CacheOfLogins;
import com.raverun.im.infrastructure.system.CacheOfNickNames;
import com.raverun.im.infrastructure.system.IdMutexForLogins;
import com.raverun.im.infrastructure.system.IdMutexProvider;
import com.raverun.im.infrastructure.system.JavaSimonTimer;
import com.raverun.im.infrastructure.system.PasswordCipher;
import com.raverun.im.infrastructure.system.UUIDGenerator;
import com.raverun.im.infrastructure.system.impl.CacheKeyGeneratorImpl;
import com.raverun.im.infrastructure.system.impl.CacheOfLoginsImpl;
import com.raverun.im.infrastructure.system.impl.CacheOfNickNamesImpl;
import com.raverun.im.infrastructure.system.impl.ExecutorServiceProvider;
import com.raverun.im.infrastructure.system.impl.IdMutexForLoginsImpl;
import com.raverun.im.infrastructure.system.impl.IdMutexProviderImpl;
import com.raverun.im.infrastructure.system.impl.JavaSimonTimerImpl;
import com.raverun.im.infrastructure.system.impl.MCCipherImpl;
import com.raverun.im.infrastructure.system.impl.PasswordCipherImpl;
import com.raverun.im.infrastructure.system.impl.UUIDGeneratorEaio;
import com.raverun.im.infrastructure.xmpp.XMPPAccountService;
import com.raverun.im.infrastructure.xmpp.XMPPUtility;
import com.raverun.im.infrastructure.xmpp.impl.XMPPAccountServiceImpl;
import com.raverun.im.infrastructure.xmpp.impl.XMPPUtilityImpl;
import com.raverun.im.infrastructure.xmpp.ops.MTAcceptBuddyOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTAddBuddyOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTDeregisterAccountOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTDiscoTransportOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTRegisterAccountOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTRejectBuddyOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTSendChatMessageOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTSetModeOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTSigninGatewayOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.MTSignoutGatewayOperationFactory;
import com.raverun.im.infrastructure.xmpp.ops.SigninResponseProcessor;
import com.raverun.im.infrastructure.xmpp.ops.impl.Kraken112SigninResponseProcessorForMsn;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTAcceptBuddyOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTAddBuddyOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTDeregisterAccountOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTDiscoTransportOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTRegisterAccountOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTRejectBuddyOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTSendChatMessageOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTSetModeOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTSigninGatewayOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.MTSignoutGatewayOperationImpl;
import com.raverun.im.infrastructure.xmpp.ops.impl.SigninResponseProcessorForGtalk;
import com.raverun.im.infrastructure.xmpp.ops.impl.SigninResponseProcessorForYahoo;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityIF;
import com.raverun.im.infrastructure.xmpp.smack.PresenceUtilityImpl;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIF;
import com.raverun.im.infrastructure.xmpp.smack.XMPPConnectionIFProvider;
import com.raverun.im.interfaces.rest.FaultBarrierHandler;
import com.raverun.im.interfaces.rest.OutgoingMediaTypeSelector;
import com.raverun.im.interfaces.rest.ProtocolBodyParser;
import com.raverun.im.interfaces.rest.ProtocolBodyParserRegistry;
import com.raverun.im.interfaces.rest.ProtocolBodyParserSelectorFactory;
import com.raverun.im.interfaces.rest.ProtocolProcessor;
import com.raverun.im.interfaces.rest.ProtocolProcessorDefault;
import com.raverun.im.interfaces.rest.ProtocolProcessorSelector;
import com.raverun.im.interfaces.rest.ProtocolUtils;
import com.raverun.im.interfaces.rest.Resource;
import com.raverun.im.interfaces.rest.ResourceDispatcher;
import com.raverun.im.interfaces.rest.ResourceRegistry;
import com.raverun.im.interfaces.rest.SessionUtilsFactory;
import com.raverun.im.interfaces.rest.SupportedIncomingMediaType;
import com.raverun.im.interfaces.rest.SupportedOutgoingMediaTypes;
import com.raverun.im.interfaces.rest.VersionChecker;
import com.raverun.im.interfaces.rest.impl.DefaultOutgoingMediaTypeSelector;
import com.raverun.im.interfaces.rest.impl.DefaultSupportedIncomingMediaType;
import com.raverun.im.interfaces.rest.impl.DefaultSupportedOutgoingMediaTypes;
import com.raverun.im.interfaces.rest.impl.FaultBarrierHandlerImpl;
import com.raverun.im.interfaces.rest.impl.ProtocolBodyParserJSON;
import com.raverun.im.interfaces.rest.impl.ProtocolBodyParserRegistryImpl;
import com.raverun.im.interfaces.rest.impl.ProtocolBodyParserSelectorImpl;
import com.raverun.im.interfaces.rest.impl.ProtocolBodyParserXML;
import com.raverun.im.interfaces.rest.impl.ProtocolProcessorDefaultImpl;
import com.raverun.im.interfaces.rest.impl.ProtocolProcessorOne;
import com.raverun.im.interfaces.rest.impl.ProtocolProcessorSelectorImpl;
import com.raverun.im.interfaces.rest.impl.ProtocolProcessorZero;
import com.raverun.im.interfaces.rest.impl.ProtocolUtilsImpl;
import com.raverun.im.interfaces.rest.impl.SessionUtilsImpl;
import com.raverun.im.interfaces.rest.impl.VersionCheckerImpl;
import com.raverun.im.interfaces.rest.impl.resources.BuddyEntityParser;
import com.raverun.im.interfaces.rest.impl.resources.ResourceDispatcherImpl;
import com.raverun.im.interfaces.rest.impl.resources.ResourceRegistryImpl;
import com.raverun.im.interfaces.rest.impl.resources.TransportParser;
import com.raverun.im.interfaces.rest.impl.resources.XMPPGatewayParser;
import com.raverun.im.interfaces.rest.impl.resources.buddy.BuddyEntityParserImpl;
import com.raverun.im.interfaces.rest.impl.resources.buddy.BuddyResource;
import com.raverun.im.interfaces.rest.impl.resources.cache.CacheResource;
import com.raverun.im.interfaces.rest.impl.resources.chat.ChatResource;
import com.raverun.im.interfaces.rest.impl.resources.mode.ModeResource;
import com.raverun.im.interfaces.rest.impl.resources.session.SessionResource;
import com.raverun.im.interfaces.rest.impl.resources.transport.TransportParserImpl;
import com.raverun.im.interfaces.rest.impl.resources.transport.TransportResource;
import com.raverun.im.interfaces.rest.impl.resources.updates.UpdatesResource;
import com.raverun.im.interfaces.rest.impl.resources.user.UserResource;
import com.raverun.im.interfaces.rest.impl.resources.xmpp.XMPPGatewayParserImpl;
import com.raverun.im.interfaces.rest.impl.resources.xmpp.XMPPGatewayResource;
import com.raverun.im.interfaces.rest.support.guice.JSON;
import com.raverun.im.interfaces.rest.support.guice.One;
import com.raverun.im.interfaces.rest.support.guice.XML;
import com.raverun.im.interfaces.rest.support.guice.Zero;
import com.raverun.shared.Configuration;
import com.raverun.shared.Obfuscator;
import com.raverun.shared.impl.FileConfiguration;
import com.raverun.shared.impl.ObfuscatorJasypt;
import com.raverun.shared.impl.ObfuscatorRot47;
import com.raverun.shared.persistence.DataSourceProvider;
import com.raverun.shared.services.DbLogger;
import com.raverun.shared.services.MCCipher;
import com.raverun.shared.services.PropertyFile;
import com.raverun.shared.services.impl.DbLoggerImpl;
import com.raverun.shared.services.impl.PropertyFileImpl;

public class IMModule extends AbstractModule
{
    private final String path;

    public IMModule( String path )
    {
        this.path = path;
    }

    @Override
    protected void configure()
    {
    // constants --------------------

        bindConstant().annotatedWith( Names.named( "autologin.on.session.start" ) ).to( Boolean.TRUE );

        bindConstant().annotatedWith( Names.named( "xmpp.generic.password" ) ).to( "1234fhgfa6tgu" );

        bindConstant().annotatedWith( Names.named( "password.cipher.extractor" ) ).to( 5 );

        bindConstant().annotatedWith( Names.named( "presence.status" ) ).to( "on raverun (mobile)" );

        /**
         * Used in {@code SigninGatewayOperationImpl}
         */
        bindConstant().annotatedWith( Names.named( "default.roster.group" ) ).to( "buddies" );

    // core -------------------------

        bindConstant().annotatedWith( Names.named( "filename" ) ).to( path );

        bind( Obfuscator.class ).annotatedWith( Names.named("rot47") ).
            to( ObfuscatorRot47.class ).in( Scopes.SINGLETON );

        bind( Obfuscator.class ).annotatedWith( Names.named("jasypt") ).
            to( ObfuscatorJasypt.class ).in( Scopes.SINGLETON );

        bind( DataSource.class ).toProvider( DataSourceProvider.class ).in( Scopes.SINGLETON );

        bind( DbLogger.class ).to( DbLoggerImpl.class ).in( Scopes.SINGLETON );

        bind( PropertyFile.class ).to( PropertyFileImpl.class ).in( Scopes.SINGLETON );

        bind( Configuration.class ).to( FileConfiguration.class ).in( Scopes.SINGLETON );

        bind( VersionChecker.class ).to( VersionCheckerImpl.class ).in( Scopes.SINGLETON );

        bind( ProtocolUtils.class ).to( ProtocolUtilsImpl.class ).in( Scopes.SINGLETON );

        bind( ProtocolBodyParser.class ).annotatedWith( JSON.class ).to( ProtocolBodyParserJSON.class ).in( Scopes.SINGLETON );

        bind( ProtocolBodyParser.class ).annotatedWith( XML.class ).to( ProtocolBodyParserXML.class ).in( Scopes.SINGLETON );

        bind( ProtocolBodyParserRegistry.class ).to( ProtocolBodyParserRegistryImpl.class ).in( Scopes.SINGLETON );

        bind( ProtocolBodyParserSelectorFactory.class )
            .toProvider( FactoryProvider.newFactory( ProtocolBodyParserSelectorFactory.class,
                ProtocolBodyParserSelectorImpl.class ) );

        bind( IdMutexProvider.class ).to( IdMutexProviderImpl.class ).in( Scopes.SINGLETON );

        bind( UUIDGenerator.class ).to( UUIDGeneratorEaio.class  ).in( Scopes.SINGLETON );

        bind( PasswordCipher.class ).to( PasswordCipherImpl.class ).in( Scopes.SINGLETON );

        bind( JavaSimonTimer.class ).to( JavaSimonTimerImpl.class ).in( Scopes.SINGLETON );

        bind( ExecutorService.class ).toProvider( ExecutorServiceProvider.class ).in( Scopes.SINGLETON );

        bind( IMConfiguration.class ).to( IMConfigurationImpl.class ).in( Scopes.SINGLETON );

        bind( MCCipher.class ).to( MCCipherImpl.class ).in( Scopes.SINGLETON );

    // XMPP response processor ------------------

        bind( SigninResponseProcessor.class ).annotatedWith( Names.named( "resp.yahoo" ) ).to( SigninResponseProcessorForYahoo.class ).in( Scopes.SINGLETON );

        bind( SigninResponseProcessor.class ).annotatedWith( Names.named( "resp.gtalk" ) ).to( SigninResponseProcessorForGtalk.class ).in( Scopes.SINGLETON );

//        bind( SigninResponseProcessor.class ).annotatedWith( Names.named( "resp.msn" ) ).to( SigninResponseProcessorForMsn.class ).in( Scopes.SINGLETON );

        bind( SigninResponseProcessor.class ).annotatedWith( Names.named( "resp.msn" ) ).to( Kraken112SigninResponseProcessorForMsn.class ).in( Scopes.SINGLETON );

    // domain ----------------------

        bind( IMSessionFactory.class )
            .toProvider( FactoryProvider.newFactory( IMSessionFactory.class,
                IMSessionImpl.class ) );

        bind( IMUserSettingFactory.class )
            .toProvider(
                FactoryProvider.newFactory( IMUserSettingFactory.class, IMUserSettingImpl.class ) );

        bind( IMUserSettingFactory2.class )
            .to( IMUserSettingFactory2Impl.class ).in( Scopes.SINGLETON );

        bind( SignInResultConjoiner.class ).to( SignInResultConjoinerImpl.class ).in( Scopes.SINGLETON );

        bind( DomainUtility.class ).to( DomainUtilityImpl.class ).in( Scopes.SINGLETON );

    // presence
        bind( PacketListenerForPresenceFactory.class )
            .toProvider( FactoryProvider.newFactory( PacketListenerForPresenceFactory.class, 
                PacketListenerForPresence.class ) );

    // message
        bind( PacketListenerForMessageFactory.class )
            .toProvider( FactoryProvider.newFactory( PacketListenerForMessageFactory.class, 
                PacketListenerForMessage.class ) );

    // IQ
        bind( PacketListenerForIQFactory.class )
            .toProvider( FactoryProvider.newFactory( PacketListenerForIQFactory.class, 
                PacketListenerForIQ.class ) );

    // Roster
        bind( PacketListenerForRosterFactory.class )
            .toProvider( FactoryProvider.newFactory( PacketListenerForRosterFactory.class, 
                PacketListenerForRosterEntries.class ) );

        bind( RosterListenerFactory.class )
            .toProvider( FactoryProvider.newFactory( RosterListenerFactory.class, RosterListenerImpl.class ) );

    // predicates ------------------

        bind( new TypeLiteral<Predicate<String>>() {} )
            .annotatedWith( Names.named( "userexist" ) )
            .to( new TypeLiteral<UserExistsPredicate>() {} ).in( Scopes.SINGLETON );

        bind( new TypeLiteral<Predicate<String>>() {} )
            .annotatedWith( Names.named( "ymail" ) )
            .to( new TypeLiteral<YahooYmailEmailPredicate>() {} ).in( Scopes.SINGLETON );

        bind( new TypeLiteral<Predicate<String>>() {} )
            .annotatedWith( Names.named( "rocketmail" ) )
            .to( new TypeLiteral<YahooRocketmailEmailPredicate>() {} ).in( Scopes.SINGLETON );

        bind( new TypeLiteral<Predicate<String>>() {} )
            .annotatedWith( Names.named( "yahoo" ) )
            .to( new TypeLiteral<YahooOriginalEmailPredicate>() {} ).in( Scopes.SINGLETON );

        bind( new TypeLiteral<Predicate<String>>() {} )
            .annotatedWith( Names.named( "google" ) )
            .to( new TypeLiteral<GoogleEmailPredicate>() {} ).in( Scopes.SINGLETON );

        bind( new TypeLiteral<Predicate<String>>() {} )
            .annotatedWith( Names.named( "qq" ) )
            .to( new TypeLiteral<QQIdPredicate>() {} ).in( Scopes.SINGLETON );

        bind( new TypeLiteral<Predicate<String>>() {} )
            .annotatedWith( Names.named( "msn" ) )
            .to( new TypeLiteral<MSNEmailPredicate>() {} ).in( Scopes.SINGLETON );

///////////////////////
// application      //
/////////////////////

        bind( AccountService.class ).to( AccountServiceImpl.class ).in( Scopes.SINGLETON );

        bind( IMTypeTransportMapper.class ).to( IMTypeTransportMapperImpl.class ).in( Scopes.SINGLETON );

        bind( JIDMapper.class ).to( JIDMapperImpl.class ).in( Scopes.SINGLETON );

/////////////////////////////////
// Cache specific bindings    //
///////////////////////////////

        bindConstant().annotatedWith( Names.named( "voip.cache.domain" ) ).to( "raverun" );

        bind( net.sf.ehcache.CacheManager.class ).toProvider( DefaultCacheManagerProvider.class ).in( Scopes.SINGLETON );

        bind( CacheClient.class ).annotatedWith( Ehcache.class ).to( EhcacheClientFacade.class ).in( Scopes.SINGLETON );

        bind( CacheKeyGenerator.class ).to( CacheKeyGeneratorImpl.class ).in( Scopes.SINGLETON );

        bind( CacheOfNickNames.class ).to( CacheOfNickNamesImpl.class ).in( Scopes.SINGLETON );

////////////////
// resources //
//////////////

        bind( Resource.class ).annotatedWith( Names.named( "user" ) ).to( UserResource.class ).in( Scopes.SINGLETON );

        bind( Resource.class ).annotatedWith( Names.named( "session") ).to(  SessionResource.class ).in( Scopes.SINGLETON );

        bind( Resource.class ).annotatedWith( Names.named( "transport") ).to(  TransportResource.class ).in( Scopes.SINGLETON );

        bind( Resource.class ).annotatedWith( Names.named( "xmpp") ).to(  XMPPGatewayResource.class ).in( Scopes.SINGLETON );

        bind( Resource.class ).annotatedWith( Names.named( "updates") ).to( UpdatesResource.class ).in( Scopes.SINGLETON );

        bind( Resource.class ).annotatedWith( Names.named( "chat") ).to( ChatResource.class ).in( Scopes.SINGLETON );

        bind( Resource.class ).annotatedWith( Names.named( "buddy" ) ).to(  BuddyResource.class  ).in( Scopes.SINGLETON );

        bind( Resource.class ).annotatedWith( Names.named( "mode" ) ).to(  ModeResource.class  ).in( Scopes.SINGLETON );

        bind( Resource.class ).annotatedWith( Names.named( "cache" ) ).to(  CacheResource.class  ).in( Scopes.SINGLETON );

        bind( ResourceRegistry.class ).to( ResourceRegistryImpl.class ).in( Scopes.SINGLETON );

        bind( ResourceDispatcher.class ).to( ResourceDispatcherImpl.class ).in( Scopes.SINGLETON );

        bind( SessionUtilsFactory.class )
            .toProvider( FactoryProvider.newFactory( SessionUtilsFactory.class,
                SessionUtilsImpl.class ) );

        bind( TransportParser.class ).to( TransportParserImpl.class ).in( Scopes.SINGLETON );

        bind( BuddyEntityParser.class ).to( BuddyEntityParserImpl.class ).in( Scopes.SINGLETON );

        bind( XMPPGatewayParser.class ).to(  XMPPGatewayParserImpl.class ).in( Scopes.SINGLETON );

    // protocols -------------------

        bind( FaultBarrierHandler.class ).to( FaultBarrierHandlerImpl.class ).in( Scopes.SINGLETON );

        bind( ProtocolProcessorDefault.class ).to( ProtocolProcessorDefaultImpl.class  ).in( Scopes.SINGLETON );

        bind( ProtocolProcessor.class ).annotatedWith( Zero.class ).to( ProtocolProcessorZero.class );

        bind( ProtocolProcessor.class ).annotatedWith( One.class ).to( ProtocolProcessorOne.class );

        bind( ProtocolProcessorSelector.class ).to( ProtocolProcessorSelectorImpl.class ).in( Scopes.SINGLETON );

        bind( SupportedIncomingMediaType.class ).to( DefaultSupportedIncomingMediaType.class );

        bind( SupportedOutgoingMediaTypes.class ).to( DefaultSupportedOutgoingMediaTypes.class );

        bind( OutgoingMediaTypeSelector.class ).to( DefaultOutgoingMediaTypeSelector.class );

    // persistence ------------------------

        bind( JDBC2JPAExceptionTranslator.class ).to( JDBC2JPAExceptionTranslatorImpl.class ).in( Scopes.SINGLETON );

        bind( UserDaoFactory.class )
            .toProvider( FactoryProvider.newFactory( UserDaoFactory.class, UserDao.class ) );

        bind( TransportDaoFactory.class )
            .toProvider( FactoryProvider.newFactory( TransportDaoFactory.class, TransportDao.class ) );

        bind( TransportMapper.class ).to( JDBCTransportMapper.class ).in( Scopes.SINGLETON );

        bind( IMUserSettingDaoFactory.class )
            .toProvider(
                FactoryProvider.newFactory( IMUserSettingDaoFactory.class, IMUserSettingDao.class ) );

        bind( IMUserXmppDaoFactory.class )
            .toProvider(
                FactoryProvider.newFactory( IMUserXmppDaoFactory.class, IMUserXmppDao.class ) );

        bind( UserService.class ).to( JDBCUserService.class ).in( Scopes.SINGLETON );

        bind( SettingsService.class ).to( JDBCSettingsService.class ).in( Scopes.SINGLETON );

        bind( IMConnectionFactory.class )
            .toProvider(
                FactoryProvider.newFactory( IMConnectionFactory.class, IMConnectionImpl.class  ) );

        bind( IMSubscriptionRequestDaoFactory.class )
            .toProvider( FactoryProvider.newFactory( IMSubscriptionRequestDaoFactory.class, IMSubscriptionRequestDao.class ) );

        bind( SubscriptionService.class ).to( JDBCSubscriptionService.class ).in( Scopes.SINGLETON );

        bind( IMMessageChatDaoFactory.class )
            .toProvider( FactoryProvider.newFactory( IMMessageChatDaoFactory.class, IMMessageChatDao.class ) );

        bind( IMMessageHeadlineDaoFactory.class )
            .toProvider( FactoryProvider.newFactory( IMMessageHeadlineDaoFactory.class, IMMessageHeadlineDao.class ) );

        bind( MessageService.class ).to( JDBCMessageService.class ).in( Scopes.SINGLETON );

        bind( IMPresenceDaoFactory.class )
            .toProvider( FactoryProvider.newFactory( IMPresenceDaoFactory.class, IMPresenceDao.class ) );

        bind( PresenceService.class ).to( JDBCPresenceService.class ).in( Scopes.SINGLETON );

    // XMPP core -------------------------

        bind( XMPPUtility.class ).to(  XMPPUtilityImpl.class ).in( Scopes.SINGLETON );

        bind( XMPPConnectionIF.class ).toProvider( XMPPConnectionIFProvider.class );

        bind( XMPPAccountService.class ).to( XMPPAccountServiceImpl.class ).in( Scopes.SINGLETON );

    // XMPP operations -------------------

        bind( PresenceUtilityIF.class ).to( PresenceUtilityImpl.class ).in( Scopes.SINGLETON );

/*        
        bind( DiscoTransportOperation.class ).to( DiscoTransportOperationImpl.class ).in( Scopes.SINGLETON );
        bind( RegisterGatewayAccountOperation.class ).to( RegisterGatewayAccountOperationImpl.class ).in( Scopes.SINGLETON );
        bind( DeregisterGatewayAccountOperation.class ).to(
            DeregisterGatewayAccountOperationImpl.class ).in( Scopes.SINGLETON );

        bind( SigninGatewayOperation.class ).to( SigninGatewayOperationImpl.class ).in( Scopes.SINGLETON );
        bind( SignoutGatewayOperation.class ).to( SignoutGatewayOperationImpl.class ).in( Scopes.SINGLETON );
        bind( SendChatMessageOperation.class ).to( SendChatMessageOperationImpl.class ).in( Scopes.SINGLETON );
        bind( AddBuddyOperation.class ).to( AddBuddyOperationImpl.class ).in( Scopes.SINGLETON );
        bind( AcceptBuddyOperation.class ).to( AcceptBuddyOperationImpl.class ).in( Scopes.SINGLETON );
        bind( RejectBuddyOperation.class ).to( RejectBuddyOperationImpl.class ).in( Scopes.SINGLETON );
        bind( SetModeOperation.class ).to( SetModeOperationImpl.class ).in( Scopes.SINGLETON );
*/

//////////////////////////////////////////////////
// New design for XMPP operations 20090919. 
// It is now thread-safe.
//////////////////////////////////////////////////
        bind( MTSigninGatewayOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTSigninGatewayOperationFactory.class, MTSigninGatewayOperationImpl.class ) );

        bind( MTSignoutGatewayOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTSignoutGatewayOperationFactory.class, MTSignoutGatewayOperationImpl.class ) );

        bind( MTAcceptBuddyOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTAcceptBuddyOperationFactory.class, MTAcceptBuddyOperationImpl.class ) );

        bind( MTRegisterAccountOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTRegisterAccountOperationFactory.class, MTRegisterAccountOperationImpl.class ) );

        bind( MTDeregisterAccountOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTDeregisterAccountOperationFactory.class, MTDeregisterAccountOperationImpl.class ) );

        bind( MTDiscoTransportOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTDiscoTransportOperationFactory.class, MTDiscoTransportOperationImpl.class ) );

        bind( MTSendChatMessageOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTSendChatMessageOperationFactory.class, MTSendChatMessageOperationImpl.class ) );

        bind( MTAddBuddyOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTAddBuddyOperationFactory.class, MTAddBuddyOperationImpl.class ) );

        bind( MTRejectBuddyOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTRejectBuddyOperationFactory.class, MTRejectBuddyOperationImpl.class ) );

        bind( MTSetModeOperationFactory.class )
            .toProvider( FactoryProvider.newFactory( MTSetModeOperationFactory.class, MTSetModeOperationImpl.class ) );

//////////////////////////////////////////////////
// prevention of concurrent logins 20090913
/////////////////////////////////////////////////
        bind( IdMutexForLogins.class ).to( IdMutexForLoginsImpl.class ).in( Scopes.SINGLETON );

        bind( CacheOfLoginsDaoFactory.class )
            .toProvider( FactoryProvider.newFactory( CacheOfLoginsDaoFactory.class, CacheOfLoginsDao.class ) );

        bind( CacheOfLoginsService.class ).to( JDBCCacheOfLoginsService.class ).in( Scopes.SINGLETON );

        bind( CacheOfLogins.class ).to( CacheOfLoginsImpl.class ).in( Scopes.SINGLETON );

////////////////////////////////////////////////////
// testing offline notifications 20090913
////////////////////////////////////////////////////

        bind( IMOfflineDaoFactory.class )
            .toProvider( FactoryProvider.newFactory( IMOfflineDaoFactory.class, IMOfflineDao.class ) );

        bind( OfflineService.class ).to( JDBCOfflineService.class ).in( Scopes.SINGLETON );
    }

    /**
     * TODO not working!
     */
    @SuppressWarnings("unused")
    private static <T> TypeLiteral<Predicate<T>> predicate(Class<T> in ) 
    { 
        return new TypeLiteral<Predicate<T>>() {};
    }

    /** @deprecated not sure why we need this!! **/
    public static enum ConfigurationType
    {
        DB, FILE 
    }
}

