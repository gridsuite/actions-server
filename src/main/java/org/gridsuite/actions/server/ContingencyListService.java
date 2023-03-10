/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.*;
import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import com.powsybl.contingency.contingency.list.identifier.IdBasedNetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifierList;
import com.powsybl.contingency.dsl.ContingencyDslLoader;
import com.powsybl.iidm.network.*;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.gridsuite.actions.server.dto.*;
import org.gridsuite.actions.server.dto.ContingencyList;
import org.gridsuite.actions.server.entities.FormContingencyListEntity;
import org.gridsuite.actions.server.entities.NumericalFilterEntity;
import org.gridsuite.actions.server.entities.IdBasedContingencyListEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.FormContingencyListRepository;
import org.gridsuite.actions.server.repositories.IdBasedContingencyListRepository;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
public class ContingencyListService {

    private ScriptContingencyListRepository scriptContingencyListRepository;

    private FormContingencyListRepository formContingencyListRepository;

    private IdBasedContingencyListRepository idBasedContingencyListRepository;

    private NetworkStoreService networkStoreService;

    private NotificationService notificationService;

    private final FormToGroovyScript formToScript = new FormToGroovyScript();

    // Self injection for @transactional support in internal calls to other methods of this service
    @Autowired
    ContingencyListService self;

    public ContingencyListService(ScriptContingencyListRepository scriptContingencyListRepository,
                                  FormContingencyListRepository formContingencyListRepository,
                                  IdBasedContingencyListRepository idBasedContingencyListRepository,
                                  NetworkStoreService networkStoreService,
                                  NotificationService notificationService) {
        this.scriptContingencyListRepository = scriptContingencyListRepository;
        this.formContingencyListRepository = formContingencyListRepository;
        this.idBasedContingencyListRepository = idBasedContingencyListRepository;
        this.networkStoreService = networkStoreService;
        this.notificationService = notificationService;
    }

    private static ScriptContingencyList fromScriptContingencyListEntity(ScriptContingencyListEntity entity) {
        return new ScriptContingencyList(entity.getId(), entity.getScript() != null ? entity.getScript() : "");
    }

    private static FormContingencyList fromFormContingencyListEntity(FormContingencyListEntity entity) {
        return new FormContingencyList(entity.getId(), entity.getEquipmentType(), NumericalFilterEntity.convert(entity.getNominalVoltage1()), NumericalFilterEntity.convert(entity.getNominalVoltage2()), entity.getCountries1(), entity.getCountries2());
    }

    List<ScriptContingencyList> getScriptContingencyLists() {
        return scriptContingencyListRepository.findAll().stream().map(ContingencyListService::fromScriptContingencyListEntity).collect(Collectors.toList());
    }

    List<ContingencyListAttributes> getContingencyLists() {
        return Stream.concat(
            scriptContingencyListRepository.findAll().stream().map(scriptContingencyListEntity ->
                new ContingencyListAttributes(scriptContingencyListEntity.getId(), ContingencyListType.SCRIPT, scriptContingencyListEntity.getModificationDate())),
            formContingencyListRepository.findAll().stream().map(formContingencyListEntity ->
                new ContingencyListAttributes(formContingencyListEntity.getId(), ContingencyListType.FORM, formContingencyListEntity.getModificationDate()))
        ).collect(Collectors.toList());
    }

    List<ContingencyListAttributes> getContingencyLists(List<UUID> ids) {
        return Stream.of(
            scriptContingencyListRepository.findAllById(ids).stream().map(scriptContingencyListEntity ->
                new ContingencyListAttributes(scriptContingencyListEntity.getId(), ContingencyListType.SCRIPT, scriptContingencyListEntity.getModificationDate())),
            formContingencyListRepository.findAllById(ids).stream().map(formContingencyListEntity ->
                new ContingencyListAttributes(formContingencyListEntity.getId(), ContingencyListType.FORM, formContingencyListEntity.getModificationDate())),
            idBasedContingencyListRepository.findAllById(ids).stream().map(formContingencyListEntity ->
                new ContingencyListAttributes(formContingencyListEntity.getId(), ContingencyListType.IDENTIFIERS, formContingencyListEntity.getModificationDate()))
        ).flatMap(Function.identity()).collect(Collectors.toList());
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
            int ignoreSize = entity.getCountries1().size();
            return entity;
        });
    }

    public Optional<FormContingencyList> getFormContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return self.doGetFormContingencyListWithPreFetchedCountries(id).map(ContingencyListService::fromFormContingencyListEntity);
    }

    public Optional<IdBasedContingencyList> getIdBasedContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return idBasedContingencyListRepository.findById(id).map(ContingencyListService::fromIdBasedContingencyListEntity);
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
        } else if (contingencyList instanceof IdBasedContingencyList) {
            IdBasedContingencyList idBasedContingencyList = (IdBasedContingencyList) contingencyList;
            return getContingencies(idBasedContingencyList, network);
        } else {
            throw new PowsyblException("Contingency list implementation not yet supported: " + contingencyList.getClass().getSimpleName());
        }
    }

    private boolean countryFilter(Terminal terminal, Set<String> countries) {
        Optional<Country> country = terminal.getVoltageLevel().getSubstation().flatMap(Substation::getCountry);
        return CollectionUtils.isEmpty(countries) || country.map(c -> countries.contains(c.name())).orElse(false);
    }

    private boolean filterByCountries(Terminal terminal1, Terminal terminal2, Set<String> filter1, Set<String> filter2) {
        return
            // terminal 1 matches filter 1 and terminal 2 matches filter 2
            countryFilter(terminal1, filter1) &&
            countryFilter(terminal2, filter2)
            || // or the opposite
            countryFilter(terminal1, filter2) &&
            countryFilter(terminal2, filter1);
    }

    private boolean filterByCountries(Branch<?> branch, FormContingencyList filter) {
        return filterByCountries(branch.getTerminal1(), branch.getTerminal2(), filter.getCountries1(), filter.getCountries2());
    }

    private boolean filterByCountries(HvdcLine line, FormContingencyList filter) {
        return filterByCountries(line.getConverterStation1().getTerminal(), line.getConverterStation2().getTerminal(), filter.getCountries1(), filter.getCountries2());
    }

    private boolean filterByVoltage(Terminal terminal, NumericalFilter numericalFilter) {
        return filterByVoltage(terminal.getVoltageLevel().getNominalV(), numericalFilter);
    }

    private boolean filterByVoltages(Branch<?> branch, NumericalFilter numFilter1, NumericalFilter numFilter2) {
        return
            // terminal 1 matches filter 1 and terminal 2 matches filter 2
            filterByVoltage(branch.getTerminal1(), numFilter1) &&
            filterByVoltage(branch.getTerminal2(), numFilter2)
            || // or the opposite
            filterByVoltage(branch.getTerminal1(), numFilter2) &&
            filterByVoltage(branch.getTerminal2(), numFilter1);
    }

    private <I extends Injection<I>> Stream<Injection<I>> getInjectionContingencyList(Stream<Injection<I>> stream, FormContingencyList formContingencyList) {
        return stream
            .filter(injection -> filterByVoltage(injection.getTerminal().getVoltageLevel().getNominalV(), formContingencyList.getNominalVoltage1()))
            .filter(injection -> countryFilter(injection.getTerminal(), formContingencyList.getCountries1()));
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

    private List<Contingency> getLineContingencyList(Network network, FormContingencyList formContingencyList) {
        return network.getLineStream()
                .filter(line -> filterByVoltages(line, formContingencyList.getNominalVoltage1(), formContingencyList.getNominalVoltage2()))
                .filter(line -> filterByCountries(line, formContingencyList))
                .map(line -> new Contingency(line.getId(), Collections.singletonList(new BranchContingency(line.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> get2WTransformerContingencyList(Network network, FormContingencyList formContingencyList) {
        return network.getTwoWindingsTransformerStream()
                .filter(transformer -> filterByVoltages(transformer, formContingencyList.getNominalVoltage1(), formContingencyList.getNominalVoltage2()))
                .filter(transformer -> filterByCountries(transformer, formContingencyList))
                .map(transformer -> new Contingency(transformer.getId(), Collections.singletonList(new BranchContingency(transformer.getId()))))
                .collect(Collectors.toList());
    }

    private List<Contingency> getHvdcContingencyList(Network network, FormContingencyList formContingencyList) {
        return network.getHvdcLineStream()
            .filter(hvdcLine -> filterByVoltage(hvdcLine.getNominalV(), formContingencyList.getNominalVoltage1()))
            .filter(hvdcLine -> filterByCountries(hvdcLine, formContingencyList))
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

    private List<Contingency> getContingencies(IdBasedContingencyList idBasedContingencyList, Network network) {
        return idBasedContingencyList.getIdentifierContingencyList().getContingencies(network);
    }

    private boolean filterByVoltage(double equipmentNominalVoltage, NumericalFilter numericalFilter) {
        if (numericalFilter == null) {
            return true;
        }
        switch (numericalFilter.getType()) {
            case EQUALITY:
                return equipmentNominalVoltage == numericalFilter.getValue1();
            case LESS_THAN:
                return equipmentNominalVoltage < numericalFilter.getValue1();
            case LESS_OR_EQUAL:
                return equipmentNominalVoltage <= numericalFilter.getValue1();
            case GREATER_THAN:
                return equipmentNominalVoltage > numericalFilter.getValue1();
            case GREATER_OR_EQUAL:
                return equipmentNominalVoltage >= numericalFilter.getValue1();
            case RANGE:
                return equipmentNominalVoltage >= numericalFilter.getValue1() && equipmentNominalVoltage <= numericalFilter.getValue2();
            default:
                throw new PowsyblException("Unknown nominal voltage operator");
        }
    }

    Optional<List<Contingency>> exportContingencyList(UUID id, UUID networkUuid, String variantId) {
        Objects.requireNonNull(id);

        return getScriptContingencyList(id).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid, variantId))
            .or(() -> getFormContingencyList(id).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid, variantId)))
                    .or(() -> getIdBasedContingencyList(id).map(contingencyList -> toPowSyBlContingencyList(contingencyList, networkUuid, variantId)));
    }

    ScriptContingencyList createScriptContingencyList(UUID id, ScriptContingencyList script) {
        ScriptContingencyListEntity entity = new ScriptContingencyListEntity(script);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromScriptContingencyListEntity(scriptContingencyListRepository.save(entity));
    }

    Optional<ScriptContingencyList> createScriptContingencyList(UUID sourceListId, UUID id) {
        ScriptContingencyList sourceScriptContingencyList = getScriptContingencyList(sourceListId).orElse(null);
        if (sourceScriptContingencyList != null) {
            ScriptContingencyListEntity entity = new ScriptContingencyListEntity(sourceScriptContingencyList);
            entity.setId(id == null ? UUID.randomUUID() : id);
            return Optional.of(fromScriptContingencyListEntity(scriptContingencyListRepository.save(entity)));
        }
        return Optional.empty();
    }

    void modifyScriptContingencyList(UUID id, ScriptContingencyList script, String userId) {
        scriptContingencyListRepository.save(scriptContingencyListRepository.getOne(id).update(script));
        notificationService.emitElementUpdated(id, userId);
    }

    public FormContingencyList createFormContingencyList(UUID id, FormContingencyList formContingencyList) {
        FormContingencyListEntity entity = new FormContingencyListEntity(formContingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromFormContingencyListEntity(formContingencyListRepository.save(entity));
    }

    public Optional<FormContingencyList> createFormContingencyList(UUID sourceListId, UUID id) {
        FormContingencyList sourceFormContingencyList = getFormContingencyList(sourceListId).orElse(null);
        if (sourceFormContingencyList != null) {
            FormContingencyListEntity entity = new FormContingencyListEntity(sourceFormContingencyList);
            entity.setId(id == null ? UUID.randomUUID() : id);
            return Optional.of(fromFormContingencyListEntity(formContingencyListRepository.save(entity)));
        }
        return Optional.empty();
    }

    public Optional<IdBasedContingencyList> createIdBasedContingencyList(UUID sourceListId, UUID id) {
        IdBasedContingencyList sourceIdBasedContingencyList = getIdBasedContingencyList(sourceListId).orElse(null);
        if (sourceIdBasedContingencyList != null) {
            IdBasedContingencyListEntity entity = new IdBasedContingencyListEntity(sourceIdBasedContingencyList.getIdentifierContingencyList());
            entity.setId(id == null ? UUID.randomUUID() : id);
            return Optional.of(fromIdBasedContingencyListEntity(idBasedContingencyListRepository.save(entity)));
        }
        return Optional.empty();
    }

    public void modifyFormContingencyList(UUID id, FormContingencyList formContingencyList, String userId) {
        // throw if not found
        formContingencyListRepository.save(formContingencyListRepository.getOne(id).update(formContingencyList));
        notificationService.emitElementUpdated(id, userId);
    }

    @Transactional
    public void deleteContingencyList(UUID id) {
        Objects.requireNonNull(id);
        // if there is no form contingency list by this Id, deleted count == 0
        if (formContingencyListRepository.deleteFormContingencyListEntityById(id) == 0) {
            if (idBasedContingencyListRepository.deleteIdBasedContingencyListEntityById(id) == 0) {
                scriptContingencyListRepository.deleteById(id);
            }
        }
    }

    private String generateGroovyScriptFromForm(FormContingencyList formContingencyList) {
        return formToScript.generateGroovyScriptFromForm(formContingencyList);
    }

    @Transactional
    public ScriptContingencyList replaceFormContingencyListWithScript(UUID id, String userId) {
        Objects.requireNonNull(id);
        Optional<FormContingencyListEntity> formContingencyList = self.doGetFormContingencyListWithPreFetchedCountries(id);
        ScriptContingencyList result = formContingencyList.map(entity -> {
            String script = generateGroovyScriptFromForm(fromFormContingencyListEntity(entity));
            var scriptContingencyListEntity = new ScriptContingencyListEntity(new ScriptContingencyList(id, script));
            scriptContingencyListEntity.setId(id);
            var res = fromScriptContingencyListEntity(scriptContingencyListRepository.save(scriptContingencyListEntity));
            formContingencyListRepository.deleteById(id);
            return res;
        }).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        });
        notificationService.emitElementUpdated(id, userId);
        return result;
    }

    @Transactional
    public ScriptContingencyList newScriptFromFormContingencyList(UUID id, UUID newId) {
        Objects.requireNonNull(id);
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

    private static IdBasedContingencyList fromIdBasedContingencyListEntity(IdBasedContingencyListEntity entity) {
        List<NetworkElementIdentifier> listOfNetworkElementIdentifierList = new ArrayList<>();
        entity.getIdentifiersListEntities().forEach(identifierList -> {
            List<NetworkElementIdentifier> networkElementIdentifiers = new ArrayList<>();
            identifierList.getEquipmentIds().forEach(equipmentId -> networkElementIdentifiers.add(new IdBasedNetworkElementIdentifier(equipmentId)));
            //TODO: NetworkElementIdentifierList name will have to be filled with identifierList.getName() when it's available in powsybl-core
            listOfNetworkElementIdentifierList.add(new NetworkElementIdentifierList(networkElementIdentifiers));
        });
        //TODO: identifiableType to be removed
        return new IdBasedContingencyList(entity.getId(), new IdentifierContingencyList(entity.getName(), "LINE", listOfNetworkElementIdentifierList));
    }

    public IdBasedContingencyList createIdBasedContingencyList(UUID id, IdentifierContingencyList identifierContingencyList) {
        IdBasedContingencyListEntity entity = new IdBasedContingencyListEntity(identifierContingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromIdBasedContingencyListEntity(idBasedContingencyListRepository.save(entity));
    }

}
