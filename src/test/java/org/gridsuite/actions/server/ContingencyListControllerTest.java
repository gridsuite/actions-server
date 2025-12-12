/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.powsybl.contingency.*;
import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import com.powsybl.contingency.json.ContingencyJsonModule;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.identifiers.IdBasedNetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifierContingencyList;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.ShuntTestCaseFactory;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.gridsuite.actions.server.dto.*;
import org.gridsuite.actions.server.repositories.IdBasedContingencyListRepository;
import org.gridsuite.actions.server.service.FilterService;
import org.gridsuite.actions.server.utils.MatcherJson;
import org.gridsuite.filter.identifierlistfilter.FilteredIdentifiables;
import org.gridsuite.filter.identifierlistfilter.IdentifiableAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.Message;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.gridsuite.filter.utils.EquipmentType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@SpringBootTest(classes = {ActionsApplication.class, TestChannelBinderConfiguration.class})
@AutoConfigureMockMvc
class ContingencyListControllerTest {

    private static final long TIMEOUT = 1000;

    private static final UUID NETWORK_UUID = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
    private static final UUID NETWORK_UUID_2 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e5");
    private static final UUID NETWORK_UUID_3 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e6");
    private static final UUID NETWORK_UUID_4 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e7");
    private static final UUID NETWORK_UUID_5 = UUID.fromString("0313daa6-9419-4d4f-8ed1-af555998665f");
    private static final String VARIANT_ID_1 = "variant_1";
    private static final String VARIANT_ID_2 = "variant_2";
    private static final String USER_ID_HEADER = "userId";
    public static final String CONTINGENCY_1 = "contingency-1";
    public static final String CONTINGENCY_2 = "contingency-2";

    private final String elementUpdateDestination = "element.update";

    private Network network;

    @Autowired
    private IdBasedContingencyListRepository idBasedContingencyListRepository;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private NetworkStoreService networkStoreService;

    @Autowired
    private OutputDestination output;

    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;

    @MockitoSpyBean
    private FilterService filterService;

    @AfterEach
    void tearDown() {
        idBasedContingencyListRepository.deleteAll();

        List<String> destinations = List.of(elementUpdateDestination);
        assertQueuesEmptyThenClear(destinations, output);
        wireMockServer.stop();
    }

    private static void assertQueuesEmptyThenClear(List<String> destinations, OutputDestination output) {
        try {
            destinations.forEach(destination -> assertNull(output.receive(TIMEOUT, destination), "Should not be any messages in queue " + destination + " : "));
        } catch (NullPointerException e) {
            // Ignoring
        } finally {
            output.clear(); // purge in order to not fail the other tests
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MockitoAnnotations.initMocks(this);

        network = EurostagTutorialExample1Factory.createWithMoreGenerators(new NetworkFactoryImpl());
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_ID_1);
        network.getVariantManager().setWorkingVariant(VARIANT_ID_1);
        // remove generator 'GEN2' from network in variant VARIANT_ID_1
        network.getGenerator("GEN2").remove();
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_ID_2);
        network.getVariantManager().setWorkingVariant(VARIANT_ID_2);
        //disconnect a line NHV1_NHV2_1
        network.getConnectable("NHV1_NHV2_1").getTerminals().forEach(Terminal::disconnect);
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        Network network2 = HvdcTestNetwork.createVsc(new NetworkFactoryImpl());
        Network network3 = SvcTestCaseFactory.createWithMoreSVCs(new NetworkFactoryImpl());
        Network network4 = ShuntTestCaseFactory.create(new NetworkFactoryImpl());
        Network network5 = EurostagTutorialExample1Factory.createWithFixedCurrentLimits(new NetworkFactoryImpl());
        given(networkStoreService.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreService.getNetwork(NETWORK_UUID_2, PreloadingStrategy.COLLECTION)).willReturn(network2);
        given(networkStoreService.getNetwork(NETWORK_UUID_3, PreloadingStrategy.COLLECTION)).willReturn(network3);
        given(networkStoreService.getNetwork(NETWORK_UUID_4, PreloadingStrategy.COLLECTION)).willReturn(network4);
        given(networkStoreService.getNetwork(NETWORK_UUID_5, PreloadingStrategy.COLLECTION)).willReturn(network5);

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModule(new ContingencyJsonModule());

        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        // mock base url of filter server as one of wire mock server
        Mockito.doAnswer(invocation -> wireMockServer.baseUrl()).when(filterService).getBaseUri();
    }

    private String genFilterBasedContingencyList(List<UUID> uuids) throws JsonProcessingException {

        List<FilterAttributes> filtersAttributes = List.of(
            new FilterAttributes(uuids.get(0), LINE, "Filter1"),
            new FilterAttributes(uuids.get(1), SUBSTATION, "Filter2"),
            new FilterAttributes(uuids.get(2), TWO_WINDINGS_TRANSFORMER, "Filter3")
        );
        List<EquipmentTypesByFilter> equipmentTypesByFilter = List.of(
            new EquipmentTypesByFilter(uuids.get(1), Set.of(IdentifiableType.GENERATOR))
        );
        return "{\"filters\":" + objectMapper.writeValueAsString(filtersAttributes) +
            ", \"selectedEquipmentTypesByFilter\":" + objectMapper.writeValueAsString(equipmentTypesByFilter) + "}";
    }

    private String genModifiedFilterBasedContingencyList(List<UUID> uuids) throws JsonProcessingException {

        List<FilterAttributes> filtersAttributes = List.of(
            new FilterAttributes(uuids.get(0), LINE, "Filter1"),
            new FilterAttributes(uuids.get(2), TWO_WINDINGS_TRANSFORMER, "Filter3")
        );
        return "{\"filters\":" + objectMapper.writeValueAsString(filtersAttributes) + ", \"selectedEquipmentTypesByFilter\":[]}";
    }

    private UUID addNewFilterBasedContingencyList(String filters) throws Exception {

        String res = mvc.perform(post("/" + VERSION + "/filters-contingency-lists")
                .content(filters)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        FilterBasedContingencyList list = objectMapper.readValue(res, FilterBasedContingencyList.class);
        FilterBasedContingencyList original = objectMapper.readValue(filters, FilterBasedContingencyList.class);
        compareFilterBasedContingencyList(original, list);

        // mandatory function but useless for this contingency list tests to increase coverage
        assertNull(list.toPowsyblContingencyList(network));
        assertEquals(Map.of(), list.getNotFoundElements(network));

        return list.getId();
    }

    private static void compareFilterBasedContingencyList(FilterBasedContingencyList expected, FilterBasedContingencyList current) {
        compareFiltersMetaDataLists(expected.getFilters(), current.getFilters());
    }

    private static void compareFiltersMetaDataLists(List<FilterAttributes> expected, List<FilterAttributes> current) {
        assertEquals(expected.size(), current.size());

        current.forEach(filter -> {
            // find element in expected with same uuid
            Optional<FilterAttributes> expectedFilter = expected.stream().filter(f ->
                f.id().equals(filter.id())).findFirst();
            assertTrue(expectedFilter.isPresent());
        });
    }

    @Test
    void testExportContingenciesInfos() throws Exception {
        Instant date = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, date, "NHV1_NHV2_1", "Test");
        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        mvc.perform(get("/" + VERSION + "/contingency-lists/contingency-infos/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1 + "&ids=" + contingencyListId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"contingency\":{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]},\"notFoundElements\":null},{\"id\":\"Test\",\"contingency\":null,\"notFoundElements\":[\"Test\"]}]"));
        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());
    }

    @Test
    void testExportContingenciesNotConnectedAndNotFoundElements() throws Exception {
        NetworkElementIdentifierContingencyList networkElementIdentifierContingencyList = new NetworkElementIdentifierContingencyList(List.of(new IdBasedNetworkElementIdentifier("NHV1_NHV2_1"), new IdBasedNetworkElementIdentifier("NHV1_NHV2_2"), new IdBasedNetworkElementIdentifier("TEST1")), "default");
        IdBasedContingencyList idBasedContingencyList = new IdBasedContingencyList(null, Instant.now(), new IdentifierContingencyList("defaultName", List.of(networkElementIdentifierContingencyList)));

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        Contingency contingency = new Contingency("default", null, List.of(new LineContingency("NHV1_NHV2_2"), new LineContingency("NHV1_NHV2_1")));
        ContingencyInfos contingencyExpectedResult = new ContingencyInfos("default", contingency, Set.of("TEST1"), Set.of("NHV1_NHV2_1"));

        mvc.perform(get("/" + VERSION + "/contingency-lists/contingency-infos/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_2 + "&ids=" + contingencyListId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(contingencyExpectedResult))))
                .andReturn();

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());
    }

    private ContingencyListMetadataImpl getMetadata(UUID id) throws Exception {
        var res = mvc.perform(get("/" + VERSION + "/contingency-lists/metadata?ids=" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<ContingencyListMetadataImpl> contingencyListAttributes = objectMapper.readValue(res, new TypeReference<>() {
        });
        assertEquals(1, contingencyListAttributes.size());
        return contingencyListAttributes.get(0);
    }

    @Test
    void testCreateContingencyBadOperator() throws Exception {
        String lineFilters = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"BAD_OP\"," +
                "    \"value1\": \"100\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries1\": [\"FR\", \"BE\"]" +
                "}";

        mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(lineFilters)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    private static IdBasedContingencyList createIdBasedContingencyList(UUID listId, Instant modificationDate, String... identifiers) {
        List<NetworkElementIdentifier> networkElementIdentifiers = Arrays.stream(identifiers).map(id -> new NetworkElementIdentifierContingencyList(List.of(new IdBasedNetworkElementIdentifier(id)), id)).collect(Collectors.toList());
        return new IdBasedContingencyList(listId, modificationDate, new IdentifierContingencyList(listId != null ? listId.toString() : "defaultName", networkElementIdentifiers));
    }

    @Test
    void testFilterBasedContingencyList() throws Exception {

        List<UUID> filters = List.of(UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID());

        // create test
        String list = genFilterBasedContingencyList(filters);
        UUID id = addNewFilterBasedContingencyList(list);

        // test get
        MappingBuilder requestPatternBuilder = WireMock.get(WireMock.urlPathEqualTo("/v1/filters/infos"))
            .withHeader(USER_ID_HEADER, WireMock.equalTo(USER_ID_HEADER));

        for (UUID filter : filters) {
            requestPatternBuilder.withQueryParam("filterUuids", WireMock.equalTo(filter.toString()));
        }

        wireMockServer.stubFor(requestPatternBuilder.willReturn(WireMock.ok()));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + id)
                .header(USER_ID_HEADER, USER_ID_HEADER)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));

        // test count
        requestPatternBuilder = WireMock.post(WireMock.urlPathEqualTo("/v1/filters/evaluate/identifiables"))
            .withQueryParam("networkUuid", WireMock.equalTo(NETWORK_UUID.toString()))
            .withRequestBody(WireMock.containing(filters.get(0).toString())
                .and(WireMock.containing(filters.get(1).toString()))
                .and(WireMock.containing(filters.get(2).toString()))
                .and(WireMock.containing(IdentifiableType.GENERATOR.name())));

        wireMockServer.stubFor(requestPatternBuilder.willReturn(WireMock.ok()));

        String res = mvc.perform(get("/" + VERSION + "/contingency-lists/count?ids=" + id + "&networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertEquals(0, Integer.parseInt(res));

        // duplicate test
        String newUuid = mvc.perform(post("/" + VERSION + "/filters-contingency-lists?duplicateFrom=" + id))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNotNull(newUuid);

        mvc.perform(post("/" + VERSION + "/filters-contingency-lists?duplicateFrom=" + UUID.randomUUID()))
            .andExpect(status().isNotFound());

        // delete lists
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + id))
            .andExpect(status().isOk());

        newUuid = newUuid.replace("\"", "");
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + newUuid))
            .andExpect(status().isOk());
    }

    @Test
    void modifyFilterBasedContingencyList() throws Exception {

        List<UUID> filters = List.of(UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID());

        String contingencyList = genFilterBasedContingencyList(filters);

        String res = mvc.perform(post("/" + VERSION + "/filters-contingency-lists")
                .content(contingencyList)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, FilterBasedContingencyList.class).getId();

        String newList = genModifiedFilterBasedContingencyList(filters);
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/" + contingencyListId)
                .content(newList)
                .contentType(APPLICATION_JSON)
                .header(USER_ID_HEADER, USER_ID_HEADER))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Message<byte[]> message = output.receive(TIMEOUT, elementUpdateDestination);
        assertEquals(contingencyListId, message.getHeaders().get(NotificationService.HEADER_ELEMENT_UUID));
        assertEquals(USER_ID_HEADER, message.getHeaders().get(NotificationService.HEADER_MODIFIED_BY));

        // delete lists
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
            .andExpect(status().isOk());
    }

    private int getContingencyListsCount() throws Exception {
        String res = mvc.perform(get("/" + VERSION + "/contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        List<IdBasedContingencyList> contingencyListAttributes = objectMapper.readValue(res, new TypeReference<>() {
        });
        return contingencyListAttributes.size();
    }

    private static void matchContingencyListMetadata(ContingencyListMetadata metadata1, ContingencyListMetadata metadata2) {
        assertEquals(metadata1.getId(), metadata2.getId());
        assertEquals(metadata1.getType(), metadata2.getType());
        assertTrue((metadata1.getModificationDate().toEpochMilli() - metadata2.getModificationDate().toEpochMilli()) < 2000);
    }

    private void matchIdBasedContingencyList(IdBasedContingencyList cl1, IdBasedContingencyList cl2) {
        matchContingencyListMetadata(cl1.getMetadata(), cl2.getMetadata());
        assertTrue(new MatcherJson<>(objectMapper, cl1.getIdentifierContingencyList()).matchesSafely(cl2.getIdentifierContingencyList()));
    }

    @Test
    void createIdBasedContingencyList() throws Exception {
        Instant modificationDate = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "NHV1_NHV2_1");

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        IdBasedContingencyList resultList = createIdBasedContingencyList(contingencyListId, modificationDate, "NHV1_NHV2_1");

        res = mvc.perform(get("/" + VERSION + "/identifier-contingency-lists/" + contingencyListId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        matchIdBasedContingencyList(objectMapper.readValue(res, IdBasedContingencyList.class), resultList);

        mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ContingencyListMetadataImpl attributes = getMetadata(contingencyListId);
        assertEquals(attributes.getId(), contingencyListId);

        assertEquals(2, getContingencyListsCount());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());

        assertEquals(1, getContingencyListsCount());
    }

    @Test
    void createIdBasedContingencyListError() throws Exception {
        Instant modificationDate = Instant.now();

        IdBasedContingencyList idBasedContingencyList1 = createIdBasedContingencyList(null, modificationDate, "");
        mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList1))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        IdBasedContingencyList idBasedContingencyList2 = createIdBasedContingencyList(null, modificationDate, new String[0]);
        mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList2))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateBasedContingencyList() throws Exception {
        Instant modificationDate = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "id1");
        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID id = objectMapper.readValue(res, IdBasedContingencyList.class).getId();

        String newUuid = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists?duplicateFrom=" + id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertNotNull(newUuid);

        mvc.perform(post("/" + VERSION + "/identifier-contingency-lists?duplicateFrom=" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportIdBasedContingencyList() throws Exception {
        Instant modificationDate = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "NHV1_NHV2_1");

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();

        mvc.perform(get("/" + VERSION + "/contingency-lists/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .queryParam("contingencyListIds", contingencyListId.toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());
    }

    @Test
    void testExportUnknownContingencyList() throws Exception {
        mvc.perform(get("/" + VERSION + "/contingency-lists/" + UUID.randomUUID() + "/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    void testCountUnknownContingencyList() throws Exception {
        String res = mvc.perform(get("/" + VERSION + "/contingency-lists/count?ids=" + UUID.randomUUID() + "&networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(0, Integer.parseInt(res));
    }

    @Test
    void modifyIdBasedContingencyList() throws Exception {
        Instant modificationDate = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "LINE1");

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();

        IdBasedContingencyList newList = createIdBasedContingencyList(contingencyListId, modificationDate, "LINE2");

        mvc.perform(put("/" + VERSION + "/identifier-contingency-lists/" + newList.getId())
                        .content(objectMapper.writeValueAsString(newList))
                        .contentType(APPLICATION_JSON)
                        .header(USER_ID_HEADER, USER_ID_HEADER))
                .andExpect(status().isOk());

        Message<byte[]> message = output.receive(TIMEOUT, elementUpdateDestination);
        assertEquals(contingencyListId, message.getHeaders().get(NotificationService.HEADER_ELEMENT_UUID));
        assertEquals(USER_ID_HEADER, message.getHeaders().get(NotificationService.HEADER_MODIFIED_BY));

        contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        IdBasedContingencyList resultList = createIdBasedContingencyList(contingencyListId, modificationDate, "LINE2");
        res = mvc.perform(get("/" + VERSION + "/identifier-contingency-lists/" + contingencyListId)
                        .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        matchIdBasedContingencyList(objectMapper.readValue(res, IdBasedContingencyList.class), resultList);

        mvc.perform(put("/" + VERSION + "/identifier-contingency-lists/" + UUID.randomUUID())
                        .content(objectMapper.writeValueAsString(newList))
                        .contentType(APPLICATION_JSON)
                        .header(USER_ID_HEADER, USER_ID_HEADER))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCountContingencyList() throws Exception {
        // insert 1 contingency list with 3 filters
        UUID id = setupCountContingencyTest();

        // count them (incl a wrong uuid) - Initial variant
        String res = mvc.perform(get("/" + VERSION + "/contingency-lists/count?ids=" + UUID.randomUUID() + "&ids=" + id + "&networkUuid=" + NETWORK_UUID + "&variantId=" + VariantManagerConstants.INITIAL_VARIANT_ID)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(2, Integer.parseInt(res));

        // count them (incl a wrong uuid) - first variant (without generator)
        res = mvc.perform(get("/" + VERSION + "/contingency-lists/count?ids=" + UUID.randomUUID() + "&ids=" + id + "&networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(1, Integer.parseInt(res));
    }

    private UUID setupCountContingencyTest() throws Exception {
        List<UUID> filters = List.of(UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID());

        String list = genFilterBasedContingencyList(filters);
        UUID id = addNewFilterBasedContingencyList(list);

        MappingBuilder requestPatternBuilder = WireMock.post(WireMock.urlPathEqualTo("/v1/filters/evaluate/identifiables"))
                .withQueryParam("networkUuid", WireMock.equalTo(NETWORK_UUID.toString()))
                .withRequestBody(WireMock.containing(filters.get(0).toString())
                        .and(WireMock.containing(filters.get(1).toString()))
                        .and(WireMock.containing(filters.get(2).toString()))
                        .and(WireMock.containing(IdentifiableType.GENERATOR.name())));

        List<IdentifiableAttributes> identifiablesList = List.of(
                new IdentifiableAttributes("GEN", IdentifiableType.GENERATOR, 0d),
                new IdentifiableAttributes("GEN2", IdentifiableType.GENERATOR, 0d));

        FilteredIdentifiables filteredIdentifiables = new FilteredIdentifiables(identifiablesList, List.of());
        wireMockServer.stubFor(requestPatternBuilder.willReturn(
                WireMock.ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(filteredIdentifiables))));
        return id;
    }

    @Test
    void testCountContingencyListByGroup() throws Exception {
        // insert 1 contingency list with 3 filters
        UUID id = setupCountContingencyTest();

        // count them (incl a wrong uuid) - Initial variant
        ContingencyIdsByGroup contingencyIdsByGroup = ContingencyIdsByGroup.builder().ids(Map.of(CONTINGENCY_1, List.of(id, UUID.randomUUID()), CONTINGENCY_2, List.of(UUID.randomUUID()))).build();
        Map<String, Long> res = objectMapper.readValue(mvc.perform(post("/" + VERSION + "/contingency-lists/count/by-group?ids=" + UUID.randomUUID() + "&ids=" + id + "&networkUuid=" + NETWORK_UUID + "&variantId=" + VariantManagerConstants.INITIAL_VARIANT_ID)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contingencyIdsByGroup)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(2, res.get(CONTINGENCY_1));
        assertEquals(0, res.get(CONTINGENCY_2));

        // count them (incl a wrong uuid) - first variant (without generator)
        res = objectMapper.readValue(mvc.perform(post("/" + VERSION + "/contingency-lists/count/by-group?ids=" + UUID.randomUUID() + "&ids=" + id + "&networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(contingencyIdsByGroup)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        assertEquals(1, res.get(CONTINGENCY_1));
        assertEquals(0, res.get(CONTINGENCY_2));
    }
}
