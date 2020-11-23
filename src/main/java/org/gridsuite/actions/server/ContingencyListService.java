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
import org.gridsuite.actions.server.dto.ContingencyList;
import org.gridsuite.actions.server.dto.FilterContingencyList;
import org.gridsuite.actions.server.dto.FilterContingencyListAttributes;
import org.gridsuite.actions.server.entities.FilterContingencyListEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.dto.ScriptContingencyList;
import org.gridsuite.actions.server.repositories.FilterContingencyListRepository;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
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

    private ScriptContingencyListRepository scriptContingencyListRepository;
    private FilterContingencyListRepository filterContingencyListRepository;

    private NetworkStoreService networkStoreService;

    public ContingencyListService(ScriptContingencyListRepository scriptContingencyListRepository, FilterContingencyListRepository filterContingencyListRepository,
                                  NetworkStoreService networkStoreService) {
        this.scriptContingencyListRepository = scriptContingencyListRepository;
        this.filterContingencyListRepository = filterContingencyListRepository;
        this.networkStoreService = networkStoreService;
    }

    private static ContingencyList fromScriptContingencyListEntity(ScriptContingencyListEntity entity) {
        return new ScriptContingencyList(entity.getName(), entity.getScript() != null ? entity.getScript() : "");
    }

    private static ContingencyList fromFilterContingencyListEntity(FilterContingencyListEntity entity) {
        return new FilterContingencyList(entity.getName(), entity.getEquipmentId(), entity.getEquipmentName(),
                entity.getEquipmentType(), entity.getNominalVoltage(), entity.getNominalVoltageOperator());
    }

    private static String sanitizeParam(String param) {
        return param != null ? param.replaceAll("[\n|\r|\t]", "_") : null;
    }

    List<ContingencyList> getScriptContingencyLists() {
        return scriptContingencyListRepository.findAll().stream().map(ContingencyListService::fromScriptContingencyListEntity).collect(Collectors.toList());
    }

    List<ContingencyList> getFilterContingencyLists() {
        return filterContingencyListRepository.findAll().stream().map(ContingencyListService::fromFilterContingencyListEntity).collect(Collectors.toList());
    }

    Optional<ContingencyList> getScriptContingencyList(String name) {
        Objects.requireNonNull(name);
        return scriptContingencyListRepository.findByName(name).map(ContingencyListService::fromScriptContingencyListEntity);
    }

    Optional<ContingencyList> getFilterContingencyList(String name) {
        Objects.requireNonNull(name);
        return filterContingencyListRepository.findByName(name).map(ContingencyListService::fromFilterContingencyListEntity);
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
        return getScriptContingencyList(name)
                .map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid));
    }

    void createScriptContingencyList(String name, String script) {
        Objects.requireNonNull(name);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(name));
        }
        scriptContingencyListRepository.insert(new ScriptContingencyListEntity(name, script));
    }

    public void createFilterContingencyList(String name, FilterContingencyListAttributes filterContingencyListAttributes) {
        Objects.requireNonNull(name);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(name));
        }
        filterContingencyListRepository.insert(new FilterContingencyListEntity(name, filterContingencyListAttributes));
    }

    void deleteContingencyList(String name) {
        Objects.requireNonNull(name);
        if (scriptContingencyListRepository.existsByName(name)) {
            scriptContingencyListRepository.deleteByName(name);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + name + " not found");
        }
    }

    void renameContingencyList(String name, String newName) {
        Objects.requireNonNull(name);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rename script contingency list '{}' to '{}'", sanitizeParam(name), sanitizeParam(newName));
        }
        Optional<ScriptContingencyListEntity> optionalContingencyListEntity = scriptContingencyListRepository.findByName(name);
        if (optionalContingencyListEntity.isPresent()) {
            scriptContingencyListRepository.deleteByName(name);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + name + " not found");
        }
        ScriptContingencyListEntity oldContingencyListEntity = optionalContingencyListEntity.get();
        createScriptContingencyList(newName, oldContingencyListEntity.getScript());
    }
}
