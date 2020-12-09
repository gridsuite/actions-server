/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.*;
import com.powsybl.contingency.dsl.ContingencyDslLoader;
import com.powsybl.iidm.network.*;
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
import org.gridsuite.actions.server.utils.NominalVoltageOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;
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

    private <I extends Injection<I>> Stream<Injection<I>> getInjectionContingencyList(Stream<Injection<I>> stream, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return stream
                .filter(injection -> equipmentIDPattern.matcher(injection.getId()).find() || injection.getOptionalName().isPresent() && equipmentNamePattern.matcher(injection.getOptionalName().get()).find())
                .filter(injection -> filtersContingencyList.getNominalVoltage() == -1 || filterByVoltage(injection.getTerminal().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()));
    }

    private List<Contingency> getGeneratorContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return getInjectionContingencyList(network.getGeneratorStream().map(gen -> gen), filtersContingencyList, equipmentIDPattern, equipmentNamePattern)
                .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new GeneratorContingency(injection.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getSVCContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return getInjectionContingencyList(network.getStaticVarCompensatorStream().map(svc -> svc), filtersContingencyList, equipmentIDPattern, equipmentNamePattern)
                .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new StaticVarCompensatorContingency(injection.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getSCContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return getInjectionContingencyList(network.getShuntCompensatorStream().map(sc -> sc), filtersContingencyList, equipmentIDPattern, equipmentNamePattern)
                .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new ShuntCompensatorContingency(injection.getId()))))
                .collect(Collectors.toList());
    }

    private <I extends Branch<I>> List<Contingency> getBranchContingencyList(Stream<Branch<I>> stream, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return stream
                .filter(branch -> equipmentIDPattern.matcher(branch.getId()).find() || branch.getOptionalName().isPresent() && equipmentNamePattern.matcher(branch.getOptionalName().get()).find())
                .filter(branch -> filtersContingencyList.getNominalVoltage() == -1 || filterByVoltage(branch.getTerminal1().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator())
                        || filterByVoltage(branch.getTerminal2().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(branch -> new Contingency(branch.getId(), Collections.singletonList(new BranchContingency(branch.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getLineContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return  getBranchContingencyList(network.getLineStream().map(line -> line), filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
    }

    private List<Contingency> get2WTransformerContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return  getBranchContingencyList(network.getTwoWindingsTransformerStream().map(twt -> twt), filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
    }

    private List<Contingency> getHvdcContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return network.getHvdcLineStream()
                .filter(hvdcLine -> equipmentIDPattern.matcher(hvdcLine.getId()).find() || hvdcLine.getOptionalName().isPresent() && equipmentNamePattern.matcher(hvdcLine.getOptionalName().get()).find())
                .filter(hvdcLine -> filtersContingencyList.getNominalVoltage() == -1 || filterByVoltage(hvdcLine.getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(hvdcLine -> new Contingency(hvdcLine.getId(), Collections.singletonList(new HvdcLineContingency(hvdcLine.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getBusbarSectionContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return getInjectionContingencyList(network.getBusbarSectionStream().map(bbs -> bbs), filtersContingencyList, equipmentIDPattern, equipmentNamePattern)
                .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new BusbarSectionContingency(injection.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getDanglingLineContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return getInjectionContingencyList(network.getDanglingLineStream().map(dl -> dl), filtersContingencyList, equipmentIDPattern, equipmentNamePattern)
                .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new DanglingLineContingency(injection.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getContingencies(FiltersContingencyList filtersContingencyList, Network network) {
        List<Contingency> contingencies;
        Pattern equipmentNamePattern = Pattern.compile(filtersContingencyList.getEquipmentName(), Pattern.CASE_INSENSITIVE);
        Pattern equipmentIDPattern = Pattern.compile(filtersContingencyList.getEquipmentID(), Pattern.CASE_INSENSITIVE);
        switch (EquipmentType.valueOf(filtersContingencyList.getEquipmentType())) {
            case  GENERATOR:
                contingencies = getGeneratorContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            case STATIC_VAR_COMPENSATOR:
                contingencies = getSVCContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            case SHUNT_COMPENSATOR:
                contingencies = getSCContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            case HVDC_LINE:
                contingencies = getHvdcContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            case BUSBAR_SECTION:
                contingencies = getBusbarSectionContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            case DANGLING_LINE:
                contingencies = getDanglingLineContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            case LINE:
                contingencies = getLineContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            case TWO_WINDINGS_TRANSFORMER:
                contingencies = get2WTransformerContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            default:
                throw new PowsyblException("Unknown equipment type");
        }
        return contingencies;
    }

    private boolean filterByVoltage(double equipmentNominalVoltage, double nominalVoltage, String nominalVoltageOperator) {
        switch (NominalVoltageOperator.fromOperator(nominalVoltageOperator)) {
            case EQUAL:
                return equipmentNominalVoltage == nominalVoltage;
            case LESS_THAN:
                return equipmentNominalVoltage < nominalVoltage;
            case LESS_THAN_OR_EQUAL:
                return equipmentNominalVoltage <= nominalVoltage;
            case MORE_THAN:
                return equipmentNominalVoltage > nominalVoltage;
            case MORE_THAN_OR_EQUAL:
                return equipmentNominalVoltage >= nominalVoltage;
            default:
                throw new PowsyblException("Unknown nominal voltage operator");
        }
    }

    Optional<List<Contingency>> exportContingencyList(String name, UUID networkUuid) {
        Objects.requireNonNull(name);

        return getScriptContingencyList(name).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid))
                .or(() -> getFiltersContingencyList(name).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid)));
    }

    void createScriptContingencyList(String name, String script) {
        Objects.requireNonNull(name);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(name));
        }
        scriptContingencyListRepository.insert(new ScriptContingencyListEntity(name, script));
    }

    public void createFilterContingencyList(String name, FiltersContingencyListAttributes filtersContingencyListAttributes) {
        Objects.requireNonNull(name);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(name));
        }
        filtersContingencyListRepository.insert(new FiltersContingencyListEntity(name, filtersContingencyListAttributes));
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

        script.ifPresentOrElse(oldContingencyListEntity -> {
            scriptContingencyListRepository.deleteByName(name);
            createScriptContingencyList(newName, oldContingencyListEntity.getScript());
        }, () -> filters.map(oldFiltersContingencyListEntity -> {
            filtersContingencyListRepository.deleteByName(name);
            createFilterContingencyList(newName, new FiltersContingencyListAttributes(oldFiltersContingencyListEntity.getEquipmentId(), oldFiltersContingencyListEntity.getEquipmentName(),
                    oldFiltersContingencyListEntity.getEquipmentType(), oldFiltersContingencyListEntity.getNominalVoltage(), oldFiltersContingencyListEntity.getNominalVoltageOperator()));
            return oldFiltersContingencyListEntity;
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + name + " not found")));
    }
}
