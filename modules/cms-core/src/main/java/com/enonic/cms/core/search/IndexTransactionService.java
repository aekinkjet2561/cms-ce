package com.enonic.cms.core.search;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.category.CategoryKey;

public interface IndexTransactionService
{

    void startTransaction();

    void commit();

    boolean isActive();

    void updateContent( ContentKey contentKey );

    void updateContent( ContentEntity contentEntity );

    void deleteContent( ContentKey contentKey );

    void updateCategory( CategoryKey categoryKey );
}
