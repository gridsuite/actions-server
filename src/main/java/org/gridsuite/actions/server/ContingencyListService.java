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
import org.gridsuite.actions.server.dto.ContingencyList;
import org.gridsuite.actions.server.entities.FiltersContingencyListEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.FiltersContingencyListRepository;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.gridsuite.actions.server.utils.NominalVoltageOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
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

    private final PathMatcher antMatcher = new AntPathMatcher("\0");

    private final FiltersToGroovyScript filtersToScript = new FiltersToGroovyScript();

    // Self injection for @transactional support in internal calls to other methods of this service
    @Autowired
    ContingencyListService self;

    public ContingencyListService(ScriptContingencyListRepository scriptContingencyListRepository,
                                  FiltersContingencyListRepository filtersContingencyListRepository,
                                  NetworkStoreService networkStoreService) {
        this.scriptContingencyListRepository = scriptContingencyListRepository;
        this.filtersContingencyListRepository = filtersContingencyListRepository;
        this.networkStoreService = networkStoreService;
    }

    private static ScriptContingencyList fromScriptContingencyListEntity(ScriptContingencyListEntity entity) {
        return new ScriptContingencyList(entity.getId(), entity.getName(), entity.getScript() != null ? entity.getScript() : "", entity.getDescription());
    }

    private static FiltersContingencyList fromFilterContingencyListEntity(FiltersContingencyListEntity entity) {
        return new FiltersContingencyList(entity.getId(), entity.getName(), entity.getEquipmentId(), entity.getEquipmentName(),
            entity.getEquipmentType(), entity.getNominalVoltage(), entity.getNominalVoltageOperator(), entity.getCountries(), entity.getDescription());
    }

    private static FiltersContingencyListAttributes fromFilterContingencyListEntityAttributes(FiltersContingencyListEntity entity) {
        return new FiltersContingencyListAttributes(entity.getId(), entity.getName(), entity.getEquipmentId(), entity.getEquipmentName(),
            entity.getEquipmentType(), entity.getNominalVoltage(), entity.getNominalVoltageOperator(), entity.getCountries(), entity.getDescription());
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
                new ContingencyListAttributes(scriptContingencyListEntity.getId(), scriptContingencyListEntity.getName(), ContingencyListType.SCRIPT, scriptContingencyListEntity.getCreationDate(), scriptContingencyListEntity.getModificationDate(), scriptContingencyListEntity.getDescription())),
            filtersContingencyListRepository.findAll().stream().map(filtersContingencyListEntity ->
                new ContingencyListAttributes(filtersContingencyListEntity.getId(), filtersContingencyListEntity.getName(), ContingencyListType.FILTERS, filtersContingencyListEntity.getCreationDate(), filtersContingencyListEntity.getModificationDate(), filtersContingencyListEntity.getDescription()))
        ).collect(Collectors.toList());
    }

    List<ContingencyListAttributes> getContingencyLists(List<UUID> ids) {
        return Stream.concat(
            scriptContingencyListRepository.findAllById(ids).stream().map(scriptContingencyListEntity ->
                new ContingencyListAttributes(scriptContingencyListEntity.getId(), scriptContingencyListEntity.getName(), ContingencyListType.SCRIPT, scriptContingencyListEntity.getCreationDate(), scriptContingencyListEntity.getModificationDate(), scriptContingencyListEntity.getDescription())),
            filtersContingencyListRepository.findAllById(ids).stream().map(filtersContingencyListEntity ->
                new ContingencyListAttributes(filtersContingencyListEntity.getId(), filtersContingencyListEntity.getName(), ContingencyListType.FILTERS, filtersContingencyListEntity.getCreationDate(), filtersContingencyListEntity.getModificationDate(), filtersContingencyListEntity.getDescription()))
        ).collect(Collectors.toList());
    }

    List<FiltersContingencyList> getFilterContingencyLists() {
        return filtersContingencyListRepository.findAllWithCountries().stream().map(ContingencyListService::fromFilterContingencyListEntity).collect(Collectors.toList());
    }

    Optional<ScriptContingencyList> getScriptContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return scriptContingencyListRepository.findById(id).map(ContingencyListService::fromScriptContingencyListEntity);
    }

    @Transactional(readOnly = true)
    public Optional<FiltersContingencyListEntity> doGetFiltersContingencyListWithPreFetchedCountries(UUID name) {
        return filtersContingencyListRepository.findById(name).map(entity -> {
            @SuppressWarnings("unused")
            int ignoreSize = entity.getCountries().size();
            return entity;
        });
    }

    public Optional<FiltersContingencyList> getFiltersContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return self.doGetFiltersContingencyListWithPreFetchedCountries(id).map(ContingencyListService::fromFilterContingencyListEntity);
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

    boolean countryFilter(Connectable<?> con, FiltersContingencyList filter) {
        Set<String> countries = filter.getCountries();
        return countries.isEmpty() || con.getTerminals().stream().anyMatch(connectable ->
            connectable.getVoltageLevel().getSubstation().getCountry().isPresent() && countries.contains(connectable.getVoltageLevel().getSubstation().getCountry().get().name()));
    }

    boolean countryFilter(HvdcLine hvdcLine, FiltersContingencyList filter) {
        return countryFilter(hvdcLine.getConverterStation1(), filter) || countryFilter(hvdcLine.getConverterStation2(), filter);
    }

    private <I extends Injection<I>> Stream<Injection<I>> getInjectionContingencyList(Stream<Injection<I>> stream, FiltersContingencyList filtersContingencyList) {
        return stream
            .filter(injection -> matches(filtersContingencyList.getEquipmentID(), injection.getId()) || injection.getOptionalName().isPresent() && matches(filtersContingencyList.getEquipmentName(), injection.getOptionalName().get()))
            .filter(injection -> filtersContingencyList.getNominalVoltage() == -1 || filterByVoltage(injection.getTerminal().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
            .filter(injection -> countryFilter(injection, filtersContingencyList));
    }

    private List<Contingency> getGeneratorContingencyList(Network network, FiltersContingencyList filtersContingencyList) {
        return getInjectionContingencyList(network.getGeneratorStream().map(gen -> gen), filtersContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new GeneratorContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getSVCContingencyList(Network network, FiltersContingencyList filtersContingencyList) {
        return getInjectionContingencyList(network.getStaticVarCompensatorStream().map(svc -> svc), filtersContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new StaticVarCompensatorContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getSCContingencyList(Network network, FiltersContingencyList filtersContingencyList) {
        return getInjectionContingencyList(network.getShuntCompensatorStream().map(sc -> sc), filtersContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new ShuntCompensatorContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private <I extends Branch<I>> List<Contingency> getBranchContingencyList(Stream<Branch<I>> stream, FiltersContingencyList filtersContingencyList) {
        return stream
            .filter(branch -> matches(filtersContingencyList.getEquipmentID(), branch.getId()) || branch.getOptionalName().isPresent() && matches(filtersContingencyList.getEquipmentName(), branch.getOptionalName().get()))
            .filter(branch -> filtersContingencyList.getNominalVoltage() == -1 || filterByVoltage(branch.getTerminal1().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator())
                || filterByVoltage(branch.getTerminal2().getVoltageLevel().getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
            .filter(branch -> countryFilter(branch, filtersContingencyList))
            .map(branch -> new Contingency(branch.getId(), Collections.singletonList(new BranchContingency(branch.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getLineContingencyList(Network network, FiltersContingencyList filtersContingencyList) {
        return getBranchContingencyList(network.getLineStream().map(line -> line), filtersContingencyList);
    }

    private List<Contingency> get2WTransformerContingencyList(Network network, FiltersContingencyList filtersContingencyList) {
        return getBranchContingencyList(network.getTwoWindingsTransformerStream().map(twt -> twt), filtersContingencyList);
    }

    private List<Contingency> getHvdcContingencyList(Network network, FiltersContingencyList filtersContingencyList) {
        return network.getHvdcLineStream()
            .filter(hvdcLine -> matches(filtersContingencyList.getEquipmentID(), hvdcLine.getId()) || hvdcLine.getOptionalName().isPresent() && matches(filtersContingencyList.getEquipmentName(), hvdcLine.getOptionalName().get()))
            .filter(hvdcLine -> filtersContingencyList.getNominalVoltage() == -1 || filterByVoltage(hvdcLine.getNominalV(), filtersContingencyList.getNominalVoltage(), filtersContingencyList.getNominalVoltageOperator()))
            .filter(hvdcLine -> countryFilter(hvdcLine, filtersContingencyList))
            .map(hvdcLine -> new Contingency(hvdcLine.getId(), Collections.singletonList(new HvdcLineContingency(hvdcLine.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getBusbarSectionContingencyList(Network network, FiltersContingencyList filtersContingencyList) {
        return getInjectionContingencyList(network.getBusbarSectionStream().map(bbs -> bbs), filtersContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new BusbarSectionContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getDanglingLineContingencyList(Network network, FiltersContingencyList filtersContingencyList) {
        return getInjectionContingencyList(network.getDanglingLineStream().map(dl -> dl), filtersContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new DanglingLineContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private boolean matches(String pattern, String path) {
        return antMatcher.match(pattern, path);
    }

    private List<Contingency> getContingencies(FiltersContingencyList filtersContingencyList, Network network) {
        List<Contingency> contingencies;
        switch (EquipmentType.valueOf(filtersContingencyList.getEquipmentType())) {
            case GENERATOR:
                contingencies = getGeneratorContingencyList(network, filtersContingencyList);
                break;
            case STATIC_VAR_COMPENSATOR:
                contingencies = getSVCContingencyList(network, filtersContingencyList);
                break;
            case SHUNT_COMPENSATOR:
                contingencies = getSCContingencyList(network, filtersContingencyList);
                break;
            case HVDC_LINE:
                contingencies = getHvdcContingencyList(network, filtersContingencyList);
                break;
            case BUSBAR_SECTION:
                contingencies = getBusbarSectionContingencyList(network, filtersContingencyList);
                break;
            case DANGLING_LINE:
                contingencies = getDanglingLineContingencyList(network, filtersContingencyList);
                break;
            case LINE:
                contingencies = getLineContingencyList(network, filtersContingencyList);
                break;
            case TWO_WINDINGS_TRANSFORMER:
                contingencies = get2WTransformerContingencyList(network, filtersContingencyList);
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

    Optional<List<Contingency>> exportContingencyList(UUID id, UUID networkUuid) {
        Objects.requireNonNull(id);

        return getScriptContingencyList(id).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid))
            .or(() -> getFiltersContingencyList(id).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid)));
    }

    ScriptContingencyList createScriptContingencyList(ScriptContingencyList script) {
        Objects.requireNonNull(script.getName());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(script.getName()));
        }
        return fromScriptContingencyListEntity(scriptContingencyListRepository.save(new ScriptContingencyListEntity(script)));
    }

    void modifyScriptContingencyList(UUID id, ScriptContingencyList script) {
        Objects.requireNonNull(script.getName());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(script.getName()));
        }
        scriptContingencyListRepository.save(scriptContingencyListRepository.getOne(id).update(script));
    }

    public FiltersContingencyList createFilterContingencyList(FiltersContingencyListAttributes filtersContingencyListAttributes) {
        Objects.requireNonNull(filtersContingencyListAttributes.getName());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create filter contingency list '{}'", filtersContingencyListAttributes.getName());
        }
        return fromFilterContingencyListEntity(filtersContingencyListRepository.save(new FiltersContingencyListEntity(filtersContingencyListAttributes)));
    }

    public void modifyFilterContingencyList(UUID id, FiltersContingencyListAttributes filtersContingencyListAttributes) {
        Objects.requireNonNull(filtersContingencyListAttributes.getName());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Modify filter contingency list '{}'", filtersContingencyListAttributes.getName());
        }
        // throw if not found
        filtersContingencyListRepository.save(filtersContingencyListRepository.getOne(id).update(filtersContingencyListAttributes));
    }

    @Transactional
    public void deleteContingencyList(UUID id) {
        Objects.requireNonNull(id);
        // if there is no filter contingency list by this Id, deleted count == 0
        if (filtersContingencyListRepository.deleteFiltersContingencyListEntityById(id) == 0) {
            scriptContingencyListRepository.deleteById(id);
        }
    }

    private String generateGroovyScriptFromFilters(FiltersContingencyListAttributes filtersContingencyListAttributes) {
        return filtersToScript.generateGroovyScriptFromFilters(filtersContingencyListAttributes);
    }

    @Transactional
    public ScriptContingencyList replaceFilterContingencyListWithScript(UUID id) {
        Objects.requireNonNull(id);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Replace filter contingency list with script'{}'", id);
        }
        Optional<FiltersContingencyListEntity> filter = self.doGetFiltersContingencyListWithPreFetchedCountries(id);
        return filter.map(entity -> {
            String script = generateGroovyScriptFromFilters(fromFilterContingencyListEntityAttributes(entity));
            var scriptContingencyListEntity = new ScriptContingencyListEntity(new ScriptContingencyList(id, entity.getName(), script, entity.getDescription()));
            var res = fromScriptContingencyListEntity(scriptContingencyListRepository.save(scriptContingencyListEntity));
            filtersContingencyListRepository.deleteById(id);
            return res;
        }).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        });
    }

    @Transactional
    public ScriptContingencyList newScriptFromFiltersContingencyList(UUID id, String scriptName) {
        Objects.requireNonNull(id);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("New script from filter contingency list'{}'", id);
        }

        Optional<FiltersContingencyListEntity> filter = self.doGetFiltersContingencyListWithPreFetchedCountries(id);
        return filter.map(entity -> {
            String script = generateGroovyScriptFromFilters(fromFilterContingencyListEntityAttributes(entity));
            return fromScriptContingencyListEntity(scriptContingencyListRepository.save(new ScriptContingencyListEntity(new ScriptContingencyList(UUID.randomUUID(), scriptName, script, entity.getDescription()))));
        }).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        });
    }
}
