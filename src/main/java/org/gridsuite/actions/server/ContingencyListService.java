/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.BatteryContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.GeneratorContingency;
import com.powsybl.contingency.HvdcLineContingency;
import com.powsybl.contingency.LineContingency;
import com.powsybl.contingency.LoadContingency;
import com.powsybl.contingency.StaticVarCompensatorContingency;
import com.powsybl.contingency.TwoWindingsTransformerContingency;
import com.powsybl.contingency.contingency.list.ContingencyList;
import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.identifiers.IdBasedNetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifierContingencyList;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.gridsuite.actions.server.dto.*;
import org.gridsuite.actions.server.entities.*;
import org.gridsuite.actions.server.repositories.FilterBasedContingencyListRepository;
import org.gridsuite.actions.server.repositories.FormContingencyListRepository;
import org.gridsuite.actions.server.repositories.IdBasedContingencyListRepository;
import org.gridsuite.actions.server.service.FilterService;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.filter.identifierlistfilter.IdentifiableAttributes;
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

    private final FormContingencyListRepository formContingencyListRepository;

    private final IdBasedContingencyListRepository idBasedContingencyListRepository;

    private final FilterBasedContingencyListRepository filterBasedContingencyListRepository;

    private final NetworkStoreService networkStoreService;

    private final NotificationService notificationService;

    private final FilterService filterService;

    public ContingencyListService(FormContingencyListRepository formContingencyListRepository,
                                  IdBasedContingencyListRepository idBasedContingencyListRepository,
                                  FilterBasedContingencyListRepository filterBasedContingencyListRepository,
                                  NetworkStoreService networkStoreService,
                                  NotificationService notificationService,
                                  FilterService filterService) {
        this.formContingencyListRepository = formContingencyListRepository;
        this.idBasedContingencyListRepository = idBasedContingencyListRepository;
        this.filterBasedContingencyListRepository = filterBasedContingencyListRepository;
        this.networkStoreService = networkStoreService;
        this.notificationService = notificationService;
        this.filterService = filterService;
    }

    private static FormContingencyList fromFormContingencyListEntity(FormContingencyListEntity entity) {
        return new FormContingencyList(entity.getId(), entity.getModificationDate(), entity.getEquipmentType(), NumericalFilterEntity.convert(entity.getNominalVoltage()), NumericalFilterEntity.convert(entity.getNominalVoltage1()), NumericalFilterEntity.convert(entity.getNominalVoltage2()),
            entity.getCountries() == null ? entity.getCountries() : Set.copyOf(entity.getCountries()),
            entity.getCountries1() == null ? entity.getCountries1() : Set.copyOf(entity.getCountries1()),
            entity.getCountries2() == null ? entity.getCountries2() : Set.copyOf(entity.getCountries2()));
    }

    ContingencyListMetadata fromContingencyListEntity(AbstractContingencyEntity entity, ContingencyListType type) {
        return new ContingencyListMetadataImpl(entity.getId(), type, entity.getModificationDate());
    }

    List<ContingencyListMetadata> getContingencyListsMetadata() {
        return Stream.of(
            formContingencyListRepository.findAll().stream().map(formContingencyListEntity ->
                    fromContingencyListEntity(formContingencyListEntity, ContingencyListType.FORM)),
            idBasedContingencyListRepository.findAll().stream().map(idBasedContingencyListEntity ->
                    fromContingencyListEntity(idBasedContingencyListEntity, ContingencyListType.IDENTIFIERS)),
            filterBasedContingencyListRepository.findAll().stream().map(filterBasedContingencyListEntity ->
                    fromContingencyListEntity(filterBasedContingencyListEntity, ContingencyListType.FILTERS))
        ).flatMap(Function.identity()).collect(Collectors.toList());
    }

    List<ContingencyListMetadata> getContingencyListsMetadata(List<UUID> ids) {
        return Stream.of(
            formContingencyListRepository.findAllById(ids).stream().map(formContingencyListEntity ->
                    fromContingencyListEntity(formContingencyListEntity, ContingencyListType.FORM)),
            idBasedContingencyListRepository.findAllById(ids).stream().map(idBasedContingencyListEntity ->
                    fromContingencyListEntity(idBasedContingencyListEntity, ContingencyListType.IDENTIFIERS)),
            filterBasedContingencyListRepository.findAllById(ids).stream().map(filterBasedContingencyListEntity ->
                fromContingencyListEntity(filterBasedContingencyListEntity, ContingencyListType.FILTERS))
        ).flatMap(Function.identity()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PersistentContingencyList> getFormContingencyLists() {
        return formContingencyListRepository.findAllWithCountries().stream().map(ContingencyListService::fromFormContingencyListEntity).collect(Collectors.toList());
    }

    private Optional<FormContingencyListEntity> doGetFormContingencyListWithPreFetchedCountries(UUID name) {
        return formContingencyListRepository.findById(name).map(entity -> {
            @SuppressWarnings("unused")
            int ignoreSize = entity.getCountries1().size();
            return entity;
        });
    }

    public List<IdentifiableAttributes> evaluateFiltersNetwork(UUID networkUuid, String variantUuid, List<UUID> filtersUuid) {
        return filterService.evaluateFilters(networkUuid, variantUuid, filtersUuid);
    }

    @Transactional(readOnly = true)
    public Optional<PersistentContingencyList> getFormContingencyList(UUID id) {
        return doGetFormContingencyList(id);
    }

    private Optional<PersistentContingencyList> doGetFormContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return doGetFormContingencyListWithPreFetchedCountries(id).map(ContingencyListService::fromFormContingencyListEntity);
    }

    @Transactional(readOnly = true)
    public Optional<PersistentContingencyList> getIdBasedContingencyList(UUID id, Network network) {
        return doGetIdBasedContingencyList(id, network);
    }

    private Optional<PersistentContingencyList> doGetIdBasedContingencyList(UUID id, Network network) {
        Objects.requireNonNull(id);
        return idBasedContingencyListRepository.findById(id).map(idBasedContingencyListEntity -> fromIdBasedContingencyListEntity(idBasedContingencyListEntity, network));
    }

    @Transactional(readOnly = true)
    public Optional<PersistentContingencyList> getFilterBasedContingencyList(UUID id) {
        return doGetFilterBasedContingencyList(id);
    }

    private Optional<PersistentContingencyList> doGetFilterBasedContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return filterBasedContingencyListRepository.findById(id).map(ContingencyListService::fromFilterBasedContingencyListEntity);
    }

    private static ContingencyElement toContingencyElement(IdentifiableAttributes id) {
        switch (id.getType()) {
            case LINE -> {
                return new LineContingency(id.getId());
            }
            case BATTERY -> {
                return new BatteryContingency(id.getId());
            }
            case LOAD -> {
                return new LoadContingency(id.getId());
            }
            case GENERATOR -> {
                return new GeneratorContingency(id.getId());
            }
            case TWO_WINDINGS_TRANSFORMER -> {
                return new TwoWindingsTransformerContingency(id.getId());
            }
            case HVDC_LINE -> {
                return new HvdcLineContingency(id.getId());
            }
            case STATIC_VAR_COMPENSATOR -> {
                return new StaticVarCompensatorContingency(id.getId());
            }
            default -> throw new IllegalStateException("Unexpected value: " + id.getType());
        }
    }

    private List<Contingency> getPowsyblContingencies(PersistentContingencyList contingencyList, Network network, UUID networkUuid, String variantUuid) {
        ContingencyList powsyblContingencyList;
        if (Objects.requireNonNull(contingencyList.getMetadata().getType()) == ContingencyListType.FILTERS) {
            FilterBasedContingencyList filterBasedContingencyList = (FilterBasedContingencyList) contingencyList;
            List<IdentifiableAttributes> identifiers = evaluateFiltersNetwork(networkUuid, variantUuid,
                filterBasedContingencyList.getFilters().stream().map(FilterMetaData::getId).toList());
            powsyblContingencyList = ContingencyList.of(identifiers.stream()
                .map(id ->
                    new Contingency(id.getId(), List.of(toContingencyElement(id))))
                .toArray(Contingency[]::new)
            );
        } else {
            powsyblContingencyList = contingencyList.toPowsyblContingencyList(network);
        }

        return powsyblContingencyList == null ? Collections.emptyList() : powsyblContingencyList.getContingencies(network);
    }

    @Transactional(readOnly = true)
    public Integer getContingencyCount(List<UUID> ids, UUID networkUuid, String variantId) {
        Network network = getNetworkFromUuid(networkUuid, variantId);
        return ids.stream()
            .map(uuid -> {
                Optional<PersistentContingencyList> contingencyList = getAnyContingencyList(uuid, network);
                return contingencyList.map(l -> getContingencies(l, network, networkUuid, variantId).size()).orElse(0);
            })
            .reduce(0, Integer::sum);
    }

    @Transactional(readOnly = true)
    public ContingencyListExportResult exportContingencyList(List<UUID> contingencyListIds, UUID networkUuid, String variantId) {
        Network network = getNetworkFromUuid(networkUuid, variantId);
        List<Contingency> contingencies = new ArrayList<>();
        List<UUID> notFoundIds = new ArrayList<>();

        contingencyListIds.forEach(contingencyListId -> {
            Optional<PersistentContingencyList> contingencyList = getAnyContingencyList(contingencyListId, network);
            contingencyList.ifPresentOrElse(
                    list -> contingencies.addAll(getContingencies(list, network, networkUuid, variantId)),
                    () -> notFoundIds.add(contingencyListId)
            );
        });
        return new ContingencyListExportResult(contingencies, notFoundIds);
    }

    private List<Contingency> getContingencies(PersistentContingencyList persistentContingencyList, Network network, UUID networkUuid, String variantId) {
        return evaluateContingencyList(persistentContingencyList, network, networkUuid, variantId)
                .stream()
                .map(ContingencyInfos::getContingency)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContingencyInfos> exportContingencyInfosList(List<UUID> ids, UUID networkUuid, String variantId) {
        Network network = getNetworkFromUuid(networkUuid, variantId);
        return ids.stream().map(id -> evaluateContingencyList(findContingencyList(id, network), network, networkUuid, variantId)).flatMap(Collection::stream).toList();
    }

    private PersistentContingencyList findContingencyList(UUID id, Network network) {
        Objects.requireNonNull(id);
        return getAnyContingencyList(id, network)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found"));
    }

    private Optional<PersistentContingencyList> getAnyContingencyList(UUID id, Network network) {
        return doGetFormContingencyList(id)
                .or(() -> doGetIdBasedContingencyList(id, network).or(() -> doGetFilterBasedContingencyList(id)));
    }

    private List<ContingencyInfos> evaluateContingencyList(PersistentContingencyList persistentContingencyList, Network network, UUID networkUuid, String variantId) {
        List<Contingency> contingencies = getPowsyblContingencies(persistentContingencyList, network, networkUuid, variantId);
        Map<String, Set<String>> notFoundElements = persistentContingencyList.getNotFoundElements(network);

        // For a gridsuite contingency with all equipments not found the powsybl contingency is not created
        List<ContingencyInfos> contingencyInfos = new ArrayList<>();
        notFoundElements.entrySet().stream()
                .filter(stringSetEntry -> contingencies.stream().noneMatch(c -> c.getId().equals(stringSetEntry.getKey())))
                .map(stringSetEntry -> new ContingencyInfos(stringSetEntry.getKey(), null, stringSetEntry.getValue(), null))
                .forEach(contingencyInfos::add);

        contingencies.stream()
                .map(contingency -> new ContingencyInfos(contingency.getId(), contingency, notFoundElements.get(contingency.getId()), getDisconnectedElements(contingency, network)))
                .forEach(contingencyInfos::add);

        return contingencyInfos;
    }

    private Set<String> getDisconnectedElements(Contingency contingency, Network network) {
        return contingency.getElements().stream()
                .filter(contingencyElement -> {
                    var connectable = network.getConnectable(contingencyElement.getId());
                    return connectable != null && isDisconnected(connectable);
                })
                .map(ContingencyElement::getId)
                .collect(Collectors.toSet());
    }

    private boolean isDisconnected(Connectable<?> connectable) {
        List<? extends Terminal> terminals = connectable.getTerminals();
        // check if the connectable are connected with terminal.isConnected()
        boolean atleastOneIsConnected = false;
        for (Terminal terminal : terminals) {
            if (terminal != null && terminal.isConnected()) {
                atleastOneIsConnected = true;
                break;
            }
        }
        return !atleastOneIsConnected;
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

    @Transactional
    public FormContingencyList createFormContingencyList(UUID id, FormContingencyList formContingencyList) {
        return doCreateFormContingencyList(id, formContingencyList);
    }

    private FormContingencyList doCreateFormContingencyList(UUID id, FormContingencyList formContingencyList) {
        FormContingencyListEntity entity = new FormContingencyListEntity(formContingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromFormContingencyListEntity(formContingencyListRepository.save(entity));
    }

    @Transactional
    public Optional<UUID> duplicateFormContingencyList(UUID sourceListId) {
        Optional<FormContingencyList> formContingencyList = doGetFormContingencyList(sourceListId).map(s -> doCreateFormContingencyList(null, (FormContingencyList) s));
        if (!formContingencyList.isPresent()) {
            throw createNotFoundException(sourceListId.toString(), "Form contingency list");
        } else {
            return Optional.of(formContingencyList.get().getId());
        }
    }

    @Transactional
    public Optional<UUID> duplicateFilterBasedContingencyList(UUID sourceListId) {
        Optional<PersistentContingencyList> contingencyList = doGetFilterBasedContingencyList(sourceListId);
        if (contingencyList.isEmpty()) {
            throw createNotFoundException(sourceListId.toString(), "Form contingency list");
        } else {
            FilterBasedContingencyList filterContingencyList = createFilterBasedContingencyList(null, (FilterBasedContingencyList) contingencyList.get());
            return Optional.of(filterContingencyList.getId());
        }
    }

    @Transactional
    public Optional<UUID> duplicateIdentifierContingencyList(UUID sourceListId) {
        Optional<IdBasedContingencyList> idBasedContingencyList = doGetIdBasedContingencyList(sourceListId, null).map(s -> createIdBasedContingencyList(null, (IdBasedContingencyList) s));
        if (!idBasedContingencyList.isPresent()) {
            throw createNotFoundException(sourceListId.toString(), "Identifier contingency list");
        } else {
            return Optional.of(idBasedContingencyList.get().getId());
        }
    }

    @Transactional
    public void modifyFormContingencyList(UUID id, FormContingencyList formContingencyList, String userId) {
        // throw if not found
        formContingencyListRepository.save(formContingencyListRepository.getReferenceById(id).update(formContingencyList));
        notificationService.emitElementUpdated(id, userId);
    }

    @Transactional
    public void modifyIdBasedContingencyList(UUID id, IdBasedContingencyList idBasedContingencyList, String userId) {
        // throw if not found
        idBasedContingencyListRepository.save(idBasedContingencyListRepository.getReferenceById(id).update(idBasedContingencyList));
        notificationService.emitElementUpdated(id, userId);
    }

    @Transactional
    public void modifyFilterBasedContingencyList(UUID id, FilterBasedContingencyList contingencyList, String userId) {
        // throw if not found
        filterBasedContingencyListRepository.save(filterBasedContingencyListRepository.getReferenceById(id).update(contingencyList));
        notificationService.emitElementUpdated(id, userId);
    }

    @Transactional
    public void deleteContingencyList(UUID id) throws EmptyResultDataAccessException {
        Objects.requireNonNull(id);
        // if there is no form contingency list by this Id, deleted count == 0
        if (formContingencyListRepository.deleteFormContingencyListEntityById(id) == 0
            && idBasedContingencyListRepository.deleteIdBasedContingencyListEntityById(id) == 0
            && filterBasedContingencyListRepository.deleteFilterBasedContingencyListEntityById(id) == 0) {
            throw new EmptyResultDataAccessException("No element found", 1);
        }
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
            listOfNetworkElementIdentifierList.add(new NetworkElementIdentifierContingencyList(networkElementIdentifiers, identifierList.getName()));
        });
        return new IdBasedContingencyList(entity.getId(),
                entity.getModificationDate(),
                new IdentifierContingencyList(entity.getId().toString(), listOfNetworkElementIdentifierList),
                notFoundElements);
    }

    private static FilterBasedContingencyList fromFilterBasedContingencyListEntity(FilterBasedContingencyListEntity entity) {
        List<FilterMetaData> filterList = new ArrayList<>();
        entity.getFiltersListEntities().forEach(f -> filterList.add(new FilterMetaData(f.getFilterId(), f.getName(), f.getEquipmentType())));
        return new FilterBasedContingencyList(entity.getId(), entity.getModificationDate(), filterList);
    }

    public IdBasedContingencyList createIdBasedContingencyList(UUID id, IdBasedContingencyList idBasedContingencyList) {
        IdBasedContingencyListEntity entity = new IdBasedContingencyListEntity(idBasedContingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromIdBasedContingencyListEntity(idBasedContingencyListRepository.save(entity), null);
    }

    public FilterBasedContingencyList createFilterBasedContingencyList(UUID id, FilterBasedContingencyList contingencyList) {
        FilterBasedContingencyListEntity entity = new FilterBasedContingencyListEntity(contingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromFilterBasedContingencyListEntity(filterBasedContingencyListRepository.save(entity));
    }

    public ResponseStatusException createNotFoundException(String resourceId, String resourceType) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("%s %s not found", resourceType, resourceId));
    }

}
