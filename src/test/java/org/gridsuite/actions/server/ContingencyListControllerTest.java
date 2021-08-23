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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.ShuntTestCaseFactory;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.gridsuite.actions.server.dto.ContingencyListAttributes;
import org.gridsuite.actions.server.dto.FiltersContingencyList;
import org.gridsuite.actions.server.dto.ScriptContingencyList;
import org.gridsuite.actions.server.entities.FiltersContingencyListEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.FiltersContingencyListRepository;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = {ActionsApplication.class})
public class ContingencyListControllerTest {

    private static final UUID NETWORK_UUID = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
    private static final UUID NETWORK_UUID_2 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e5");
    private static final UUID NETWORK_UUID_3 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e6");
    private static final UUID NETWORK_UUID_4 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e7");
    private static final double EPSILON = .001;

    @Autowired
    private ScriptContingencyListRepository scriptContingencyListRepository;

    @Autowired
    private FiltersContingencyListRepository filtersContingencyListRepository;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NetworkStoreService networkStoreService;

    @After
    public void cleanDB() {
        scriptContingencyListRepository.deleteAll();
        filtersContingencyListRepository.deleteAll();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators(new NetworkFactoryImpl());
        Network network2 = HvdcTestNetwork.createVsc(new NetworkFactoryImpl());
        Network network3 = SvcTestCaseFactory.createWithMoreSVCs(new NetworkFactoryImpl());
        Network network4 = ShuntTestCaseFactory.create(new NetworkFactoryImpl());
        given(networkStoreService.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreService.getNetwork(NETWORK_UUID_2, PreloadingStrategy.COLLECTION)).willReturn(network2);
        given(networkStoreService.getNetwork(NETWORK_UUID_3, PreloadingStrategy.COLLECTION)).willReturn(network3);
        given(networkStoreService.getNetwork(NETWORK_UUID_4, PreloadingStrategy.COLLECTION)).willReturn(network4);

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        cleanDB();
    }

    @Test
    public void test() throws Exception {
        UUID notFoundId = UUID.fromString("abcdef01-1234-5678-abcd-e123456789aa");
        String script = "{ \n" +
                "\"name\" : \"foo\",\n" +
                "\"script\" : \"contingency('NHV1_NHV2_1') {" +
                "     equipments 'NHV1_NHV2_1'}\"," +
                "\"userId\": \"userId\"," +
                "\"isPrivate\": \"false\"" +
                "}";

        String filters = "{\n" +
                "  \"name\": \"tic\"," +
                "  \"equipmentID\": \"GEN*\"," +
                "  \"equipmentName\": \"GEN*\"," +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        String filters2 = "{\n" +
                "  \"name\": \"tuc\"," +
                "  \"equipmentID\": \"LINE*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage\": \"225\"," +
                "  \"nominalVoltageOperator\": \"<=\"," +
                "  \"countries\": [\"FR\", \"IT\", \"NL\"]" +
                "}";

        String filters3 = "{\n" +
                "  \"name\": \"toc\"," +
                "  \"equipmentID\": \"LOAD*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"LOAD\"," +
                "  \"nominalVoltage\": \"380\"," +
                "  \"nominalVoltageOperator\": \"=\"," +
                "  \"countries\": []" +
                "}";

        UUID scriptId = addNewScriptFilter(script, "userId", false);

        String res = mvc.perform(post("/" + VERSION + "/filters-contingency-lists?isPrivate=false")
                .header("userId", "userId")
                .content(filters)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID ticId = objectMapper.readValue(res, FiltersContingencyList.class).getId();

        mvc.perform(post("/" + VERSION + "/filters-contingency-lists?isPrivate=true")
                .header("userId", "userId")
                .content(filters2)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(post("/" + VERSION + "/filters-contingency-lists?isPrivate=false")
                .header("userId", "userId")
                .content(filters3)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Check data
        mvc.perform(get("/" + VERSION + "/contingency-lists")
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"foo\",\"type\":\"SCRIPT\"},{\"name\":\"tic\",\"type\":\"FILTERS\"},{\"name\":\"tuc\",\"type\":\"FILTERS\"},{\"name\":\"toc\",\"type\":\"FILTERS\"}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists")
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}]", false));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists")
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FILTERS\"},{\"name\":\"toc\",\"equipmentID\":\"LOAD*\",\"equipmentName\":\"*\",\"equipmentType\":\"LOAD\",\"nominalVoltage\":380.0,\"nominalVoltageOperator\":\"=\",\"countries\":[],\"type\":\"FILTERS\"},{\"name\":\"tuc\",\"equipmentID\":\"LINE*\",\"equipmentName\":\"*\",\"equipmentType\":\"LINE\",\"nominalVoltage\":225.0,\"nominalVoltageOperator\":\"<=\",\"countries\":[\"IT\",\"FR\",\"NL\"],\"type\":\"FILTERS\"}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + scriptId)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}", false));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + ticId)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FILTERS\"}", false));

        // check not found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + notFoundId)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + notFoundId)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        // export contingencies
        mvc.perform(get("/" + VERSION + "/contingency-lists/" + scriptId + "/export")
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]", true)); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + ticId + "/export")
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]", true)); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + scriptId + "/export?networkUuid=" + NETWORK_UUID)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]}]", true));

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + scriptId)
                .header("userId", "userId"))
                .andExpect(status().isOk());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + ticId)
                .header("userId", "userId"))
                .andExpect(status().isOk());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + scriptId)
                .header("userId", "userId"))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + ticId)
                .header("userId", "userId"))
                .andExpect(status().isNotFound());
    }

    public StringBuilder jsonVal(String id, String val, boolean trailingComma) {
        return new StringBuilder("\"").append(id).append("\": \"").append(val).append("\"").append(trailingComma ? ", " : "");
    }

    public StringBuilder jsonVal(String id, Double val, boolean trailingComma) {
        return new StringBuilder("\"").append(id).append("\": ").append(val).append(trailingComma ? ", " : "");
    }

    public String genContingencyFilter(String equipmentId, String name, EquipmentType type,
                                       Integer nominalVoltage, String nominalVoltageOperator, Set<String> countries, String description) {
        return "{" +
                jsonVal("name", name, true) +
                jsonVal("equipmentID", equipmentId, true) +
                jsonVal("equipmentName", name, true) +
                jsonVal("equipmentType", type.name(), true) +
                jsonVal("nominalVoltage", "" + nominalVoltage, true) +
                jsonVal("nominalVoltageOperator", nominalVoltageOperator, true) +
                jsonVal("description", description, true) +
                jsonVal("userId", "userId", true) +
                jsonVal("isPrivate", "false", true) +
                "\"countries\": [" + (!countries.isEmpty() ? "\"" + join(countries, "\",\"") + "\"" : "") + "]}";
    }

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDateFilter() throws Exception {
        String desc = "smurf";
        String filter = genContingencyFilter("testDate", "*", EquipmentType.LINE, 11, "=", Set.of(), desc);

        UUID id = addNewFilterList(filter, "userId", false);
        ContingencyListAttributes attributes = getMetadata(id, "userId");

        assertEquals(id, attributes.getId());
        assertEquals(desc, attributes.getDescription());
        Date baseCreationDate = attributes.getCreationDate();
        Date baseModificationDate = attributes.getModificationDate();

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/" + id + "?isPrivate=false")
                .header("userId", "userId")
                .content(filter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        attributes = getMetadata(id, "userId");
        assertEquals(baseCreationDate, attributes.getCreationDate());
        assertTrue(baseModificationDate.getTime() < attributes.getModificationDate().getTime());
    }

    private UUID addNewFilterList(String filter, String userId, boolean isPrivate) throws Exception {
        String res = mvc.perform(post("/" + VERSION + "/filters-contingency-lists?isPrivate={isPrivate}", isPrivate)
                .header("userId", userId)
                .content(filter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        FiltersContingencyList list = objectMapper.readValue(res, FiltersContingencyList.class);
        FiltersContingencyList original = objectMapper.readValue(filter, FiltersContingencyList.class);
        compareFilterList(original, list);
        return list.getId();
    }

    private UUID addNewScriptFilter(String filter, String userId, boolean isPrivate) throws Exception {
        String res = mvc.perform(post("/" + VERSION + "/script-contingency-lists?isPrivate={isPrivate}", isPrivate)
                .header("userId", userId)
                .content(filter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        ScriptContingencyList list = objectMapper.readValue(res, ScriptContingencyList.class);
        compareScriptList(objectMapper.readValue(filter, ScriptContingencyList.class), list);
        return list.getId();
    }

    @Test
    public void testExportContingencies1() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");
        Set<String> belgium = Collections.singleton("BE");
        String lineFilters = genContingencyFilter("*", "*", EquipmentType.LINE, -1, "=", noCountries, null);
        String lineFilters1 = genContingencyFilter("NHV1*", "*", EquipmentType.LINE, 100, "<", noCountries, null);
        String lineFilters2 = genContingencyFilter("NHV1*", "*", EquipmentType.LINE, 380, "=", noCountries, null);
        String lineFilters3 = genContingencyFilter("NHV1*", "*", EquipmentType.LINE, 390, ">=", noCountries, null);
        String lineFilters4 = genContingencyFilter("NHV1*", "*", EquipmentType.LINE, 390, "<=", noCountries, null);
        String lineFilters5 = genContingencyFilter("*", "*", EquipmentType.LINE, 100, ">", noCountries, null);
        String lineFilters6 = genContingencyFilter("UNKNOWN", "NVH1*", EquipmentType.LINE, 100, ">", noCountries, null);
        String lineFilters7 = genContingencyFilter("*", "*", EquipmentType.LINE, -1, ">", france, null);

        testExportContingencies(lineFilters, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(lineFilters1, " []", NETWORK_UUID, "userId", false);
        testExportContingencies(lineFilters2, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(lineFilters3, " []", NETWORK_UUID, "userId", false);
        testExportContingencies(lineFilters4, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(lineFilters5, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(lineFilters6, " []", NETWORK_UUID, "userId", false);
        testExportContingencies(lineFilters7, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);

        String twtFilters0 = "{\"name\":\"test_tw\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String twtFilters1 = "{\"name\":\"test_tw\", \"equipmentID\": \"NGEN_NHV1\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String twtFilters2 = "{\"name\":\"test_tw\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"NGEN_NHV1\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String twtFilters3 = "{\"name\":\"test_tw\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \">\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String twtFilters4 = genContingencyFilter("*", "*", EquipmentType.TWO_WINDINGS_TRANSFORMER, -1, ">", france, null);
        testExportContingencies(twtFilters0, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(twtFilters1, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(twtFilters2, " []", NETWORK_UUID, "userId", false);
        testExportContingencies(twtFilters3, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(twtFilters4, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID, "userId", false);

        String generatorFilters1 = "{\"name\":\"test_generator\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String generatorFilters2 = "{\"name\":\"test_generator\", \"equipmentID\": \"GEN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String generatorFilters3 = "{\"name\":\"test_generator\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"GEN\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String generatorFilters4 = "{\"name\":\"test_generator\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \"<\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String generatorFilters5 = genContingencyFilter("*", "*", EquipmentType.GENERATOR, -1, ">", france, null);
        String generatorFilters6 = genContingencyFilter("*", "*", EquipmentType.GENERATOR, -1, ">", belgium, null);
        testExportContingencies(generatorFilters1, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(generatorFilters2, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(generatorFilters3, " []", NETWORK_UUID, "userId", false);
        testExportContingencies(generatorFilters4, " []", NETWORK_UUID, "userId", false);
        testExportContingencies(generatorFilters5, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID, "userId", false);
        testExportContingencies(generatorFilters6, " []", NETWORK_UUID, "userId", false);

        String svcFilters1 = "{\"name\":\"test_scv\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String svcFilters2 = "{\"name\":\"test_scv\", \"equipmentID\": \"SVC3\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String svcFilters3 = "{\"name\":\"test_scv\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"SVC2*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String svcFilters4 = "{\"name\":\"test_scv\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"100\",\"nominalVoltageOperator\": \"<\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String svcFilters5 = genContingencyFilter("*", "*", EquipmentType.STATIC_VAR_COMPENSATOR, -1, "<", france, null);
        String svcFilters6 = genContingencyFilter("*", "*", EquipmentType.STATIC_VAR_COMPENSATOR, -1, "<", belgium, null);
        testExportContingencies(svcFilters1, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}," +
                "{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3, "userId", false);
        testExportContingencies(svcFilters2, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3, "userId", false);
        testExportContingencies(svcFilters3, " []", NETWORK_UUID_3, "userId", false);
        testExportContingencies(svcFilters4, " []", NETWORK_UUID_3, "userId", false);
        testExportContingencies(svcFilters5, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]},{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3, "userId", false);
        testExportContingencies(svcFilters6, " []", NETWORK_UUID_3, "userId", false);
    }

    @Test
    public void testExportContingencies2() throws Exception {
        String scFilters1 = "{\"name\": \"test_sc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String scFilters2 = "{\"name\": \"test_sc\", \"equipmentID\": \"SHUNT*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String scFilters3 = "{\"name\": \"test_sc\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"SHUNT*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String scFilters4 = "{\"name\": \"test_sc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        testExportContingencies(scFilters1, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4, "userId", false);
        testExportContingencies(scFilters2, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4, "userId", false);
        testExportContingencies(scFilters3, " []", NETWORK_UUID_4, "userId", false);
        testExportContingencies(scFilters4, " []", NETWORK_UUID_4, "userId", false);

        String hvdcFilters1 = "{\"name\": \"test_hvdc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String hvdcFilters2 = "{\"name\": \"test_hvdc\", \"equipmentID\": \"L*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String hvdcFilters3 = "{\"name\": \"test_hvdc\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"L*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String hvdcFilters4 = "{\"name\": \"test_hvdc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"400\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        String hvdcFilters5 = "{\"name\": \"test_hvdc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"<\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        testExportContingencies(hvdcFilters1, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2, "userId", false);
        testExportContingencies(hvdcFilters2, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2, "userId", false);
        testExportContingencies(hvdcFilters3, " []", NETWORK_UUID_2, "userId", false);
        testExportContingencies(hvdcFilters4, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2, "userId", false);
        testExportContingencies(hvdcFilters5, " []", NETWORK_UUID_2, "userId", false);

        String bbsFilters = "{\"name\":\"test_bbs\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"BUSBAR_SECTION\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        testExportContingencies(bbsFilters, " []", NETWORK_UUID, "userId", false);

        String dlFilters = "{\"name\":\"test_dl\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"DANGLING_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\", \"userId\": \"userId\", \"isPrivate\": \"false\"}";
        testExportContingencies(dlFilters, " []", NETWORK_UUID, "userId", false);
    }

    void compareFilterList(FiltersContingencyList expected, FiltersContingencyList current) throws JsonProcessingException {
        assertEquals(expected.getName(), current.getName());
        if (null == expected.getCountries()) {
            assertTrue(current.getCountries().isEmpty());
        } else {
            assertEquals(expected.getCountries(), current.getCountries());
        }
        assertEquals(expected.getDescription(), current.getDescription());
        assertEquals(expected.getEquipmentName(), current.getEquipmentName());
        assertEquals(expected.getEquipmentType(), current.getEquipmentType());
        assertEquals(expected.getEquipmentID(), current.getEquipmentID());
        assertTrue(Math.abs(expected.getNominalVoltage() - current.getNominalVoltage()) < EPSILON);
        assertEquals(expected.getNominalVoltageOperator(), current.getNominalVoltageOperator());
        assertEquals(expected.getUserId(), current.getUserId());
        assertEquals(expected.isPrivate(), current.isPrivate());
    }

    @Test
    public void modifyFilterList() throws Exception {
        UUID id = addNewFilterList(genContingencyFilter("equiId", "filterName", EquipmentType.LINE,
                10, "=>",
                Collections.emptySet(), "plop"), "userId", false);

        String newFilter = genContingencyFilter("equiIdBis", "filterNameBis", EquipmentType.LINE,
                12, "<=",
                Collections.emptySet(), "plopBis");

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/" + id)
                .header("userId", "userId")
                .content(newFilter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        String res = mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + id)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        compareFilterList(objectMapper.readValue(newFilter, FiltersContingencyList.class),
                objectMapper.readValue(res, FiltersContingencyList.class));

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/" + UUID.randomUUID())
                .header("userId", "userId")
                .content(newFilter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void modifyScriptFilter() throws Exception {
        UUID id = addNewScriptFilter("{ \n" +
                "\"name\" : \"foo\",\n" +
                "\"script\" : \"contingency('NHV1_NHV2_1') {" +
                "     equipments 'NHV1_NHV2_1'}\"," +
                "\"userId\": \"userId\"," +
                "\"isPrivate\": \"false\"" +
                "}", "userId", false);

        String newFilter = "{\n" +
                "\"name\" : \"bar\",\n" +
                "\"script\" : \"contingency('NHV1_NHV2_2') {" +
                "     equipments 'NHV1_NHV2_2'}\"," +
                "\"userId\": \"userId\"," +
                "\"isPrivate\": \"false\"" +
                "}";

        mvc.perform(put("/" + VERSION + "/script-contingency-lists/" + id)
                .header("userId", "userId")
                .content(newFilter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        String res = mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + id)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        compareScriptList(objectMapper.readValue(newFilter, ScriptContingencyList.class),
                objectMapper.readValue(res, ScriptContingencyList.class));

        mvc.perform(put("/" + VERSION + "/script-contingency-lists/" + UUID.randomUUID())
                .header("userId", "userId")
                .content(newFilter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private void compareScriptList(ScriptContingencyList expected, ScriptContingencyList current) {
        assertEquals(expected.getName(), current.getName());
        assertEquals(expected.getScript(), current.getScript());
        assertEquals(expected.getDescription(), current.getDescription());
        assertEquals(expected.getUserId(), current.getUserId());
        assertEquals(expected.isPrivate(), current.isPrivate());
    }

    private void testExportContingencies(String content, String expectedContent, UUID networkId, String userId, boolean isPrivate) throws Exception {
        // put the data
        UUID filterId = addNewFilterList(content, userId, isPrivate);

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + filterId + "/export?networkUuid=" + networkId)
                .header("userId", userId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(expectedContent));
        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + filterId)
                .header("userId", userId))
                .andExpect(status().isOk());
    }

    @Test
    public void emptyScriptTest() throws Exception {
        UUID id = addNewScriptFilter("{" +
                "\"name\" : \"foo\"," +
                "\"script\" : \"\"," +
                "\"description\":\"something\"," +
                "\"userId\": \"userId\"," +
                "\"isPrivate\": \"false\"}", "userId", false);

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + id + "/export")
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]"));

        ContingencyListAttributes attributes = getMetadata(id, "userId");
        assertEquals(attributes.getId(), id);
        assertEquals("foo", attributes.getName());
        assertEquals("something", attributes.getDescription());
    }

    private ContingencyListAttributes getMetadata(UUID id, String userId) throws Exception {
        var res = mvc.perform(get("/" + VERSION + "/metadata/")
                .header("userId", userId)
                .content("[\"" + id + "\"]")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<ContingencyListAttributes> contingencyListAttributes = objectMapper.readValue(res, new TypeReference<>() {
        });
        assertEquals(1, contingencyListAttributes.size());
        return contingencyListAttributes.get(0);
    }

    @Test
    public void testExportContingencies3() {
        Throwable e = null;
        UUID id = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e8");
        String lineFilters = genContingencyFilter("*", "*", EquipmentType.LINE, -1, "=", Collections.emptySet(), null);
        try {
            testExportContingencies(lineFilters, "", id, "userId", false);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Network '7928181c-7977-4592-ba19-88027e4254e8' not found", e.getMessage());
    }

    @Test
    public void testExportContingencies4() {
        Throwable e = null;
        String lineFilters = genContingencyFilter("*", "*", EquipmentType.LINE, 200, "$", Collections.emptySet(), null);
        try {
            testExportContingencies(lineFilters, "", NETWORK_UUID, "userId", false);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Unknown nominal voltage operator", e.getMessage());
    }

    @Test
    public void contingencyListAttributesTest() {
        UUID contingencyListAttrId = UUID.randomUUID();
        ContingencyListAttributes contingencyListAttributes = new ContingencyListAttributes(contingencyListAttrId, "myList", ContingencyListType.SCRIPT, null, null, null, "userId", false);
        assertEquals(contingencyListAttrId, contingencyListAttributes.getId());
        assertEquals("myList", contingencyListAttributes.getName());
        assertEquals(ContingencyListType.SCRIPT, contingencyListAttributes.getType());
        assertEquals("userId", contingencyListAttributes.getUserId());
        assertFalse(contingencyListAttributes.isPrivate());
        ContingencyListAttributes contingencyListAttributes2 = new ContingencyListAttributes();
        assertNull(contingencyListAttributes2.getId());
        assertNull(contingencyListAttributes2.getName());
        assertNull(contingencyListAttributes2.getType());
        assertNull(contingencyListAttributes2.getUserId());
        assertFalse(contingencyListAttributes2.isPrivate());
    }

    @Test
    public void scriptContingencyListEntityTest() {
        ScriptContingencyListEntity entity = new ScriptContingencyListEntity();
        entity.setName("list1");
        entity.setScript("");

        assertEquals("list1", entity.getName());
        assertEquals("", entity.getScript());
    }

    @Test
    public void filtersContingencyListEntityTest() {
        FiltersContingencyListEntity entity = new FiltersContingencyListEntity();
        entity.setName("list1");
        entity.setEquipmentId("id1");
        entity.setEquipmentName("name1");
        entity.setEquipmentType("LINE");
        entity.setNominalVoltage(225.);
        entity.setNominalVoltageOperator("=");
        entity.setCountries(Set.of("FRANCE", "ITALY"));

        assertEquals("list1", entity.getName());
        assertEquals("id1", entity.getEquipmentId());
        assertEquals("name1", entity.getEquipmentName());
        assertEquals("LINE", entity.getEquipmentType());
        assertEquals(225., entity.getNominalVoltage(), 0.1);
        assertEquals("=", entity.getNominalVoltageOperator());
        assertTrue(entity.getCountries().contains("FRANCE"));
        assertTrue(entity.getCountries().contains("ITALY"));
    }

    @Test
    public void replaceFiltersWithScriptTest() throws Exception {
        String filters = "{\n" +
                "  \"name\": \"" + "willBeScript" + "\"," +
                "  \"equipmentID\": \"GEN*\"," +
                "  \"equipmentName\": \"GEN*\"," +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"," +
                "  \"countries\": [\"FR\", \"BE\"]," +
                "  \"userId\": \"userId\"," +
                "  \"isPrivate\": \"false\"" +
                "}";

        // Put data
        UUID id = addNewFilterList(filters, "userId", false);

        // replace with groovy script
        String res = mvc.perform(post("/" + VERSION + "/filters-contingency-lists/" + id + "/replace-with-script")
                .header("userId", "userId"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID newId = objectMapper.readValue(res, ScriptContingencyList.class).getId();

        // check filter list tic not found
        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + id)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // check script tic is found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + newId)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }

    @Test
    public void copyFiltersToScriptTest() throws Exception {
        String filters = new StringJoiner(",\n", "{\n", "\n}")
                .add("  \"name\": \"tic\"")
                .add("  \"equipmentID\": \"GEN*\"")
                .add("  \"equipmentName\": \"GEN*\"")
                .add("  \"equipmentType\": \"GENERATOR\"")
                .add("  \"nominalVoltage\": \"100\"")
                .add("  \"nominalVoltageOperator\": \">\"")
                .add("  \"countries\": [\"FR\", \"BE\"]")
                .add("  \"userId\": \"userId\"")
                .add("  \"isPrivate\": \"false\"")
                .toString();

        // Put data
        UUID firstUUID = addNewFilterList(filters, "userId", false);

        // new script from filters
        mvc.perform(post("/" + VERSION + "/filters-contingency-lists/" + firstUUID + "/new-script/tac")
                .header("userId", "userId"))
                .andExpect(status().isOk());

        // check script tac is found
        String res = mvc.perform(get("/" + VERSION + "/script-contingency-lists/")
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();
        assertTrue(res.contains("tac"));

        // check filter list tic found
        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + firstUUID)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void changeAccessRightsTest() throws Exception {
        String filters = new StringJoiner(",\n", "{\n", "\n}")
                .add("  \"name\": \"tic\"")
                .add("  \"equipmentID\": \"GEN*\"")
                .add("  \"equipmentName\": \"GEN*\"")
                .add("  \"equipmentType\": \"GENERATOR\"")
                .add("  \"nominalVoltage\": \"100\"")
                .add("  \"nominalVoltageOperator\": \">\"")
                .add("  \"countries\": [\"FR\", \"BE\"]")
                .add("  \"userId\": \"userId\"")
                .add("  \"isPrivate\": \"false\"")
                .toString();

        String res1 = mvc.perform(post("/" + VERSION + "/filters-contingency-lists?isPrivate=false")
                .header("userId", "userId")
                .content(filters)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        FiltersContingencyList filter1 = objectMapper.readValue(res1, FiltersContingencyList.class);
        UUID firstUUID = filter1.getId();

        // Change access rights to private
        mvc.perform(post("/" + VERSION + "/contingency-lists/" + firstUUID + "/private")
                .header("userId", "userId"))
                .andExpect(status().isOk());

        ContingencyListAttributes attributes = getMetadata(firstUUID, "userId");
        assertEquals(firstUUID, attributes.getId());
        assertEquals("userId", attributes.getUserId());
        assertTrue(attributes.isPrivate());

        // Change access rights to public
        mvc.perform(post("/" + VERSION + "/contingency-lists/" + firstUUID + "/public")
                .header("userId", "userId"))
                .andExpect(status().isOk());

        attributes = getMetadata(firstUUID, "userId");
        assertEquals(firstUUID, attributes.getId());
        assertEquals("userId", attributes.getUserId());
        assertFalse(attributes.isPrivate());

        String res2 = mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + firstUUID)
                .header("userId", "userId")
                .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        FiltersContingencyList filter2 = objectMapper.readValue(res2, FiltersContingencyList.class);
        compareFilterList(filter1, filter2);
    }
}
