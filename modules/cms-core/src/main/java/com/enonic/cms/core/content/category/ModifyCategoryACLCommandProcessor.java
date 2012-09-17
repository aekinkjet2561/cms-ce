package com.enonic.cms.core.content.category;


import java.util.SortedMap;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.search.IndexTransactionService;
import com.enonic.cms.core.security.group.GroupEntity;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.GroupDao;

class ModifyCategoryACLCommandProcessor
{
    private UpdateCategoryAccessChecker updateCategoryAccessChecker;

    private GroupDao groupDao;

    private final IndexTransactionService indexTransactionService;

    private SortedMap<CategoryKey, CategoryEntity> categoriesToUpdate;

    private final ContentDao contentDao;

    ModifyCategoryACLCommandProcessor( final GroupDao groupDao, final UpdateCategoryAccessChecker updateCategoryAccessChecker,
                                       final IndexTransactionService indexTransactionService, final ContentDao contentDao )
    {
        this.groupDao = groupDao;
        this.updateCategoryAccessChecker = updateCategoryAccessChecker;
        this.indexTransactionService = indexTransactionService;
        this.contentDao = contentDao;
    }

    void setCategoriesToUpdate( SortedMap<CategoryKey, CategoryEntity> categoriesToUpdate )
    {
        this.categoriesToUpdate = categoriesToUpdate;
    }

    void process( final ModifyCategoryACLCommand command )
    {
        checkUpdateCategoriesAccess();

        for ( CategoryEntity category : categoriesToUpdate.values() )
        {
            category.removeAcessRights( command.getToBeRemoved() );
            processThoseToBeAddedOrModified( command.getToBeAdded(), category );
            processThoseToBeAddedOrModified( command.getToBeModified(), category );

            indexTransactionService.registerUpdate( contentDao.findContentKeysByCategory( category.getKey() ), true );
        }
    }

    private void processThoseToBeAddedOrModified( final Iterable<CategoryAccessControl> categoryAccessControls,
                                                  final CategoryEntity category )
    {
        // add (and modify if already existing)
        for ( CategoryAccessControl categoryAccessControl : categoryAccessControls )
        {
            GroupEntity group = groupDao.findByKey( categoryAccessControl.getGroupKey() );
            if ( !category.hasAccessForGroup( categoryAccessControl.getGroupKey() ) )
            {
                category.addAccessRight( CategoryAccessEntity.create( category.getKey(), group, categoryAccessControl ) );
            }
            else
            {
                CategoryAccessEntity access = category.getCategoryAccess( categoryAccessControl.getGroupKey() );
                access.setAccess( categoryAccessControl );
            }
        }
    }

    private void checkUpdateCategoriesAccess()
        throws CreateCategoryAccessException
    {
        for ( CategoryEntity categoryToUpdate : categoriesToUpdate.values() )
        {
            updateCategoryAccessChecker.checkAccessToUpdateCategory( categoryToUpdate );
        }
    }
}
