/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.repositories;

import org.gridsuite.actions.server.entities.IdBasedContingencyListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@Repository
public interface IdBasedContingencyListRepository extends JpaRepository<IdBasedContingencyListEntity, UUID> {
    Integer deleteIdBasedContingencyListEntityById(UUID id);
}
