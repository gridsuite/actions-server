/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.dsl.ContingencyDslLoader;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
public class ContingencyListService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContingencyListService.class);

    private ScriptContingencyListRepository repository;

    private NetworkStoreService networkStoreService;

    public ContingencyListService(ScriptContingencyListRepository repository, NetworkStoreService networkStoreService) {
        this.repository = repository;
        this.networkStoreService = networkStoreService;
    }

    private static ContingencyList fromEntity(ScriptContingencyListEntity entity) {
        return new ScriptContingencyList(entity.getName(), entity.getScript() != null ? entity.getScript() : "");
    }

    private static String sanitizeParam(String param) {
        return param != null ? param.replaceAll("[\n|\r|\t]", "_") : null;
    }

    List<ContingencyList> getContingencyLists() {
        return repository.findAll().stream().map(ContingencyListService::fromEntity).collect(Collectors.toList());
    }

    Optional<ContingencyList> getContingencyList(String name) {
        Objects.requireNonNull(name);
        return repository.findByName(name).map(ContingencyListService::fromEntity);
    }

    private List<Contingency> toPowSyBlContingencyList(ContingencyList contingencyList, UUID networkUuid) {
        if (contingencyList instanceof ScriptContingencyList) {
            Network network;
            if (networkUuid == null) {
                // use an empty network, script might not have need to network
                network = new NetworkFactoryImpl().createNetwork("empty", "empty");
            } else {
                network = networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION);
                if (network == null) {
                    throw new PowsyblException("Network '" + networkUuid + "' not found");
                }
            }
            String script = ((ScriptContingencyList) contingencyList).getScript();
            return new ContingencyDslLoader(script).load(network);
        } else {
            throw new PowsyblException("Contingency list implementation not yet supported: " + contingencyList.getClass().getSimpleName());
        }
    }

    Optional<List<Contingency>> exportContingencyList(String name, UUID networkUuid) {
        Objects.requireNonNull(name);
        return getContingencyList(name)
                .map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid));
    }

    void createScriptContingencyList(String name, String script) {
        Objects.requireNonNull(name);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(name));
        }
        repository.insert(new ScriptContingencyListEntity(name, script));
    }

    void deleteContingencyList(String name) {
        Objects.requireNonNull(name);
        if (repository.existsByName(name)) {
            repository.deleteByName(name);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + name + " not found");
        }
    }

    void renameContingencyList(String name, String newName) {
        Objects.requireNonNull(name);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rename script contingency list '{}' to '{}'", sanitizeParam(name), sanitizeParam(newName));
        }
        Optional<ScriptContingencyListEntity> optionalContingencyListEntity = repository.findByName(name);
        if (optionalContingencyListEntity.isPresent()) {
            repository.deleteByName(name);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + name + " not found");
        }
        ScriptContingencyListEntity oldContingencyListEntity = optionalContingencyListEntity.get();
        createScriptContingencyList(newName, oldContingencyListEntity.getScript());
    }
}
