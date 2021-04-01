/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.repositories;

import org.gridsuite.actions.server.entities.FiltersContingencyListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Repository
public interface FiltersContingencyListRepository extends JpaRepository<FiltersContingencyListEntity, String> {

    @Query("SELECT DISTINCT entity FROM FiltersContingencyListEntity entity LEFT JOIN FETCH entity.countries")
    List<FiltersContingencyListEntity> findAllWithCountries();

    Optional<FiltersContingencyListEntity> findByName(String name);

    @Transactional
    void deleteByName(String name);

    boolean existsByName(String name);
}
