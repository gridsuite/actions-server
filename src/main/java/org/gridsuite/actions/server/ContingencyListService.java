/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.*;
import com.powsybl.contingency.contingency.list.*;
import com.powsybl.contingency.contingency.list.identifier.IdBasedNetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifierList;
import com.powsybl.iidm.network.*;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.gridsuite.actions.server.dto.*;
import org.gridsuite.actions.server.entities.*;
import org.gridsuite.actions.server.repositories.FormContingencyListRepository;
import org.gridsuite.actions.server.repositories.IdBasedContingencyListRepository;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.actions.server.utils.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.EmptyResultDataAccessException;
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
        return new ScriptContingencyList(entity.getId(), entity.getModificationDate(), entity.getScript() != null ? entity.getScript() : "");
    }

    private static FormContingencyList fromFormContingencyListEntity(FormContingencyListEntity entity) {
        return new FormContingencyList(entity.getId(), entity.getModificationDate(), entity.getEquipmentType(), NumericalFilterEntity.convert(entity.getNominalVoltage()), NumericalFilterEntity.convert(entity.getNominalVoltage1()), NumericalFilterEntity.convert(entity.getNominalVoltage2()), entity.getCountries(), entity.getCountries1(), entity.getCountries2());
    }

    List<PersistentContingencyList> getScriptContingencyLists() {
        return scriptContingencyListRepository.findAll().stream().map(ContingencyListService::fromScriptContingencyListEntity).collect(Collectors.toList());
    }

    ContingencyListMetadata fromContingencyListEntity(AbstractContingencyEntity entity, ContingencyListType type) {
        return new ContingencyListMetadataImpl(entity.getId(), type, entity.getModificationDate());
    }

    List<ContingencyListMetadata> getContingencyListsMetadata() {
        return Stream.of(
            scriptContingencyListRepository.findAll().stream().map(scriptContingencyListEntity ->
                    fromContingencyListEntity(scriptContingencyListEntity, ContingencyListType.SCRIPT)),
            formContingencyListRepository.findAll().stream().map(formContingencyListEntity ->
                    fromContingencyListEntity(formContingencyListEntity, ContingencyListType.FORM)),
            idBasedContingencyListRepository.findAll().stream().map(idBasedContingencyListEntity ->
                    fromContingencyListEntity(idBasedContingencyListEntity, ContingencyListType.IDENTIFIERS))
        ).flatMap(Function.identity()).collect(Collectors.toList());
    }

    List<ContingencyListMetadata> getContingencyListsMetadata(List<UUID> ids) {
        return Stream.of(
            scriptContingencyListRepository.findAllById(ids).stream().map(scriptContingencyListEntity ->
                    fromContingencyListEntity(scriptContingencyListEntity, ContingencyListType.SCRIPT)),
            formContingencyListRepository.findAllById(ids).stream().map(formContingencyListEntity ->
                    fromContingencyListEntity(formContingencyListEntity, ContingencyListType.FORM)),
            idBasedContingencyListRepository.findAllById(ids).stream().map(idBasedContingencyListEntity ->
                    fromContingencyListEntity(idBasedContingencyListEntity, ContingencyListType.IDENTIFIERS))
        ).flatMap(Function.identity()).collect(Collectors.toList());
    }

    List<PersistentContingencyList> getFormContingencyLists() {
        return formContingencyListRepository.findAllWithCountries().stream().map(ContingencyListService::fromFormContingencyListEntity).collect(Collectors.toList());
    }

    Optional<PersistentContingencyList> getScriptContingencyList(UUID id) {
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

    public Optional<PersistentContingencyList> getFormContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return self.doGetFormContingencyListWithPreFetchedCountries(id).map(ContingencyListService::fromFormContingencyListEntity);
    }

    public Optional<PersistentContingencyList> getIdBasedContingencyList(UUID id, Network network) {
        Objects.requireNonNull(id);
        return idBasedContingencyListRepository.findById(id).map(idBasedContingencyListEntity -> fromIdBasedContingencyListEntity(idBasedContingencyListEntity, network));
    }

    private List<Contingency> getPowsyblContingencies(PersistentContingencyList contingencyList, Network network) {
        ContingencyList powsyblContingencyList = contingencyList.toPowsyblContingencyList(network);
        return powsyblContingencyList == null ? Collections.emptyList() : powsyblContingencyList.getContingencies(network);
    }

    List<ContingencyInfos> exportContingencyList(UUID id, UUID networkUuid, String variantId) {
        Objects.requireNonNull(id);
        Network network = getNetworkFromUuid(networkUuid, variantId);
        Optional<PersistentContingencyList> optionalPersistentContingencyList = getScriptContingencyList(id)
                .or(() -> getFormContingencyList(id))
                .or(() -> getIdBasedContingencyList(id, network));

        if (optionalPersistentContingencyList.isEmpty()) {
            return null;
        }

        PersistentContingencyList persistentContingencyList = optionalPersistentContingencyList.get();
        List<Contingency> contingencies = getPowsyblContingencies(persistentContingencyList, network);

        List<ContingencyInfos> contingencyInfos = new ArrayList<>();
        Map<String, Set<String>> notFoundElements = persistentContingencyList.getNotFoundElements(network);

        // we add all the contingencies that have only wrong ids
        notFoundElements.entrySet().stream()
                .filter(stringSetEntry -> contingencies.stream().noneMatch(c -> c.getId().equals(stringSetEntry.getKey())))
                .map(stringSetEntry -> new ContingencyInfos(stringSetEntry.getKey(), null, stringSetEntry.getValue()))
                .forEach(contingencyInfos::add);

        contingencies.stream()
                .map(contingency -> new ContingencyInfos(contingency, notFoundElements.get(contingency.getId())))
                .forEach(contingencyInfos::add);

        return contingencyInfos;
    }

    private Network getNetworkFromUuid(UUID networkUuid, String variantId) {
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

        return network;
    }

    ScriptContingencyList createScriptContingencyList(UUID id, ScriptContingencyList script) {
        ScriptContingencyListEntity entity = new ScriptContingencyListEntity(script);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromScriptContingencyListEntity(scriptContingencyListRepository.save(entity));
    }

    Optional<UUID> duplicateScriptContingencyList(UUID sourceListId) {
        Optional<ScriptContingencyList> scriptContingencyList = getScriptContingencyList(sourceListId).map(s -> createScriptContingencyList(null, (ScriptContingencyList) s));
        if (!scriptContingencyList.isPresent()) {
            ExceptionUtils.throwNotFound(sourceListId.toString(), "Script contingency list");
        } else {
            return Optional.of(scriptContingencyList.get().getId());
        }
        return Optional.empty();
    }

    void modifyScriptContingencyList(UUID id, ScriptContingencyList script, String userId) {
        scriptContingencyListRepository.save(scriptContingencyListRepository.getReferenceById(id).update(script));
        notificationService.emitElementUpdated(id, userId);
    }

    public FormContingencyList createFormContingencyList(UUID id, FormContingencyList formContingencyList) {
        FormContingencyListEntity entity = new FormContingencyListEntity(formContingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromFormContingencyListEntity(formContingencyListRepository.save(entity));
    }

    public Optional<UUID> duplicateFormContingencyList(UUID sourceListId) {
        Optional<FormContingencyList> formContingencyList = getFormContingencyList(sourceListId).map(s -> createFormContingencyList(null, (FormContingencyList) s));
        if (!formContingencyList.isPresent()) {
            ExceptionUtils.throwNotFound(sourceListId.toString(), "Form contingency list");
        } else {
            return Optional.of(formContingencyList.get().getId());
        }
        return Optional.empty();
    }

    public Optional<UUID> duplicateIdentifierContingencyList(UUID sourceListId) {
        Optional<IdBasedContingencyList> idBasedContingencyList = getIdBasedContingencyList(sourceListId, null).map(s -> createIdBasedContingencyList(null, (IdBasedContingencyList) s));
        if (!idBasedContingencyList.isPresent()) {
            ExceptionUtils.throwNotFound(sourceListId.toString(), "Identifier contingency list");
        } else {
            return Optional.of(idBasedContingencyList.get().getId());
        }
        return Optional.empty();
    }

    public void modifyFormContingencyList(UUID id, FormContingencyList formContingencyList, String userId) {
        // throw if not found
        formContingencyListRepository.save(formContingencyListRepository.getReferenceById(id).update(formContingencyList));
        notificationService.emitElementUpdated(id, userId);
    }

    public void modifyIdBasedContingencyList(UUID id, IdBasedContingencyList idBasedContingencyList, String userId) {
        // throw if not found
        idBasedContingencyListRepository.save(idBasedContingencyListRepository.getReferenceById(id).update(idBasedContingencyList));
        notificationService.emitElementUpdated(id, userId);
    }

    @Transactional
    public void deleteContingencyList(UUID id) throws EmptyResultDataAccessException {
        Objects.requireNonNull(id);
        // if there is no form contingency list by this Id, deleted count == 0
        if (formContingencyListRepository.deleteFormContingencyListEntityById(id) == 0) {
            if (idBasedContingencyListRepository.deleteIdBasedContingencyListEntityById(id) == 0) {
                if (scriptContingencyListRepository.deleteScriptContingencyListById(id) == 0) {
                    throw new EmptyResultDataAccessException("No element found", 1);
                }
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
            var scriptContingencyListEntity = new ScriptContingencyListEntity(new ScriptContingencyList(id, null, script));
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
            ScriptContingencyListEntity scriptEntity = new ScriptContingencyListEntity(new ScriptContingencyList(script));
            scriptEntity.setId(newId == null ? UUID.randomUUID() : newId);
            return fromScriptContingencyListEntity(scriptContingencyListRepository.save(scriptEntity));
        }).orElseThrow(() -> {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found");
        });
    }

    private static IdBasedContingencyList fromIdBasedContingencyListEntity(IdBasedContingencyListEntity entity, Network network) {
        List<NetworkElementIdentifier> listOfNetworkElementIdentifierList = new ArrayList<>();
        Map<String, Set<String>> notFoundElements = new HashMap<>();
        entity.getIdentifiersListEntities().forEach(identifierList -> {
            List<NetworkElementIdentifier> networkElementIdentifiers = new ArrayList<>();
            identifierList.getEquipmentIds().forEach(equipmentId -> {
                if (network != null && network.getIdentifiable(equipmentId) == null) {
                    Set<String> ids = notFoundElements.computeIfAbsent(identifierList.getName(), k -> new HashSet<>());
                    ids.add(equipmentId);
                }
                networkElementIdentifiers.add(new IdBasedNetworkElementIdentifier(equipmentId));
            });
            listOfNetworkElementIdentifierList.add(new NetworkElementIdentifierList(networkElementIdentifiers, identifierList.getName()));
        });
        return new IdBasedContingencyList(entity.getId(),
                entity.getModificationDate(),
                new IdentifierContingencyList(entity.getId().toString(), listOfNetworkElementIdentifierList),
                notFoundElements);
    }

    public IdBasedContingencyList createIdBasedContingencyList(UUID id, IdBasedContingencyList idBasedContingencyList) {
        IdBasedContingencyListEntity entity = new IdBasedContingencyListEntity(idBasedContingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromIdBasedContingencyListEntity(idBasedContingencyListRepository.save(entity), null);
    }

}
