/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

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
import java.util.Set;
import java.util.UUID;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
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

    private void cleanDB() {
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
        String script =
                "contingency('NHV1_NHV2_1') {" +
                        "     equipments 'NHV1_NHV2_1'" +
                        "}";

        String filters = "{\n" +
                "  \"equipmentID\": \"GEN*\"," +
                "  \"equipmentName\": \"GEN*\"," +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        String filters2 = "{\n" +
                "  \"equipmentID\": \"LINE*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage\": \"225\"," +
                "  \"nominalVoltageOperator\": \"<=\"," +
                "  \"countries\": [\"FR\", \"IT\", \"NL\"]" +
                "}";

        String filters3 = "{\n" +
                "  \"equipmentID\": \"LOAD*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"LOAD\"," +
                "  \"nominalVoltage\": \"380\"," +
                "  \"nominalVoltageOperator\": \"=\"," +
                "  \"countries\": []" +
                "}";

        // Put data
        mvc.perform(put("/" + VERSION + "/script-contingency-lists/foo")
                .content(script)
                .contentType(TEXT_PLAIN))
                .andExpect(status().isOk());

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/tic")
                .content(filters)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/tuc")
                .content(filters2)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/toc")
                .content(filters3)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Check data
        mvc.perform(get("/" + VERSION + "/contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"foo\",\"type\":\"SCRIPT\"},{\"name\":\"tic\",\"type\":\"FILTERS\"},{\"name\":\"tuc\",\"type\":\"FILTERS\"},{\"name\":\"toc\",\"type\":\"FILTERS\"}]", true));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}]", true));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FILTERS\"},{\"name\":\"toc\",\"equipmentID\":\"LOAD*\",\"equipmentName\":\"*\",\"equipmentType\":\"LOAD\",\"nominalVoltage\":380.0,\"nominalVoltageOperator\":\"=\",\"countries\":[],\"type\":\"FILTERS\"},{\"name\":\"tuc\",\"equipmentID\":\"LINE*\",\"equipmentName\":\"*\",\"equipmentType\":\"LINE\",\"nominalVoltage\":225.0,\"nominalVoltageOperator\":\"<=\",\"countries\":[\"IT\",\"FR\",\"NL\"],\"type\":\"FILTERS\"}]", true));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}", true));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/tic")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FILTERS\"}", true));

        // check not found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/bar")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/tac")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        // export contingencies
        mvc.perform(get("/" + VERSION + "/contingency-lists/foo/export")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]", true)); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/contingency-lists/tic/export")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]", true)); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/contingency-lists/foo/export?networkUuid=" + NETWORK_UUID)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]}]", true));

        // rename baz --> bar ---> baz not found
        mvc.perform(post("/" + VERSION + "/script-contingency-lists/baz/rename")
                .content("{\"newContingencyListName\": \"bar\"}")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // rename foo --> bar
        mvc.perform(post("/" + VERSION + "/contingency-lists/foo/rename")
                .content("{\"newContingencyListName\": \"bar\"}")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // rename tic --> tac
        mvc.perform(post("/" + VERSION + "/contingency-lists/tic/rename")
                .content("{\"newContingencyListName\": \"tac\"}")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // check tac values
        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/tac")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"tac\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FILTERS\"}", true));

        // check bar values
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/bar")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"bar\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}", true));

        // check foo not found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/bar"))
                .andExpect(status().isOk());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/tac"))
                .andExpect(status().isOk());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/foo"))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/tac"))
                .andExpect(status().isNotFound());
    }

    public StringBuilder jsonVal(String id, String val, boolean trailingComma) {
        return new StringBuilder("\"").append(id).append("\": \"").append(val).append("\"").append(trailingComma ? ", " : "");
    }

    public String genContingencyFilter(String id, String name, EquipmentType type,
                                       Integer nominalVoltage, String nominalVoltageOperator, Set<String> countries) {
        return "{" +
                jsonVal("equipmentID", id, true) +
                jsonVal("equipmentName", name, true) +
                jsonVal("equipmentType", type.name(), true) +
                jsonVal("nominalVoltage", "" + nominalVoltage, true) +
                jsonVal("nominalVoltageOperator", nominalVoltageOperator, true) +
                "\"countries\": [" + (!countries.isEmpty() ? "\"" + join(countries, "\",\"") + "\"" : "") + "]}";

    }

    @Test
    public void testExportContingencies1() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");
        Set<String> belgium = Collections.singleton("BE");
        String lineFilters  = genContingencyFilter("*", "*", EquipmentType.LINE, -1, "=", noCountries);
        String lineFilters1 = genContingencyFilter("NHV1*", "*", EquipmentType.LINE, 100, "<", noCountries);
        String lineFilters2 = genContingencyFilter("NHV1*", "*", EquipmentType.LINE, 380, "=", noCountries);
        String lineFilters3 = genContingencyFilter("NHV1*", "*", EquipmentType.LINE, 390, ">=", noCountries);
        String lineFilters4 = genContingencyFilter("NHV1*", "*", EquipmentType.LINE, 390, "<=", noCountries);
        String lineFilters5 = genContingencyFilter("*", "*", EquipmentType.LINE, 100, ">", noCountries);
        String lineFilters6 = genContingencyFilter("UNKNOWN", "NVH1*", EquipmentType.LINE, 100, ">", noCountries);
        String lineFilters7 = genContingencyFilter("*", "*", EquipmentType.LINE, -1, ">", france);

        testExportContingencies("lineFilters", lineFilters, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters1, " []", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters2, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters3, " []", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters4, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters5, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters6, " []", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters7, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        String twtFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtFilters1 = "{\"equipmentID\": \"NGEN_NHV1\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtFilters2 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"NGEN_NHV1\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtFilters3 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \">\"}";
        String twtFilters4 = genContingencyFilter("*", "*", EquipmentType.TWO_WINDINGS_TRANSFORMER, -1, ">", france);
        testExportContingencies("twtFilters", twtFilters, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("twtFilters", twtFilters1, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("twtFilters", twtFilters2, " []", NETWORK_UUID);
        testExportContingencies("twtFilters", twtFilters3, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("twtFilters", twtFilters4, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        String generatorFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorFilters2 = "{\"equipmentID\": \"GEN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorFilters3 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"GEN\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorFilters4 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \"<\"}";
        String generatorFilters5 = genContingencyFilter("*", "*", EquipmentType.GENERATOR, -1, ">", france);
        String generatorFilters6 = genContingencyFilter("*", "*", EquipmentType.GENERATOR, -1, ">", belgium);
        testExportContingencies("generatorFilters", generatorFilters, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies("generatorFilters", generatorFilters2, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies("generatorFilters", generatorFilters3, " []", NETWORK_UUID);
        testExportContingencies("generatorFilters", generatorFilters4, " []", NETWORK_UUID);
        testExportContingencies("generatorFilters", generatorFilters5, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies("generatorFilters", generatorFilters6, " []", NETWORK_UUID);

        String svcFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcFilters2 = "{\"equipmentID\": \"SVC3\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcFilters3 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"SVC2*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcFilters4 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"100\",\"nominalVoltageOperator\": \"<\"}";
        String svcFilters5 = genContingencyFilter("*", "*", EquipmentType.STATIC_VAR_COMPENSATOR, -1, "<", france);
        String svcFilters6 = genContingencyFilter("*", "*", EquipmentType.STATIC_VAR_COMPENSATOR, -1, "<", belgium);
        testExportContingencies("svcFilters", svcFilters, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}," +
                "{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies("svcFilters", svcFilters2, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies("svcFilters", svcFilters3, " []", NETWORK_UUID_3);
        testExportContingencies("svcFilters", svcFilters4, " []", NETWORK_UUID_3);
        testExportContingencies("svcFilters", svcFilters5, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]},{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies("svcFilters", svcFilters6, " []", NETWORK_UUID_3);
    }

    @Test
    public void testExportContingencies2() throws Exception {
        String scFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scFilters2 = "{\"equipmentID\": \"SHUNT*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scFilters3 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"SHUNT*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scFilters4 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"=\"}";
        testExportContingencies("scFilters", scFilters, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies("scFilters", scFilters2, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies("scFilters", scFilters3, " []", NETWORK_UUID_4);
        testExportContingencies("scFilters", scFilters4, " []", NETWORK_UUID_4);

        String hvdcFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcFilters2 = "{\"equipmentID\": \"L*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcFilters3 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"L*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcFilters4 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"400\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcFilters5 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"<\"}";
        testExportContingencies("hvdcFilters", hvdcFilters, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies("hvdcFilters", hvdcFilters2, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies("hvdcFilters", hvdcFilters3, " []", NETWORK_UUID_2);
        testExportContingencies("hvdcFilters", hvdcFilters4, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies("hvdcFilters", hvdcFilters5, " []", NETWORK_UUID_2);

        String bbsFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"BUSBAR_SECTION\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        testExportContingencies("bbsFilters", bbsFilters, " []", NETWORK_UUID);

        String dlFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"DANGLING_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        testExportContingencies("dlFilters", dlFilters, " []", NETWORK_UUID);
    }

    private void testExportContingencies(String filtersName, String content, String expectedContent, UUID uuid) throws Exception {
        // put the data
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/" + filtersName)
                .content(content)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // export contingencies
        mvc.perform(get("/" + VERSION + "/contingency-lists/" + filtersName + "/export?networkUuid=" + uuid)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(expectedContent));

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + filtersName))
                .andExpect(status().isOk());
    }

    @Test
    public void emptyScriptTest() throws Exception {
        mvc.perform(put("/" + VERSION + "/script-contingency-lists/foo")
                .content("")
                .contentType(TEXT_PLAIN))
                .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/contingency-lists/foo/export")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    public void testExportContingencies3() {
        Throwable e = null;
        String lineFilters = genContingencyFilter("*", "*", EquipmentType.LINE, -1, "=", Collections.emptySet());
        try {
            testExportContingencies("lineFilters", lineFilters, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID_5);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Network '7928181c-7977-4592-ba19-88027e4254e8' not found", e.getMessage());
    }

    @Test
    public void testExportContingencies4() {
        Throwable e = null;
        String lineFilters = genContingencyFilter("*", "*", EquipmentType.LINE, 200, "$", Collections.emptySet());
        try {
            testExportContingencies("lineFilters", lineFilters, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Unknown nominal voltage operator", e.getMessage());
    }

    @Test
    public void contingencyListAttributesTest() {
        ContingencyListAttributes contingencyListAttributes = new ContingencyListAttributes("myList", ContingencyListType.SCRIPT);
        assertEquals("myList", contingencyListAttributes.getName());
        assertEquals(ContingencyListType.SCRIPT, contingencyListAttributes.getType());
        ContingencyListAttributes contingencyListAttributes2 = new ContingencyListAttributes();
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
        String filters = "{\n" +
                "  \"equipmentID\": \"GEN*\"," +
                "  \"equipmentName\": \"GEN*\"," +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        // Put data
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/tic")
                .content(filters)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // replace with groovy script
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/tic/replace-with-script"))
                .andExpect(status().isOk());

        // check filter list tic not found
        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/tic")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // check script tic is found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/tic")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }

    @Test
    public void copyFiltersToScriptTest() throws Exception {
        String filters = "{\n" +
                "  \"equipmentID\": \"GEN*\"," +
                "  \"equipmentName\": \"GEN*\"," +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        // Put data
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/tic")
                .content(filters)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // new script from filters
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/tic/new-script/tac"))
                .andExpect(status().isOk());

        // check script tac is found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/tac")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));

        // check filter list tic found
        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/tic")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
