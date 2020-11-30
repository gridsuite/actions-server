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

    private List<Contingency> getGeneratorContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return network.getGeneratorStream()
                .filter(gen -> filtersContingencyList.getEquipmentName().equals("*") || gen.getOptionalName().isPresent() && equipmentNamePattern.matcher(gen.getOptionalName().get()).find())
                .filter(gen -> filtersContingencyList.getEquipmentID().equals("*") || equipmentIDPattern.matcher(gen.getId()).find())
                .filter(gen -> filtersContingencyList.getNominalVoltage().equals("*") || filterByVoltage(gen.getTargetV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(gen -> new Contingency(gen.getId(), Collections.singletonList(new GeneratorContingency(gen.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getSVCContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return network.getStaticVarCompensatorStream()
                .filter(svc -> filtersContingencyList.getEquipmentName().equals("*") || svc.getOptionalName().isPresent() && equipmentNamePattern.matcher(svc.getOptionalName().get()).find())
                .filter(svc ->  filtersContingencyList.getEquipmentID().equals("*") || equipmentIDPattern.matcher(svc.getId()).find())
                .filter(svc -> filtersContingencyList.getNominalVoltage().equals("*") || filterByVoltage(svc.getVoltageSetpoint(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(svc -> new Contingency(svc.getId(), Collections.singletonList(new StaticVarCompensatorContingency(svc.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getSCContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return network.getShuntCompensatorStream()
                .filter(sc -> filtersContingencyList.getEquipmentName().equals("*") || sc.getOptionalName().isPresent() && equipmentNamePattern.matcher(sc.getOptionalName().get()).find())
                .filter(sc -> filtersContingencyList.getEquipmentID().equals("*") || equipmentIDPattern.matcher(sc.getId()).find())
                .filter(sc -> filtersContingencyList.getNominalVoltage().equals("*") || filterByVoltage(sc.getTargetV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(sc -> new Contingency(sc.getId(), Collections.singletonList(new ShuntCompensatorContingency(sc.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getBranchContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return network.getBranchStream()
                .filter(branch -> filtersContingencyList.getEquipmentName().equals("*") || branch.getOptionalName().isPresent() && equipmentNamePattern.matcher(branch.getOptionalName().get().toString()).find())
                .filter(branch -> filtersContingencyList.getEquipmentID().equals("*") || equipmentIDPattern.matcher(branch.getId()).find())
                .filter(branch -> filtersContingencyList.getNominalVoltage().equals("*") || filterByVoltage(branch.getTerminal1().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator())
                        || filterByVoltage(branch.getTerminal2().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(branch -> new Contingency(branch.getId(), Collections.singletonList(new BranchContingency(branch.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getHvdcContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return network.getHvdcLineStream()
                .filter(hvdcLine -> filtersContingencyList.getEquipmentName().equals("*") || hvdcLine.getOptionalName().isPresent() && equipmentNamePattern.matcher(hvdcLine.getOptionalName().get()).find())
                .filter(hvdcLine -> filtersContingencyList.getEquipmentID().equals("*") || equipmentIDPattern.matcher(hvdcLine.getId()).find())
                .filter(hvdcLine -> filtersContingencyList.getNominalVoltage().equals("*") || filterByVoltage(hvdcLine.getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(hvdcLine -> new Contingency(hvdcLine.getId(), Collections.singletonList(new HvdcLineContingency(hvdcLine.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getBusbarSectionContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return network.getBusbarSectionStream()
                .filter(bs -> filtersContingencyList.getEquipmentName().equals("*") || bs.getOptionalName().isPresent() && equipmentNamePattern.matcher(bs.getOptionalName().get()).find())
                .filter(bs -> filtersContingencyList.getEquipmentID().equals("*") || equipmentIDPattern.matcher(bs.getId()).find())
                .filter(bs -> filtersContingencyList.getNominalVoltage().equals("*") || filterByVoltage(bs.getV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(bs -> new Contingency(bs.getId(), Collections.singletonList(new BusbarSectionContingency(bs.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getDanglingLineContingencyList(Network network, FiltersContingencyList filtersContingencyList, Pattern equipmentIDPattern, Pattern equipmentNamePattern) {
        return network.getDanglingLineStream()
                .filter(dl -> filtersContingencyList.getEquipmentName().equals("*") || dl.getOptionalName().isPresent() && equipmentNamePattern.matcher(dl.getOptionalName().get()).find())
                .filter(dl -> filtersContingencyList.getEquipmentID().equals("*") || equipmentIDPattern.matcher(dl.getId()).find())
                .filter(dl -> filtersContingencyList.getNominalVoltage().equals("*") || filterByVoltage(dl.getTerminal().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
                .map(dl -> new Contingency(dl.getId(), Collections.singletonList(new DanglingLineContingency(dl.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getContingencies(FiltersContingencyList filtersContingencyList, Network network) {
        List<Contingency> contingencies = null;
        Pattern equipmentNamePattern = Pattern.compile(filtersContingencyList.getEquipmentName().equals("*") ? ".*" : filtersContingencyList.getEquipmentName(), Pattern.CASE_INSENSITIVE);
        Pattern equipmentIDPattern = Pattern.compile(filtersContingencyList.getEquipmentID().equals("*") ? ".*" : filtersContingencyList.getEquipmentID(), Pattern.CASE_INSENSITIVE);
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
            case BRANCH:
                contingencies = getBranchContingencyList(network, filtersContingencyList, equipmentIDPattern, equipmentNamePattern);
                break;
            default:
                throw new PowsyblException("Unknown equipment type");
        }
        return contingencies;
    }

    private boolean filterByVoltage(double equipmentNominalVoltage, String nominalVoltage, String nominalVoltageOperator) {
        double voltage = Double.parseDouble(nominalVoltage);
        if (nominalVoltageOperator.equals("=")) {
            return equipmentNominalVoltage == voltage;
        } else if (nominalVoltageOperator.equals(">")) {
            return equipmentNominalVoltage > voltage;
        } else if (nominalVoltageOperator.equals(">=")) {
            return equipmentNominalVoltage >= voltage;
        } else if (nominalVoltageOperator.equals("<")) {
            return equipmentNominalVoltage <= voltage;
        } else if (nominalVoltageOperator.equals("<=")) {
            return equipmentNominalVoltage <= voltage;
        } else {
            throw new PowsyblException("Unknown nominal voltage operator");
        }
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
