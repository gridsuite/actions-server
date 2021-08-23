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

    private final ScriptContingencyListRepository scriptContingencyListRepository;

    private final FiltersContingencyListRepository filtersContingencyListRepository;

    private final NetworkStoreService networkStoreService;

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
        return new ScriptContingencyList(entity.getId(), entity.getName(), entity.getScript() != null ? entity.getScript() : "",
                entity.getDescription(), entity.getUserId(), entity.isPrivate());
    }

    private static FiltersContingencyList fromFilterContingencyListEntity(FiltersContingencyListEntity entity) {
        return new FiltersContingencyList(entity.getId(), entity.getName(), entity.getEquipmentId(), entity.getEquipmentName(),
            entity.getEquipmentType(), entity.getNominalVoltage(), entity.getNominalVoltageOperator(),
                entity.getCountries(), entity.getDescription(), entity.getUserId(), entity.isPrivate());
    }

    private static FiltersContingencyListAttributes fromFilterContingencyListEntityAttributes(FiltersContingencyListEntity entity) {
        return new FiltersContingencyListAttributes(entity.getId(), entity.getName(), entity.getEquipmentId(), entity.getEquipmentName(),
            entity.getEquipmentType(), entity.getNominalVoltage(), entity.getNominalVoltageOperator(),
                entity.getCountries(), entity.getDescription(), entity.getUserId(), entity.isPrivate());
    }

    private static String sanitizeParam(String param) {
        return param != null ? param.replaceAll("[\n|\r|\t]", "_") : null;
    }

    List<ScriptContingencyList> getScriptContingencyLists(String userId) {
        return scriptContingencyListRepository.findByUserIdOrIsPrivate(userId, false).stream().map(ContingencyListService::fromScriptContingencyListEntity).collect(Collectors.toList());
    }

    List<ContingencyListAttributes> getContingencyLists(String userId) {
        return Stream.concat(
            scriptContingencyListRepository.findByUserIdOrIsPrivate(userId, false).stream().map(scriptContingencyListEntity ->
                new ContingencyListAttributes(scriptContingencyListEntity.getId(), scriptContingencyListEntity.getName(), ContingencyListType.SCRIPT,
                        scriptContingencyListEntity.getCreationDate(), scriptContingencyListEntity.getModificationDate(),
                        scriptContingencyListEntity.getDescription(), scriptContingencyListEntity.getUserId(), scriptContingencyListEntity.isPrivate())),
            filtersContingencyListRepository.findByUserIdOrIsPrivate(userId, false).stream().map(filtersContingencyListEntity ->
                new ContingencyListAttributes(filtersContingencyListEntity.getId(), filtersContingencyListEntity.getName(), ContingencyListType.FILTERS,
                        filtersContingencyListEntity.getCreationDate(), filtersContingencyListEntity.getModificationDate(),
                        filtersContingencyListEntity.getDescription(), filtersContingencyListEntity.getUserId(), filtersContingencyListEntity.isPrivate()))
        ).collect(Collectors.toList());
    }

    List<ContingencyListAttributes> getContingencyLists(List<UUID> ids, String userId) {
        return Stream.concat(
            scriptContingencyListRepository.findAllByUuids(ids, userId).stream().map(scriptContingencyListEntity ->
                new ContingencyListAttributes(scriptContingencyListEntity.getId(), scriptContingencyListEntity.getName(), ContingencyListType.SCRIPT,
                        scriptContingencyListEntity.getCreationDate(), scriptContingencyListEntity.getModificationDate(),
                        scriptContingencyListEntity.getDescription(), scriptContingencyListEntity.getUserId(), scriptContingencyListEntity.isPrivate())),
            filtersContingencyListRepository.findAllByUuids(ids, userId).stream().map(filtersContingencyListEntity ->
                new ContingencyListAttributes(filtersContingencyListEntity.getId(), filtersContingencyListEntity.getName(), ContingencyListType.FILTERS,
                        filtersContingencyListEntity.getCreationDate(), filtersContingencyListEntity.getModificationDate(),
                        filtersContingencyListEntity.getDescription(), filtersContingencyListEntity.getUserId(), filtersContingencyListEntity.isPrivate()))
        ).collect(Collectors.toList());
    }

    List<FiltersContingencyList> getFilterContingencyLists(String userId) {
        return filtersContingencyListRepository.findAllWithCountriesByUserIdOrIsPrivate(userId, false).stream().map(ContingencyListService::fromFilterContingencyListEntity).collect(Collectors.toList());
    }

    Optional<ScriptContingencyList> getScriptContingencyList(UUID id, String userId) {
        Objects.requireNonNull(id);
        return scriptContingencyListRepository.findByIdAndUserIdOrIsPrivate(id, userId, false).map(ContingencyListService::fromScriptContingencyListEntity);
    }

    @Transactional(readOnly = true)
    public Optional<FiltersContingencyListEntity> doGetFiltersContingencyListWithPreFetchedCountries(UUID id, String userId) {
        return filtersContingencyListRepository.findByIdAndUserIdOrIsPrivate(id, userId, false).map(entity -> {
            @SuppressWarnings("unused")
            int ignoreSize = entity.getCountries().size();
            return entity;
        });
    }

    public Optional<FiltersContingencyList> getFiltersContingencyList(UUID id, String userId) {
        Objects.requireNonNull(id);
        return self.doGetFiltersContingencyListWithPreFetchedCountries(id, userId).map(ContingencyListService::fromFilterContingencyListEntity);
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

    Optional<List<Contingency>> exportContingencyList(UUID id, UUID networkUuid, String userId) {
        Objects.requireNonNull(id);

        return getScriptContingencyList(id, userId).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid))
            .or(() -> getFiltersContingencyList(id, userId).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid)));
    }

    ScriptContingencyList createScriptContingencyList(ScriptContingencyList script, String userId, Boolean isPrivate) {
        Objects.requireNonNull(script.getName());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(script.getName()));
        }
        script.setUserId(userId);
        script.setPrivate(isPrivate);
        return fromScriptContingencyListEntity(scriptContingencyListRepository.save(new ScriptContingencyListEntity(script)));
    }

    void modifyScriptContingencyList(UUID id, ScriptContingencyList script, String userId) {
        Objects.requireNonNull(script.getName());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", sanitizeParam(script.getName()));
        }

        Optional<ScriptContingencyListEntity> entity = scriptContingencyListRepository.findByIdAndUserIdOrIsPrivate(id, userId, false);
        if (entity.isPresent()) {
            if (!entity.get().getUserId().equals(userId)) {  // only the owner can modify the contingency list
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The contingency list '" + entity.get().getName() + "' can only be modified by it's owner");
            } else {
                script.setUserId(entity.get().getUserId());
                script.setPrivate(entity.get().isPrivate());
                scriptContingencyListRepository.save(entity.get().update(script));
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        }
    }

    public FiltersContingencyList createFilterContingencyList(FiltersContingencyListAttributes filtersContingencyListAttributes,
                                                              String userId, Boolean isPrivate) {
        Objects.requireNonNull(filtersContingencyListAttributes.getName());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create filter contingency list '{}'", filtersContingencyListAttributes.getName());
        }
        filtersContingencyListAttributes.setUserId(userId);
        filtersContingencyListAttributes.setPrivate(isPrivate);
        return fromFilterContingencyListEntity(filtersContingencyListRepository.save(new FiltersContingencyListEntity(filtersContingencyListAttributes)));
    }

    public void modifyFilterContingencyList(UUID id, FiltersContingencyListAttributes filtersContingencyListAttributes, String userId) {
        Objects.requireNonNull(filtersContingencyListAttributes.getName());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Modify filter contingency list '{}'", filtersContingencyListAttributes.getName());
        }

        Optional<FiltersContingencyListEntity> entity = filtersContingencyListRepository.findByIdAndUserIdOrIsPrivate(id, userId, false);
        if (entity.isPresent()) {
            if (!entity.get().getUserId().equals(userId)) {  // only the owner can modify the contingency list
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The contingency list '" + entity.get().getName() + "' can only be modified by it's owner");
            } else {
                filtersContingencyListAttributes.setUserId(entity.get().getUserId());
                filtersContingencyListAttributes.setPrivate(entity.get().isPrivate());
                filtersContingencyListRepository.save(entity.get().update(filtersContingencyListAttributes));
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        }
    }

    @Transactional
    public void deleteContingencyList(UUID id, String userId) {
        Objects.requireNonNull(id);

        Optional<FiltersContingencyListEntity> fEntity = filtersContingencyListRepository.findByIdAndUserIdOrIsPrivate(id, userId, false);
        if (fEntity.isPresent()) {
            if (!fEntity.get().getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The contingency list '" + fEntity.get().getName() + "' can only be deleted by it's owner");
            } else {
                filtersContingencyListRepository.deleteById(id);
            }
        } else {
            Optional<ScriptContingencyListEntity> sEntity = scriptContingencyListRepository.findByIdAndUserIdOrIsPrivate(id, userId, false);
            if (sEntity.isPresent()) {
                if (!sEntity.get().getUserId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The contingency list '" + sEntity.get().getName() + "' can only be deleted by it's owner");
                } else {
                    scriptContingencyListRepository.deleteById(id);
                }
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
            }
        }
    }

    private String generateGroovyScriptFromFilters(FiltersContingencyListAttributes filtersContingencyListAttributes) {
        return filtersToScript.generateGroovyScriptFromFilters(filtersContingencyListAttributes);
    }

    @Transactional
    public ScriptContingencyList replaceFilterContingencyListWithScript(UUID id, String userId) {
        Objects.requireNonNull(id);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Replace filter contingency list with script'{}'", id);
        }
        Optional<FiltersContingencyListEntity> filter = self.doGetFiltersContingencyListWithPreFetchedCountries(id, userId);
        return filter.map(entity -> {
            String script = generateGroovyScriptFromFilters(fromFilterContingencyListEntityAttributes(entity));
            var scriptContingencyListEntity = new ScriptContingencyListEntity(new ScriptContingencyList(id, entity.getName(), script, entity.getDescription(), userId, entity.isPrivate()));
            var res = fromScriptContingencyListEntity(scriptContingencyListRepository.save(scriptContingencyListEntity));
            filtersContingencyListRepository.deleteById(id);
            return res;
        }).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        });
    }

    @Transactional
    public ScriptContingencyList newScriptFromFiltersContingencyList(UUID id, String scriptName, String userId) {
        Objects.requireNonNull(id);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("New script from filter contingency list'{}'", id);
        }

        Optional<FiltersContingencyListEntity> filter = self.doGetFiltersContingencyListWithPreFetchedCountries(id, userId);
        return filter.map(entity -> {
            String script = generateGroovyScriptFromFilters(fromFilterContingencyListEntityAttributes(entity));
            return fromScriptContingencyListEntity(scriptContingencyListRepository.save(new ScriptContingencyListEntity(new ScriptContingencyList(UUID.randomUUID(), scriptName, script, entity.getDescription(), entity.getUserId(), entity.isPrivate()))));
        }).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        });
    }

    public ContingencyList changeAccessRights(UUID id, String userId, boolean toPrivate) {
        Objects.requireNonNull(id);

        Optional<FiltersContingencyListEntity> fEntity = filtersContingencyListRepository.findByIdAndUserIdOrIsPrivate(id, userId, false);
        if (fEntity.isPresent()) {
            if (!fEntity.get().getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The access rights of contingency list '" + fEntity.get().getName() + "' can only be modified by it's owner");
            } else {
                filtersContingencyListRepository.save(fEntity.get().updateIsPrivate(toPrivate));
                return fromFilterContingencyListEntity(fEntity.get());
            }
        } else {
            Optional<ScriptContingencyListEntity> sEntity = scriptContingencyListRepository.findByIdAndUserIdOrIsPrivate(id, userId, false);
            if (sEntity.isPresent()) {
                if (!sEntity.get().getUserId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The access rights of contingency list '" + sEntity.get().getName() + "' can only be modified by it's owner");
                } else {
                    scriptContingencyListRepository.save(sEntity.get().updateIsPrivate(toPrivate));
                    return fromScriptContingencyListEntity(sEntity.get());
                }
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
            }
        }
    }
}
