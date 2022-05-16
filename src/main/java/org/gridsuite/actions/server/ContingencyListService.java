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
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.gridsuite.actions.server.dto.*;
import org.gridsuite.actions.server.dto.ContingencyList;
import org.gridsuite.actions.server.entities.FormContingencyListEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.FormContingencyListRepository;
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

    private FormContingencyListRepository formContingencyListRepository;

    private NetworkStoreService networkStoreService;

    private final PathMatcher antMatcher = new AntPathMatcher("\0");

    private final FormToGroovyScript formToScript = new FormToGroovyScript();

    // Self injection for @transactional support in internal calls to other methods of this service
    @Autowired
    ContingencyListService self;

    public ContingencyListService(ScriptContingencyListRepository scriptContingencyListRepository,
                                  FormContingencyListRepository formContingencyListRepository,
                                  NetworkStoreService networkStoreService) {
        this.scriptContingencyListRepository = scriptContingencyListRepository;
        this.formContingencyListRepository = formContingencyListRepository;
        this.networkStoreService = networkStoreService;
    }

    private static ScriptContingencyList fromScriptContingencyListEntity(ScriptContingencyListEntity entity) {
        return new ScriptContingencyList(entity.getId(), entity.getScript() != null ? entity.getScript() : "");
    }

    private static FormContingencyList fromFormContingencyListEntity(FormContingencyListEntity entity) {
        return new FormContingencyList(entity.getId(), entity.getEquipmentId(), entity.getEquipmentName(),
            entity.getEquipmentType(), entity.getNominalVoltage(), entity.getNominalVoltageOperator(), entity.getCountries());
    }

    List<ScriptContingencyList> getScriptContingencyLists() {
        return scriptContingencyListRepository.findAll().stream().map(ContingencyListService::fromScriptContingencyListEntity).collect(Collectors.toList());
    }

    List<ContingencyListAttributes> getContingencyLists() {
        return Stream.concat(
            scriptContingencyListRepository.findAll().stream().map(scriptContingencyListEntity ->
                new ContingencyListAttributes(scriptContingencyListEntity.getId(), ContingencyListType.SCRIPT, scriptContingencyListEntity.getCreationDate(), scriptContingencyListEntity.getModificationDate())),
            formContingencyListRepository.findAll().stream().map(formContingencyListEntity ->
                new ContingencyListAttributes(formContingencyListEntity.getId(), ContingencyListType.FORM, formContingencyListEntity.getCreationDate(), formContingencyListEntity.getModificationDate()))
        ).collect(Collectors.toList());
    }

    List<ContingencyListAttributes> getContingencyLists(List<UUID> ids) {
        return Stream.concat(
            scriptContingencyListRepository.findAllById(ids).stream().map(scriptContingencyListEntity ->
                new ContingencyListAttributes(scriptContingencyListEntity.getId(), ContingencyListType.SCRIPT, scriptContingencyListEntity.getCreationDate(), scriptContingencyListEntity.getModificationDate())),
            formContingencyListRepository.findAllById(ids).stream().map(formContingencyListEntity ->
                new ContingencyListAttributes(formContingencyListEntity.getId(), ContingencyListType.FORM, formContingencyListEntity.getCreationDate(), formContingencyListEntity.getModificationDate()))
        ).collect(Collectors.toList());
    }

    List<FormContingencyList> getFormContingencyLists() {
        return formContingencyListRepository.findAllWithCountries().stream().map(ContingencyListService::fromFormContingencyListEntity).collect(Collectors.toList());
    }

    Optional<ScriptContingencyList> getScriptContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return scriptContingencyListRepository.findById(id).map(ContingencyListService::fromScriptContingencyListEntity);
    }

    @Transactional(readOnly = true)
    public Optional<FormContingencyListEntity> doGetFormContingencyListWithPreFetchedCountries(UUID name) {
        return formContingencyListRepository.findById(name).map(entity -> {
            @SuppressWarnings("unused")
            int ignoreSize = entity.getCountries().size();
            return entity;
        });
    }

    public Optional<FormContingencyList> getFormContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return self.doGetFormContingencyListWithPreFetchedCountries(id).map(ContingencyListService::fromFormContingencyListEntity);
    }

    private List<Contingency> toPowSyBlContingencyList(ContingencyList contingencyList, UUID networkUuid, String variantId) {
        Network network;
        if (networkUuid == null) {
            // use an empty network, script might not have need to network
            network = new NetworkFactoryImpl().createNetwork("empty", "empty");
        } else {
            network = networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION);
            if (network == null) {
                throw new PowsyblException("Network '" + networkUuid + "' not found");
            }
            if (variantId != null) {
                network.getVariantManager().setWorkingVariant(variantId);
            }
        }

        if (contingencyList instanceof ScriptContingencyList) {
            String script = ((ScriptContingencyList) contingencyList).getScript();
            ImportCustomizer customizer = new ImportCustomizer();
            customizer.addImports("org.gridsuite.actions.server.utils.FiltersUtils");
            customizer.addStaticStars("org.gridsuite.actions.server.utils.FiltersUtils");
            return new ContingencyDslLoader(script).load(network, customizer);
        } else if (contingencyList instanceof FormContingencyList) {
            FormContingencyList formContingencyList = (FormContingencyList) contingencyList;
            return getContingencies(formContingencyList, network);
        } else {
            throw new PowsyblException("Contingency list implementation not yet supported: " + contingencyList.getClass().getSimpleName());
        }
    }

    boolean countryFilter(Connectable<?> con, FormContingencyList filter) {
        Set<String> countries = filter.getCountries();
        return countries.isEmpty() || con.getTerminals().stream().anyMatch(connectable -> {
            Optional<Country> country = connectable.getVoltageLevel().getSubstation().flatMap(Substation::getCountry);
            return country.map(c -> countries.contains(c.name())).orElse(false);
        });
    }

    boolean countryFilter(HvdcLine hvdcLine, FormContingencyList filter) {
        return countryFilter(hvdcLine.getConverterStation1(), filter) || countryFilter(hvdcLine.getConverterStation2(), filter);
    }

    private <I extends Injection<I>> Stream<Injection<I>> getInjectionContingencyList(Stream<Injection<I>> stream, FormContingencyList formContingencyList) {
        return stream
            .filter(injection -> matches(formContingencyList.getEquipmentID(), injection.getId()) || injection.getOptionalName().isPresent() && matches(formContingencyList.getEquipmentName(), injection.getOptionalName().get()))
            .filter(injection -> formContingencyList.getNominalVoltage() == -1 || filterByVoltage(injection.getTerminal().getVoltageLevel().getNominalV(), formContingencyList.getNominalVoltage(), formContingencyList.getNominalVoltageOperator()))
            .filter(injection -> countryFilter(injection, formContingencyList));
    }

    private List<Contingency> getGeneratorContingencyList(Network network, FormContingencyList formContingencyList) {
        return getInjectionContingencyList(network.getGeneratorStream().map(gen -> gen), formContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new GeneratorContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getSVCContingencyList(Network network, FormContingencyList formContingencyList) {
        return getInjectionContingencyList(network.getStaticVarCompensatorStream().map(svc -> svc), formContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new StaticVarCompensatorContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getSCContingencyList(Network network, FormContingencyList formContingencyList) {
        return getInjectionContingencyList(network.getShuntCompensatorStream().map(sc -> sc), formContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new ShuntCompensatorContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private <I extends Branch<I>> List<Contingency> getBranchContingencyList(Stream<Branch<I>> stream, FormContingencyList formContingencyList) {
        return stream
            .filter(branch -> matches(formContingencyList.getEquipmentID(), branch.getId()) || branch.getOptionalName().isPresent() && matches(formContingencyList.getEquipmentName(), branch.getOptionalName().get()))
            .filter(branch -> formContingencyList.getNominalVoltage() == -1 || filterByVoltage(branch.getTerminal1().getVoltageLevel().getNominalV(), formContingencyList.getNominalVoltage(), formContingencyList.getNominalVoltageOperator())
                || filterByVoltage(branch.getTerminal2().getVoltageLevel().getNominalV(), formContingencyList.getNominalVoltage(), formContingencyList.getNominalVoltageOperator()))
            .filter(branch -> countryFilter(branch, formContingencyList))
            .map(branch -> new Contingency(branch.getId(), Collections.singletonList(new BranchContingency(branch.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getLineContingencyList(Network network, FormContingencyList formContingencyList) {
        return getBranchContingencyList(network.getLineStream().map(line -> line), formContingencyList);
    }

    private List<Contingency> get2WTransformerContingencyList(Network network, FormContingencyList formContingencyList) {
        return getBranchContingencyList(network.getTwoWindingsTransformerStream().map(twt -> twt), formContingencyList);
    }

    private List<Contingency> getHvdcContingencyList(Network network, FormContingencyList formContingencyList) {
        return network.getHvdcLineStream()
            .filter(hvdcLine -> matches(formContingencyList.getEquipmentID(), hvdcLine.getId()) || hvdcLine.getOptionalName().isPresent() && matches(formContingencyList.getEquipmentName(), hvdcLine.getOptionalName().get()))
            .filter(hvdcLine -> formContingencyList.getNominalVoltage() == -1 || filterByVoltage(hvdcLine.getNominalV(), formContingencyList.getNominalVoltage(), formContingencyList.getNominalVoltageOperator()))
            .filter(hvdcLine -> countryFilter(hvdcLine, formContingencyList))
            .map(hvdcLine -> new Contingency(hvdcLine.getId(), Collections.singletonList(new HvdcLineContingency(hvdcLine.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getBusbarSectionContingencyList(Network network, FormContingencyList formContingencyList) {
        return getInjectionContingencyList(network.getBusbarSectionStream().map(bbs -> bbs), formContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new BusbarSectionContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private List<Contingency> getDanglingLineContingencyList(Network network, FormContingencyList formContingencyList) {
        return getInjectionContingencyList(network.getDanglingLineStream().map(dl -> dl), formContingencyList)
            .map(injection -> new Contingency(injection.getId(), Collections.singletonList(new DanglingLineContingency(injection.getId()))))
            .collect(Collectors.toList());
    }

    private boolean matches(String pattern, String path) {
        return antMatcher.match(pattern, path);
    }

    private List<Contingency> getContingencies(FormContingencyList formContingencyList, Network network) {
        List<Contingency> contingencies;
        switch (EquipmentType.valueOf(formContingencyList.getEquipmentType())) {
            case GENERATOR:
                contingencies = getGeneratorContingencyList(network, formContingencyList);
                break;
            case STATIC_VAR_COMPENSATOR:
                contingencies = getSVCContingencyList(network, formContingencyList);
                break;
            case SHUNT_COMPENSATOR:
                contingencies = getSCContingencyList(network, formContingencyList);
                break;
            case HVDC_LINE:
                contingencies = getHvdcContingencyList(network, formContingencyList);
                break;
            case BUSBAR_SECTION:
                contingencies = getBusbarSectionContingencyList(network, formContingencyList);
                break;
            case DANGLING_LINE:
                contingencies = getDanglingLineContingencyList(network, formContingencyList);
                break;
            case LINE:
                contingencies = getLineContingencyList(network, formContingencyList);
                break;
            case TWO_WINDINGS_TRANSFORMER:
                contingencies = get2WTransformerContingencyList(network, formContingencyList);
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

    Optional<List<Contingency>> exportContingencyList(UUID id, UUID networkUuid, String variantId) {
        Objects.requireNonNull(id);

        return getScriptContingencyList(id).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid, variantId))
            .or(() -> getFormContingencyList(id).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid, variantId)));
    }

    ScriptContingencyList createScriptContingencyList(UUID id, ScriptContingencyList script) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", script.getId());
        }
        ScriptContingencyListEntity entity = new ScriptContingencyListEntity(script);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromScriptContingencyListEntity(scriptContingencyListRepository.save(entity));
    }

    Optional<ScriptContingencyList> createScriptContingencyList(UUID parentListId, UUID id) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}' based on '{}'", id, parentListId);
        }
        ScriptContingencyList parentScriptContingencyList = getScriptContingencyList(parentListId).orElse(null);
        if (parentScriptContingencyList != null) {
            ScriptContingencyListEntity entity = new ScriptContingencyListEntity(parentScriptContingencyList);
            entity.setId(id == null ? UUID.randomUUID() : id);
            return Optional.of(fromScriptContingencyListEntity(scriptContingencyListRepository.save(entity)));
        }
        return Optional.empty();
    }

    void modifyScriptContingencyList(UUID id, ScriptContingencyList script) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create script contingency list '{}'", script.getId());
        }
        scriptContingencyListRepository.save(scriptContingencyListRepository.getOne(id).update(script));
    }

    public FormContingencyList createFormContingencyList(UUID id, FormContingencyList formContingencyList) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create form contingency list '{}' based on ", formContingencyList.getId());
        }
        FormContingencyListEntity entity = new FormContingencyListEntity(formContingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromFormContingencyListEntity(formContingencyListRepository.save(entity));
    }

    public Optional<FormContingencyList> createFormContingencyList(UUID parentListId, UUID id) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Create form contingency list '{}' based on '{}'", id, parentListId);
        }
        FormContingencyList parentFormContingencyList = getFormContingencyList(parentListId).orElse(null);
        if (parentFormContingencyList != null) {
            FormContingencyListEntity entity = new FormContingencyListEntity(parentFormContingencyList);
            entity.setId(id == null ? UUID.randomUUID() : id);
            return Optional.of(fromFormContingencyListEntity(formContingencyListRepository.save(entity)));
        }
        return Optional.empty();
    }

    public void modifyFormContingencyList(UUID id, FormContingencyList formContingencyList) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Modify form contingency list '{}'", formContingencyList.getId());
        }
        // throw if not found
        formContingencyListRepository.save(formContingencyListRepository.getOne(id).update(formContingencyList));
    }

    @Transactional
    public void deleteContingencyList(UUID id) {
        Objects.requireNonNull(id);
        // if there is no form contingency list by this Id, deleted count == 0
        if (formContingencyListRepository.deleteFormContingencyListEntityById(id) == 0) {
            scriptContingencyListRepository.deleteById(id);
        }
    }

    private String generateGroovyScriptFromForm(FormContingencyList formContingencyList) {
        return formToScript.generateGroovyScriptFromForm(formContingencyList);
    }

    @Transactional
    public ScriptContingencyList replaceFormContingencyListWithScript(UUID id) {
        Objects.requireNonNull(id);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Replace form contingency list with script'{}'", id);
        }
        Optional<FormContingencyListEntity> formContingencyList = self.doGetFormContingencyListWithPreFetchedCountries(id);
        return formContingencyList.map(entity -> {
            String script = generateGroovyScriptFromForm(fromFormContingencyListEntity(entity));
            var scriptContingencyListEntity = new ScriptContingencyListEntity(new ScriptContingencyList(id, script));
            scriptContingencyListEntity.setId(id);
            var res = fromScriptContingencyListEntity(scriptContingencyListRepository.save(scriptContingencyListEntity));
            formContingencyListRepository.deleteById(id);
            return res;
        }).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        });
    }

    @Transactional
    public ScriptContingencyList newScriptFromFormContingencyList(UUID id, UUID newId) {
        Objects.requireNonNull(id);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("New script from form contingency list'{}'", id);
        }

        Optional<FormContingencyListEntity> formContingencyList = self.doGetFormContingencyListWithPreFetchedCountries(id);
        return formContingencyList.map(entity -> {
            String script = generateGroovyScriptFromForm(fromFormContingencyListEntity(entity));
            ScriptContingencyListEntity scriptEntity = new ScriptContingencyListEntity(new ScriptContingencyList(null, script));
            scriptEntity.setId(newId == null ? UUID.randomUUID() : newId);
            return fromScriptContingencyListEntity(scriptContingencyListRepository.save(scriptEntity));
        }).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        });
    }
}
