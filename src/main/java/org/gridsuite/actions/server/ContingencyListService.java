/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import org.gridsuite.actions.server.repositories.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Service
public class ContingencyListService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContingencyListService.class);

    private ScriptContingencyListRepository repository;

    public ContingencyListService(ScriptContingencyListRepository repository) {
        this.repository = repository;
    }

    private static ContingencyList fromEntity(ScriptContingencyListEntity entity) {
        return new ScriptContingencyList(entity.getName(), entity.getScript());
    }

    List<ContingencyList> getContingencyLists() {
        return repository.findAll().stream().map(ContingencyListService::fromEntity).collect(Collectors.toList());
    }

    Optional<ContingencyList> getContingencyList(String name) {
        Objects.requireNonNull(name);
        return repository.findByName(name).map(ContingencyListService::fromEntity);
    }

    void createScriptContingencyList(String name, String script) {
        Objects.requireNonNull(name);
        LOGGER.info("Create script contingency list '{}'", name);
        repository.insert(new ScriptContingencyListEntity(name, script));
    }
}
