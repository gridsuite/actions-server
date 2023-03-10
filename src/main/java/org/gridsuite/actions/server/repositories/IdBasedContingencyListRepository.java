package org.gridsuite.actions.server.repositories;

import org.gridsuite.actions.server.entities.IdBasedContingencyListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IdBasedContingencyListRepository extends JpaRepository<IdBasedContingencyListEntity, UUID> {
    Integer deleteIdBasedContingencyListEntityById(UUID id);
}
