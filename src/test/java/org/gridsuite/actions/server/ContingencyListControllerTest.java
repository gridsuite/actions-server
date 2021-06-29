/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.fasterxml.jackson.core.type.TypeReference;
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
    private static final UUID NETWORK_UUID_5 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e8");

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

        cleanDB();
    }

    @Test
    public void test() throws Exception {
        UUID scriptId = UUID.fromString("98765412-1234-5678-abcd-e123456789aa");
        UUID ticId = UUID.fromString("12345678-1234-5678-abcd-e123456789aa");
        UUID notFoundId = UUID.fromString("abcdef01-1234-5678-abcd-e123456789aa");
        String script = "{ \n" +
            "\"name\" : \"foo\",\n" +
            "\"id\" : \"" + scriptId + "\",\n" +
            "\"script\" : \"contingency('NHV1_NHV2_1') {" +
            "     equipments 'NHV1_NHV2_1'}\"" +
            "}";

        String filters = "{\n" +
            "  \"id\" : \"" + ticId + "\",\n" +
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

        // Put data
        mvc.perform(put("/" + VERSION + "/script-contingency-lists/")
            .content(script)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/")
            .content(filters)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/")
            .content(filters2)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/")
            .content(filters3)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        // Check data
        mvc.perform(get("/" + VERSION + "/contingency-lists")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("[{\"name\":\"foo\",\"type\":\"SCRIPT\"},{\"name\":\"tic\",\"type\":\"FILTERS\"},{\"name\":\"tuc\",\"type\":\"FILTERS\"},{\"name\":\"toc\",\"type\":\"FILTERS\"}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("[{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}]", false));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("[{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FILTERS\"},{\"name\":\"toc\",\"equipmentID\":\"LOAD*\",\"equipmentName\":\"*\",\"equipmentType\":\"LOAD\",\"nominalVoltage\":380.0,\"nominalVoltageOperator\":\"=\",\"countries\":[],\"type\":\"FILTERS\"},{\"name\":\"tuc\",\"equipmentID\":\"LINE*\",\"equipmentName\":\"*\",\"equipmentType\":\"LINE\",\"nominalVoltage\":225.0,\"nominalVoltageOperator\":\"<=\",\"countries\":[\"IT\",\"FR\",\"NL\"],\"type\":\"FILTERS\"}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + scriptId)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}", false));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + ticId)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FILTERS\"}", false));

        // check not found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + notFoundId)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + notFoundId)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));

        // export contingencies
        mvc.perform(get("/" + VERSION + "/contingency-lists/" + scriptId + "/export")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("[]", true)); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + ticId + "/export")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("[]", true)); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + scriptId + "/export?networkUuid=" + NETWORK_UUID)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]}]", true));

        // rename baz --> bar ---> baz not found
        mvc.perform(post("/" + VERSION + "/script-contingency-lists/" + notFoundId + "/rename")
            .content("{\"newContingencyListName\": \"bar\"}")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isNotFound());

        // rename baz --> bar ---> baz not found
        mvc.perform(post("/" + VERSION + "/contingency-lists/" + notFoundId + "/rename")
            .content("{\"newContingencyListName\": \"bar\"}")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isNotFound());

        // rename foo --> bar
        mvc.perform(post("/" + VERSION + "/contingency-lists/" + scriptId + "/rename")
            .content("{\"newContingencyListName\": \"bar\"}")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        // rename tic --> tac
        mvc.perform(post("/" + VERSION + "/contingency-lists/" + ticId + "/rename")
            .content("{\"newContingencyListName\": \"tac\"}")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        // check tac values
        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + ticId)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("{\"name\":\"tac\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FILTERS\"}", false));

        // check bar values
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + scriptId)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("{\"name\":\"bar\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}", false));

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + scriptId))
            .andExpect(status().isOk());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + ticId))
            .andExpect(status().isOk());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + scriptId))
            .andExpect(status().isNotFound());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + ticId))
            .andExpect(status().isNotFound());
    }

    public StringBuilder jsonVal(String id, String val, boolean trailingComma) {
        return new StringBuilder("\"").append(id).append("\": \"").append(val).append("\"").append(trailingComma ? ", " : "");
    }

    public String genContingencyFilter(UUID uuid, String id, String name, EquipmentType type,
                                       Integer nominalVoltage, String nominalVoltageOperator, Set<String> countries, String description) {
        return "{" +
            jsonVal("id", uuid.toString(), true) +
            jsonVal("name", "thisIsNotAnUUID_" + uuid, true) +
            jsonVal("equipmentID", id, true) +
            jsonVal("equipmentName", name, true) +
            jsonVal("equipmentType", type.name(), true) +
            jsonVal("nominalVoltage", "" + nominalVoltage, true) +
            jsonVal("nominalVoltageOperator", nominalVoltageOperator, true) +
            jsonVal("description", description, true) +
            "\"countries\": [" + (!countries.isEmpty() ? "\"" + join(countries, "\",\"") + "\"" : "") + "]}";

    }

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDateFilter() throws Exception {
        UUID id = UUID.randomUUID();
        String desc = "smurf";
        String filter = genContingencyFilter(id, "testDate", "*", EquipmentType.LINE, 11, "=", Set.of(), desc);

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/")
            .content(filter)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        ContingencyListAttributes attributes = getMetadata(id);

        assertEquals(id, attributes.getId());
        assertEquals(desc, attributes.getDescription());
        Date baseCreationDate = attributes.getCreationDate();
        Date baseModificationDate = attributes.getModificationDate();

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/")
            .content(filter)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        attributes = getMetadata(id);
        assertEquals(baseCreationDate, attributes.getCreationDate());
        assertTrue(baseModificationDate.getTime() < attributes.getModificationDate().getTime());
    }

    @Test
    public void testExportContingencies1() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");
        Set<String> belgium = Collections.singleton("BE");
        UUID id = UUID.randomUUID();
        String lineFilters = genContingencyFilter(id, "*", "*", EquipmentType.LINE, -1, "=", noCountries, null);
        String lineFilters1 = genContingencyFilter(id, "NHV1*", "*", EquipmentType.LINE, 100, "<", noCountries, null);
        String lineFilters2 = genContingencyFilter(id, "NHV1*", "*", EquipmentType.LINE, 380, "=", noCountries, null);
        String lineFilters3 = genContingencyFilter(id, "NHV1*", "*", EquipmentType.LINE, 390, ">=", noCountries, null);
        String lineFilters4 = genContingencyFilter(id, "NHV1*", "*", EquipmentType.LINE, 390, "<=", noCountries, null);
        String lineFilters5 = genContingencyFilter(id, "*", "*", EquipmentType.LINE, 100, ">", noCountries, null);
        String lineFilters6 = genContingencyFilter(id, "UNKNOWN", "NVH1*", EquipmentType.LINE, 100, ">", noCountries, null);
        String lineFilters7 = genContingencyFilter(id, "*", "*", EquipmentType.LINE, -1, ">", france, null);

        testExportContingencies(id, lineFilters, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(id, lineFilters1, " []", NETWORK_UUID);
        testExportContingencies(id, lineFilters2, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(id, lineFilters3, " []", NETWORK_UUID);
        testExportContingencies(id, lineFilters4, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(id, lineFilters5, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(id, lineFilters6, " []", NETWORK_UUID);
        testExportContingencies(id, lineFilters7, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        UUID twfilterId = UUID.randomUUID();

        String twtFilters0 = String.format("{\"id\":\"%s\", \"name\":\"test_tw\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}", twfilterId);
        String twtFilters1 = String.format("{\"id\":\"%s\", \"name\":\"test_tw\", \"equipmentID\": \"NGEN_NHV1\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}", twfilterId);
        String twtFilters2 = String.format("{\"id\":\"%s\", \"name\":\"test_tw\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"NGEN_NHV1\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}", twfilterId);
        String twtFilters3 = String.format("{\"id\":\"%s\", \"name\":\"test_tw\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \">\"}", twfilterId);
        String twtFilters4 = genContingencyFilter(twfilterId, "*", "*", EquipmentType.TWO_WINDINGS_TRANSFORMER, -1, ">", france, null);
        testExportContingencies(twfilterId, twtFilters0, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(twfilterId, twtFilters1, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(twfilterId, twtFilters2, " []", NETWORK_UUID);
        testExportContingencies(twfilterId, twtFilters3, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(twfilterId, twtFilters4, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        UUID generatorFilterId = UUID.randomUUID();

        String generatorFilters1 = String.format("{\"id\":\"%s\", \"name\":\"test_generator\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", generatorFilterId);
        String generatorFilters2 = String.format("{\"id\":\"%s\", \"name\":\"test_generator\", \"equipmentID\": \"GEN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", generatorFilterId);
        String generatorFilters3 = String.format("{\"id\":\"%s\", \"name\":\"test_generator\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"GEN\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", generatorFilterId);
        String generatorFilters4 = String.format("{\"id\":\"%s\", \"name\":\"test_generator\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \"<\"}", generatorFilterId);
        String generatorFilters5 = genContingencyFilter(generatorFilterId, "*", "*", EquipmentType.GENERATOR, -1, ">", france, null);
        String generatorFilters6 = genContingencyFilter(generatorFilterId, "*", "*", EquipmentType.GENERATOR, -1, ">", belgium, null);
        testExportContingencies(generatorFilterId, generatorFilters1, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies(generatorFilterId, generatorFilters2, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies(generatorFilterId, generatorFilters3, " []", NETWORK_UUID);
        testExportContingencies(generatorFilterId, generatorFilters4, " []", NETWORK_UUID);
        testExportContingencies(generatorFilterId, generatorFilters5, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies(generatorFilterId, generatorFilters6, " []", NETWORK_UUID);

        UUID svcFilterId = UUID.randomUUID();
        String svcFilters1 = String.format("{\"id\":\"%s\", \"name\":\"test_scv\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", svcFilterId);
        String svcFilters2 = String.format("{\"id\":\"%s\", \"name\":\"test_scv\", \"equipmentID\": \"SVC3\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", svcFilterId);
        String svcFilters3 = String.format("{\"id\":\"%s\", \"name\":\"test_scv\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"SVC2*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", svcFilterId);
        String svcFilters4 = String.format("{\"id\":\"%s\", \"name\":\"test_scv\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"100\",\"nominalVoltageOperator\": \"<\"}", svcFilterId);
        String svcFilters5 = genContingencyFilter(svcFilterId, "*", "*", EquipmentType.STATIC_VAR_COMPENSATOR, -1, "<", france, null);
        String svcFilters6 = genContingencyFilter(svcFilterId, "*", "*", EquipmentType.STATIC_VAR_COMPENSATOR, -1, "<", belgium, null);
        testExportContingencies(svcFilterId, svcFilters1, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}," +
            "{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies(svcFilterId, svcFilters2, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies(svcFilterId, svcFilters3, " []", NETWORK_UUID_3);
        testExportContingencies(svcFilterId, svcFilters4, " []", NETWORK_UUID_3);
        testExportContingencies(svcFilterId, svcFilters5, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]},{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies(svcFilterId, svcFilters6, " []", NETWORK_UUID_3);
    }

    @Test
    public void testExportContingencies2() throws Exception {
        UUID idFilter1 = UUID.randomUUID();
        UUID idFilter2 = UUID.randomUUID();
        UUID idFilter3 = UUID.randomUUID();
        UUID idFilter4 = UUID.randomUUID();
        String scFilters1 = String.format("{ \"id\":\"%s\", \"name\": \"test_sc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", idFilter1);
        String scFilters2 = String.format("{ \"id\":\"%s\", \"name\": \"test_sc\", \"equipmentID\": \"SHUNT*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", idFilter2);
        String scFilters3 = String.format("{ \"id\":\"%s\", \"name\": \"test_sc\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"SHUNT*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", idFilter3);
        String scFilters4 = String.format("{ \"id\":\"%s\", \"name\": \"test_sc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"=\"}", idFilter4);
        testExportContingencies(idFilter1, scFilters1, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies(idFilter2, scFilters2, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies(idFilter3, scFilters3, " []", NETWORK_UUID_4);
        testExportContingencies(idFilter4, scFilters4, " []", NETWORK_UUID_4);

        UUID hvdcFilterId = UUID.randomUUID();

        String hvdcFilters1 = String.format("{\"id\":\"%s\", \"name\": \"test_hvdc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", hvdcFilterId);
        String hvdcFilters2 = String.format("{\"id\":\"%s\", \"name\": \"test_hvdc\", \"equipmentID\": \"L*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", hvdcFilterId);
        String hvdcFilters3 = String.format("{\"id\":\"%s\", \"name\": \"test_hvdc\", \"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"L*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", hvdcFilterId);
        String hvdcFilters4 = String.format("{\"id\":\"%s\", \"name\": \"test_hvdc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"400\",\"nominalVoltageOperator\": \"=\"}", hvdcFilterId);
        String hvdcFilters5 = String.format("{\"id\":\"%s\", \"name\": \"test_hvdc\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"<\"}", hvdcFilterId);
        testExportContingencies(hvdcFilterId, hvdcFilters1, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcFilterId, hvdcFilters2, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcFilterId, hvdcFilters3, " []", NETWORK_UUID_2);
        testExportContingencies(hvdcFilterId, hvdcFilters4, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcFilterId, hvdcFilters5, " []", NETWORK_UUID_2);

        UUID bbsIlterId = UUID.randomUUID();

        String bbsFilters = String.format("{\"id\":\"%s\", \"name\":\"test_bbs\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"BUSBAR_SECTION\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", bbsIlterId);
        testExportContingencies(bbsIlterId, bbsFilters, " []", NETWORK_UUID);

        UUID dlFilterId = UUID.randomUUID();

        String dlFilters = String.format("{\"id\":\"%s\", \"name\":\"test_dl\", \"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"DANGLING_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}", dlFilterId);
        testExportContingencies(dlFilterId, dlFilters, " []", NETWORK_UUID);
    }

    private void testExportContingencies(UUID filterId, String content, String expectedContent, UUID uuid) throws Exception {
        // put the data
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/")
            .content(content)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        // export contingencies
        mvc.perform(get("/" + VERSION + "/contingency-lists/" + filterId + "/export?networkUuid=" + uuid)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json(expectedContent));

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + filterId))
            .andExpect(status().isOk());
    }

    @Test
    public void emptyScriptTest() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(put("/" + VERSION + "/script-contingency-lists/")
            .content("{\"id\" : \"" + id + "\"," +
                "\"name\" : \"foo\"," +
                "\"script\" : \"\"," +
                "\"description\":\"something\"}")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + id + "/export")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andExpect(content().json("[]"));

        ContingencyListAttributes attributes = getMetadata(id);
        assertEquals(attributes.getId(), id);
        assertEquals(attributes.getName(), "foo");
        assertEquals(attributes.getDescription(), "something");
    }

    private ContingencyListAttributes getMetadata(UUID id) throws Exception {
        var res = mvc.perform(get("/" + VERSION + "/metadata/")
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
        UUID id = UUID.randomUUID();
        String lineFilters = genContingencyFilter(id, "*", "*", EquipmentType.LINE, -1, "=", Collections.emptySet(), null);
        try {
            testExportContingencies(id, lineFilters, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID_5);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Network '7928181c-7977-4592-ba19-88027e4254e8' not found", e.getMessage());
    }

    @Test
    public void testExportContingencies4() {
        Throwable e = null;
        UUID id = UUID.randomUUID();
        String lineFilters = genContingencyFilter(id, "*", "*", EquipmentType.LINE, 200, "$", Collections.emptySet(), null);
        try {
            testExportContingencies(id, lineFilters, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Unknown nominal voltage operator", e.getMessage());
    }

    @Test
    public void contingencyListAttributesTest() {
        UUID contingencyListAttrId = UUID.randomUUID();
        ContingencyListAttributes contingencyListAttributes = new ContingencyListAttributes(contingencyListAttrId, "myList", ContingencyListType.SCRIPT, null, null, null);
        assertEquals(contingencyListAttrId, contingencyListAttributes.getId());
        assertEquals("myList", contingencyListAttributes.getName());
        assertEquals(ContingencyListType.SCRIPT, contingencyListAttributes.getType());
        ContingencyListAttributes contingencyListAttributes2 = new ContingencyListAttributes();
        assertNull(contingencyListAttributes2.getId());
        assertNull(contingencyListAttributes2.getName());
        assertNull(contingencyListAttributes2.getType());
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
        UUID id = UUID.randomUUID();
        String filters = "{\n" +
            "  \"id\": \"" + id + "\"," +
            "  \"name\": \"" + "willBeScript" + "\"," +
            "  \"equipmentID\": \"GEN*\"," +
            "  \"equipmentName\": \"GEN*\"," +
            "  \"equipmentType\": \"GENERATOR\"," +
            "  \"nominalVoltage\": \"100\"," +
            "  \"nominalVoltageOperator\": \">\"," +
            "  \"countries\": [\"FR\", \"BE\"]" +
            "}";

        // Put data
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/")
            .content(filters)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        // replace with groovy script
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/" + id + "/replace-with-script"))
            .andExpect(status().isOk());

        // check filter list tic not found
        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + id)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isNotFound());

        // check script tic is found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + id)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }

    @Test
    public void copyFiltersToScriptTest() throws Exception {
        UUID firstUUID = UUID.fromString("914df591-68aa-409f-bc30-847ede2a6a72");
        String filters = "{\n" +
            "  \"id\": \"" + firstUUID + "\"," +
            "  \"name\": \"tic\"," +
            "  \"equipmentID\": \"GEN*\"," +
            "  \"equipmentName\": \"GEN*\"," +
            "  \"equipmentType\": \"GENERATOR\"," +
            "  \"nominalVoltage\": \"100\"," +
            "  \"nominalVoltageOperator\": \">\"," +
            "  \"countries\": [\"FR\", \"BE\"]" +
            "}";

        // Put data
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/")
            .content(filters)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());

        // new script from filters
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/" + firstUUID + "/new-script/tac"))
            .andExpect(status().isOk());

        // check script tac is found
        var res = mvc.perform(get("/" + VERSION + "/script-contingency-lists/")
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
            .andReturn().getResponse().getContentAsString();
        assertTrue(res.contains("tac"));

        // check filter list tic found
        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/" + firstUUID)
            .contentType(APPLICATION_JSON))
            .andExpect(status().isOk());
    }
}
