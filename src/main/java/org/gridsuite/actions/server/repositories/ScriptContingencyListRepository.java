/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.repositories;

import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Repository
public interface ScriptContingencyListRepository extends JpaRepository<ScriptContingencyListEntity, UUID> {

    List<ScriptContingencyListEntity> findByUserIdOrIsPrivate(@Param("userId") String userId, @Param("isPrivate") boolean isPrivate);

    @Query(value = "SELECT * FROM script_contingency_list s WHERE id in ?1 and (isPrivate='false' or userId=?2)", nativeQuery = true)
    List<ScriptContingencyListEntity> findAllByUuids(List<UUID> uuids, String userId);

    @Query(value = "SELECT * FROM script_contingency_list s WHERE id=?1 and (isPrivate=?3 or userId=?2)", nativeQuery = true)
    Optional<ScriptContingencyListEntity> findByIdAndUserIdOrIsPrivate(@Param("id") UUID id, @Param("userId") String userId, @Param("isPrivate") boolean isPrivate);
}
