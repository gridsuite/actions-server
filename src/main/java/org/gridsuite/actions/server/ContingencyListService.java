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
import org.gridsuite.actions.server.dto.*;
import org.gridsuite.actions.server.entities.FiltersContingencyListEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.FiltersContingencyListRepository;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
public class ContingencyListService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContingencyListService.class);

    private ScriptContingencyListRepository scriptContingencyListRepository;
    private FiltersContingencyListRepository filtersContingencyListRepository;

    private NetworkStoreService networkStoreService;

    public ContingencyListService(ScriptContingencyListRepository scriptContingencyListRepository, FiltersContingencyListRepository filtersContingencyListRepository,
                                  NetworkStoreService networkStoreService) {
        this.scriptContingencyListRepository = scriptContingencyListRepository;
        this.filtersContingencyListRepository = filtersContingencyListRepository;
        this.networkStoreService = networkStoreService;
    }

    private static ScriptContingencyList fromScriptContingencyListEntity(ScriptContingencyListEntity entity) {
        return new ScriptContingencyList(entity.getName(), entity.getScript() != null ? entity.getScript() : "");
    }

    private static FiltersContingencyList fromFilterContingencyListEntity(FiltersContingencyListEntity entity) {
        return new FiltersContingencyList(entity.getName(), entity.getEquipmentId(), entity.getEquipmentName(),
                entity.getEquipmentType(), entity.getNominalVoltage(), entity.getNominalVoltageOperator());
    }

    private static String sanitizeParam(String param) {
        return param != null ? param.replaceAll("[\n|\r|\t]", "_") : null;
    }

    List<ScriptContingencyList> getScriptContingencyLists() {
        return scriptContingencyListRepository.findAll().stream().map(ContingencyListService::fromScriptContingencyListEntity).collect(Collectors.toList());
    }

    List<ContingencyListAttributes> getContingencyLists() {
        return Stream.concat(
                scriptContingencyListRepository.findAll().stream().map(scriptContingencyListEntity ->
                        new ContingencyListAttributes(scriptContingencyListEntity.getName(), ContingencyListType.SCRIPT)),
                filtersContingencyListRepository.findAll().stream().map(filtersContingencyListEntity ->
                        new ContingencyListAttributes(filtersContingencyListEntity.getName(), ContingencyListType.FILTERS))
        ).collect(Collectors.toList());
    }

    List<FiltersContingencyList> getFilterContingencyLists() {
        return filtersContingencyListRepository.findAll().stream().map(ContingencyListService::fromFilterContingencyListEntity).collect(Collectors.toList());
    }

    Optional<ScriptContingencyList> getScriptContingencyList(String name) {
        Objects.requireNonNull(name);
        return scriptContingencyListRepository.findByName(name).map(ContingencyListService::fromScriptContingencyListEntity);
    }

    Optional<FiltersContingencyList> getFiltersContingencyList(String name) {
        Objects.requireNonNull(name);
        return filtersContingencyListRepository.findByName(name).map(ContingencyListService::fromFilterContingencyListEntity);
    }

    private List<Contingency> toPowSyBlContingencyList(ContingencyList contingencyList, UUID networkUuid) {
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

        if (contingencyList instanceof ScriptContingencyList) {
            String script = ((ScriptContingencyList) contingencyList).getScript();
            return new ContingencyDslLoader(script).load(network);
        } else if (contingencyList instanceof FiltersContingencyList) {
            FiltersContingencyList filtersContingencyList = (FiltersContingencyList) contingencyList;
            return getContingencies(filtersContingencyList, network);
        } else {
            throw new PowsyblException("Contingency list implementation not yet supported: " + contingencyList.getClass().getSimpleName());
        }
    }

    private List<Contingency> getContingencies(FiltersContingencyList filtersContingencyList, Network network) {
        List<Contingency> contingencies = new ArrayList<>();
        switch (EquipmentType.valueOf(filtersContingencyList.getEquipmentType())) {
            case  GENERATOR:
                network.getGenerators();
                // TO DO
                break;
            default:
                throw new PowsyblException("Unknown equipment type");
        }
        return contingencies;
    }

    Optional<List<Contingency>> exportContingencyList(String name, UUID networkUuid) {
        Objects.requireNonNull(name);

        if (getScriptContingencyList(name).isPresent()) {
            return getScriptContingencyList(name)
                    .map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid));
        } else {
            return getFiltersContingencyList(name)
                    .map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid));
        }

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
        filtersContingencyListRepository.insert(new FiltersContingencyListEntity(name, filterContingencyListAttributes));
    }

    void deleteContingencyList(String name) {
        Objects.requireNonNull(name);
        if (scriptContingencyListRepository.existsByName(name)) {
            scriptContingencyListRepository.deleteByName(name);
        } else if (filtersContingencyListRepository.existsByName(name)) {
            filtersContingencyListRepository.deleteByName(name);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + name + " not found");
        }
    }

    void renameContingencyList(String name, String newName) {
        Objects.requireNonNull(name);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("rename script contingency list '{}' to '{}'", sanitizeParam(name), sanitizeParam(newName));
        }
        Optional<ScriptContingencyListEntity> script = scriptContingencyListRepository.findByName(name);
        Optional<FiltersContingencyListEntity> filters = filtersContingencyListRepository.findByName(name);
        if (script.isPresent()) {
            scriptContingencyListRepository.deleteByName(name);
            ScriptContingencyListEntity oldContingencyListEntity = script.get();
            createScriptContingencyList(newName, oldContingencyListEntity.getScript());
        } else if (filters.isPresent()) {
            filtersContingencyListRepository.deleteByName(name);
            FiltersContingencyListEntity oldFiltersContingencyListEntity = filters.get();
            createFilterContingencyList(newName, new FilterContingencyListAttributes(oldFiltersContingencyListEntity.getEquipmentId(), oldFiltersContingencyListEntity.getEquipmentName(),
                    oldFiltersContingencyListEntity.getEquipmentType(), oldFiltersContingencyListEntity.getNominalVoltage(), oldFiltersContingencyListEntity.getNominalVoltageOperator()));

        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + name + " not found");
        }

    }
}
