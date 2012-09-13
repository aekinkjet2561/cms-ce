package com.enonic.cms.itest.content;

import java.util.List;

import org.jdom.Document;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentService;
import com.enonic.cms.core.content.ContentStatus;
import com.enonic.cms.core.content.access.ContentAccessEntity;
import com.enonic.cms.core.content.category.CategoryAccessControl;
import com.enonic.cms.core.content.category.CategoryAccessType;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.category.CategoryService;
import com.enonic.cms.core.content.category.ModifyCategoryACLCommand;
import com.enonic.cms.core.content.category.StoreNewCategoryCommand;
import com.enonic.cms.core.content.category.SynchronizeCategoryACLCommand;
import com.enonic.cms.core.content.command.CreateContentCommand;
import com.enonic.cms.core.content.contentdata.ContentData;
import com.enonic.cms.core.content.contentdata.custom.CustomContentData;
import com.enonic.cms.core.content.contentdata.custom.stringbased.TextDataEntry;
import com.enonic.cms.core.content.contenttype.ContentHandlerName;
import com.enonic.cms.core.content.contenttype.ContentTypeConfig;
import com.enonic.cms.core.content.contenttype.ContentTypeConfigBuilder;
import com.enonic.cms.core.content.index.ContentIndexQuery;
import com.enonic.cms.core.content.query.OpenContentQuery;
import com.enonic.cms.core.content.resultset.ContentResultSet;
import com.enonic.cms.core.security.user.UserType;
import com.enonic.cms.itest.search.ContentIndexServiceTestHibernatedBase;
import com.enonic.cms.store.dao.CategoryDao;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.GroupDao;
import com.enonic.cms.store.dao.UserDao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CategoryServiceImpl_indexUpdateTest
    extends ContentIndexServiceTestHibernatedBase
{

    private Document personCtyConfigAsDocument;

    @Autowired
    private ContentService contentService;

    @Autowired
    protected CategoryService categoryService;

    @Autowired
    protected CategoryDao categoryDao;

    @Autowired
    protected UserDao userDao;

    @Autowired
    protected GroupDao groupDao;

    @Autowired
    private ContentDao contentDao;

    public static final String CONTENT_TYPE_NAME = "aContentType";

    @Before
    public void setUp()
    {
        SynchronizeCategoryACLCommand.executeInOneTransaction = true;
        ModifyCategoryACLCommand.executeInOneTransaction = true;

        factory = fixture.getFactory();

        // setup needed common data for each test
        fixture.initSystemData();

        // setting up a simple content type config
        ContentTypeConfigBuilder contentTypeConfigBuilder = new ContentTypeConfigBuilder( "Person", "name" );
        contentTypeConfigBuilder.startBlock( "Person" );
        contentTypeConfigBuilder.addInput( "name", "text", "contentdata/name", "Name", true );
        contentTypeConfigBuilder.endBlock();
        personCtyConfigAsDocument = XMLDocumentFactory.create( contentTypeConfigBuilder.toString() ).getAsJDOMDocument();

        fixture.save( factory.createContentHandler( "Custom content", ContentHandlerName.CUSTOM.getHandlerClassShortName() ) );
        fixture.save( factory.createContentType( CONTENT_TYPE_NAME, ContentHandlerName.CUSTOM.getHandlerClassShortName(),
                                                 personCtyConfigAsDocument ) );
    }

    @Test
    public void index_updated_for_content_in_category_when_acl_changed()
    {
        // setup
        final String categoryName = "Category";
        final String adminUser = "admin";
        final String aNormalUserUid = "aUser";

        createUser( aNormalUserUid, UserType.NORMAL );

        final CategoryKey categoryKey = storeCategory( CONTENT_TYPE_NAME, categoryName );
        assertNotNull( categoryDao.findByKey( categoryKey ) );

        ContentAccessEntity normalUserAccess = createContentAccess( aNormalUserUid, true, false );
        final ContentKey contentKey = createContent( CONTENT_TYPE_NAME, categoryName, adminUser, Lists.newArrayList( normalUserAccess ) );
        assertNotNull( contentDao.findByKey( contentKey ) );

        fixture.flushAndClearHibernateSesssion();
        fixture.flushIndexTransaction();

        // exercise

        // Verify that user does not have access
        OpenContentQuery queryAssertingCategoryBrowse = createQueryAssertingCategoryBrowse( aNormalUserUid, contentKey );
        ContentResultSet contentResultSet = contentService.queryContent( queryAssertingCategoryBrowse );
        assertEquals( 0, contentResultSet.getKeys().size() );

        // Add admin browse for user to category
        CategoryAccessControl acl = new CategoryAccessControl();
        acl.setGroupKey( fixture.findGroupByName( aNormalUserUid ).getGroupKey() );
        acl.setAdminBrowseAccess( true );
        addAclForCategory( categoryName, adminUser, acl );

        fixture.flushAndClearHibernateSesssion();
        fixture.flushIndexTransaction();

        // Verify that user now get content from query
        contentResultSet = contentService.queryContent( queryAssertingCategoryBrowse );
        assertEquals( 1, contentResultSet.getKeys().size() );

        /*
        fixture.getIndexTransactionService().startTransaction();
        fixture.getIndexTransactionService().registerUpdate( contentKey, true );

        fixture.flushIndexTransaction();

        query = new OpenContentQuery();
        query.setUser( fixture.findUserByName( aNormalUserUid ) );
        query.setContentKeyFilter( Lists.newArrayList( contentKey ) );
        contentResultSet = contentService.queryContent( query );
        assertEquals( 1, contentResultSet.getKeys().size() );
        */

    }

    private OpenContentQuery createQueryAssertingCategoryBrowse( final String aNormalUserUid, final ContentKey contentKey )
    {
        OpenContentQuery query = new OpenContentQuery();
        query.setUser( fixture.findUserByName( aNormalUserUid ) );
        query.setContentKeyFilter( Lists.newArrayList( contentKey ) );
        query.setCategoryAccessTypeFilter( Lists.newArrayList( CategoryAccessType.ADMIN_BROWSE, CategoryAccessType.READ ),
                                           ContentIndexQuery.CategoryAccessTypeFilterPolicy.AND );
        return query;
    }

    private void addAclForCategory( final String categoryName, final String updaterUid, final CategoryAccessControl acl )
    {
        ModifyCategoryACLCommand modifyCategoryACLCommand = new ModifyCategoryACLCommand();
        modifyCategoryACLCommand.addToBeAdded( acl );
        modifyCategoryACLCommand.includeContent();
        modifyCategoryACLCommand.setUpdater( fixture.findUserByName( updaterUid ).getKey() );
        modifyCategoryACLCommand.addCategory( fixture.findCategoryByName( categoryName ).getKey() );

        categoryService.modifyCategoryACL_withoutRequiresNewPropagation_for_test_only( modifyCategoryACLCommand );
    }

    private void createUser( final String aNormalUserUid, final UserType userType )
    {
        fixture.createAndStoreUserAndUserGroup( aNormalUserUid, aNormalUserUid + "fullname", userType, "testuserstore" );
    }

    private ContentKey createContent( final String contentTypeName, final String categoryName, final String creatorUid,
                                      List<ContentAccessEntity> contentAccesses )
    {
        CustomContentData contentData = new CustomContentData( fixture.findContentTypeByName( contentTypeName ).getContentTypeConfig() );
        contentData.add( new TextDataEntry( contentData.getInputConfig( "name" ), "person" ) );
        CreateContentCommand createContentCommand = createCreateContentCommand( "aContent", categoryName, contentData, creatorUid );
        createContentCommand.addContentAccessRights( contentAccesses, null );
        return contentService.createContent( createContentCommand );
    }

    private CategoryKey storeCategory( final String contentTypeName, final String categoryName )
    {
        StoreNewCategoryCommand storeNewCategoryCommand = createStoreNewCategoryCommand( categoryName, contentTypeName, null );
        return categoryService.storeNewCategory( storeNewCategoryCommand );
    }

    private void createContentType( final String contentTypeName )
    {
        fixture.save(
            factory.createContentType( contentTypeName, ContentHandlerName.CUSTOM.getHandlerClassShortName(), personCtyConfigAsDocument ) );
    }

    protected CreateContentCommand createCreateContentCommand( String categoryName, String creatorUid, ContentStatus contentStatus )
    {
        CreateContentCommand createContentCommand = new CreateContentCommand();
        createContentCommand.setCategory( fixture.findCategoryByName( categoryName ) );
        createContentCommand.setCreator( fixture.findUserByName( creatorUid ).getKey() );
        createContentCommand.setLanguage( fixture.findLanguageByCode( "en" ) );
        createContentCommand.setStatus( contentStatus );
        createContentCommand.setPriority( 0 );
        createContentCommand.setContentName( "name_" + categoryName + "_" + contentStatus );

        ContentTypeConfig contentTypeConfig = fixture.findContentTypeByName( "MyContentType" ).getContentTypeConfig();
        CustomContentData contentData = new CustomContentData( contentTypeConfig );
        contentData.add( new TextDataEntry( contentTypeConfig.getInputConfig( "name" ), "Initial" ) );
        createContentCommand.setContentData( contentData );
        return createContentCommand;
    }

    private CreateContentCommand createCreateContentCommand( String contentName, String categoryName, ContentData contentData,
                                                             final String creatorUid )
    {
        CreateContentCommand command = new CreateContentCommand();
        command.setCreator( fixture.findUserByName( creatorUid ).getKey() );
        command.setStatus( ContentStatus.APPROVED );
        command.setContentName( contentName );
        command.setCategory( fixture.findCategoryByName( categoryName ).getKey() );
        command.setContentData( contentData );
        command.setLanguage( fixture.findLanguageByCode( "en" ).getKey() );
        command.setPriority( 0 );
        return command;
    }

    private StoreNewCategoryCommand createStoreNewCategoryCommand( String name, String contentTypeName, String parentCategoryName )
    {
        StoreNewCategoryCommand command = new StoreNewCategoryCommand();
        command.setCreator( fixture.findUserByName( "admin" ).getKey() );
        command.setParentCategory( parentCategoryName != null ? fixture.findCategoryByName( parentCategoryName ).getKey() : null );
        command.setName( name );
        command.setDescription( "A " + name + "." );
        command.setContentType( fixture.findContentTypeByName( contentTypeName ).getContentTypeKey() );
        command.setLanguage( fixture.findLanguageByCode( "en" ).getKey() );
        command.setAutoApprove( true );
        return command;
    }

    private void addCategoryAC( String userName, String accesses, StoreNewCategoryCommand command )
    {
        command.addAccessRight( factory.createCategoryAccessControl( fixture.findUserByName( userName ).getUserGroup(), accesses ) );
    }

    private void addCategoryAC( String userName, String accesses, List<CategoryAccessControl> list )
    {
        list.add( factory.createCategoryAccessControl( fixture.findUserByName( userName ).getUserGroup(), accesses ) );
    }
}