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
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
@WebMvcTest(ContingencyListController.class)
@ContextConfiguration(classes = {ActionsApplication.class})
public class ContingencyListControllerTest extends AbstractEmbeddedCassandraSetup {

    private static final UUID NETWORK_UUID = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
    private static final UUID NETWORK_UUID_2 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e5");
    private static final UUID NETWORK_UUID_3 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e6");
    private static final UUID NETWORK_UUID_4 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e7");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ScriptContingencyListRepository contingencyListRepository;

    @Autowired
    private ContingencyListService contingencyListService;

    @MockBean
    private NetworkStoreService networkStoreService;

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
                "  \"nominalVoltageOperator\": \">\"" +
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

        // Check data
        mvc.perform(get("/" + VERSION + "/contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"foo\",\"type\":\"SCRIPT\"},{\"name\":\"tic\",\"type\":\"FILTERS\"}]"));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\"}]"));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"type\":\"FILTERS\"}]"));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\"}"));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/tic")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"type\":\"FILTERS\"}"));

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
                .andExpect(content().json("[]")); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/contingency-lists/tic/export")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]")); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/contingency-lists/foo/export?networkUuid=" + NETWORK_UUID)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]"));

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
                .andExpect(content().json("{\"name\":\"tac\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"type\":\"FILTERS\"}"));

        // check bar values
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/bar")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"bar\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\"}"));

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

    @Test
    public void testExportContingencies1() throws Exception {
        String lineFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String lineFilters1 = "{\"equipmentID\": \"NHV1*\", \"equipmentName\": \"*\", \"equipmentType\": \"LINE\", \"nominalVoltage\": \"100\",\"nominalVoltageOperator\": \"<\"}";
        String lineFilters2 = "{\"equipmentID\": \"NHV1*\", \"equipmentName\": \"*\", \"equipmentType\": \"LINE\", \"nominalVoltage\": \"380\",\"nominalVoltageOperator\": \"=\"}";
        String lineFilters3 = "{\"equipmentID\": \"NHV1*\", \"equipmentName\": \"*\", \"equipmentType\": \"LINE\", \"nominalVoltage\": \"390\",\"nominalVoltageOperator\": \">=\"}";
        String lineFilters4 = "{\"equipmentID\": \"NHV1*\", \"equipmentName\": \"*\", \"equipmentType\": \"LINE\", \"nominalVoltage\": \"390\",\"nominalVoltageOperator\": \"<=\"}";
        String lineFilters5 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"LINE\", \"nominalVoltage\": \"100\",\"nominalVoltageOperator\": \">\"}";
        String lineFilters6 = "{\"equipmentID\": \".NOTFOUND*\", \"equipmentName\": \"NVH1*\", \"equipmentType\": \"LINE\", \"nominalVoltage\": \"100\",\"nominalVoltageOperator\": \">\"}";
        testExportContingencies("lineFilters", lineFilters, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters1, " []", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters2, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters3, " []", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters4, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters5, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("lineFilters", lineFilters6, " []", NETWORK_UUID);

        String twtFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtFilters1 = "{\"equipmentID\": \"NGEN_NHV1\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtFilters2 = "{\"equipmentID\": \"NOTFOUND*\", \"equipmentName\": \"NGEN_NHV1\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtFilters3 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \">\"}";
        testExportContingencies("twtFilters", twtFilters, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("twtFilters", twtFilters1, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies("twtFilters", twtFilters2, " []", NETWORK_UUID);
        testExportContingencies("twtFilters", twtFilters3, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        String generatorFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorFilters2 = "{\"equipmentID\": \"GEN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorFilters3 = "{\"equipmentID\": \"NOTFOUND\", \"equipmentName\": \"GEN\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorFilters4 = "{\"equipmentID\": \"NOTFOUND\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \"<\"}";
        testExportContingencies("generatorFilters", generatorFilters, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies("generatorFilters", generatorFilters2, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies("generatorFilters", generatorFilters3, " []", NETWORK_UUID);
        testExportContingencies("generatorFilters", generatorFilters4, " []", NETWORK_UUID);

        String svcFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcFilters2 = "{\"equipmentID\": \"SVC3\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcFilters3 = "{\"equipmentID\": \"NOTFOUND*\", \"equipmentName\": \"SVC2*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcFilters4 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"100\",\"nominalVoltageOperator\": \"<\"}";
        testExportContingencies("svcFilters", svcFilters, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}," +
                "{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies("svcFilters", svcFilters2, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies("svcFilters", svcFilters3, " []", NETWORK_UUID_3);
        testExportContingencies("svcFilters", svcFilters4, " []", NETWORK_UUID_3);
    }

    @Test
    public void testExportContingencies2() throws Exception {
        String scFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scFilters2 = "{\"equipmentID\": \"SHUNT*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scFilters3 = "{\"equipmentID\": \"NOTFOUND*\", \"equipmentName\": \"SHUNT*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scFilters4 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"=\"}";
        testExportContingencies("scFilters", scFilters, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies("scFilters", scFilters2, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies("scFilters", scFilters3, " []", NETWORK_UUID_4);
        testExportContingencies("scFilters", scFilters4, " []", NETWORK_UUID_4);

        String hvdcFilters = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcFilters2 = "{\"equipmentID\": \"L*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcFilters3 = "{\"equipmentID\": \"NOTFOUND*\", \"equipmentName\": \"AL*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
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
                .andExpect(status().isOk());    }

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
    public void contingencyListAttributesTest() throws Exception {
        ContingencyListAttributes contingencyListAttributes = new ContingencyListAttributes("myList", ContingencyListType.SCRIPT);
        assertEquals("myList", contingencyListAttributes.getName());
        assertEquals(ContingencyListType.SCRIPT, contingencyListAttributes.getType());
        ContingencyListAttributes contingencyListAttributes2 = new ContingencyListAttributes();
        assertNull(contingencyListAttributes2.getName());
        assertNull(contingencyListAttributes2.getType());
    }
}
