/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.repositories;

import org.gridsuite.actions.server.entities.GuiContingencyListEntity;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */
@Repository
public interface GuiContingencyListRepository extends CassandraRepository<GuiContingencyListEntity, String> {

    Optional<GuiContingencyListEntity> findByName(String name);

    void deleteByName(String name);

    boolean existsByName(String name);
}
