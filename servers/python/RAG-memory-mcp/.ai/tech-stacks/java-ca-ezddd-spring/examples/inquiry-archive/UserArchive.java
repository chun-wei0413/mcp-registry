package tw.teddysoft.aiscrum.scrumteam.usecase.port.out.archive;

import tw.teddysoft.aiscrum.scrumteam.usecase.port.out.UserData;
import tw.teddysoft.ezddd.cqrs.usecase.query.Archive;

/**
 * Archive interface for managing User data in the Query Model.
 * Users are from upstream Account BC, stored locally for reference.
 * This follows the Archive pattern for Query Model CRUD operations.
 */
public interface UserArchive extends Archive<UserData, String> {
    // findById, save, delete methods are inherited from Archive interface
    // No additional methods needed - Archive pattern focuses on basic CRUD
}