/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

        Network network = EurostagTutorialExample1Factory.create(new NetworkFactoryImpl());
        given(networkStoreService.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION)).willReturn(network);
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
                .andExpect(content().json("[{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":\"100\",\"nominalVoltageOperator\":\">\",\"type\":\"FILTERS\"}]"));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\"}"));

        mvc.perform(get("/" + VERSION + "/filters-contingency-lists/tic")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"tic\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":\"100\",\"nominalVoltageOperator\":\">\",\"type\":\"FILTERS\"}"));

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
                .andExpect(content().json("{\"name\":\"tac\",\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":\"100\",\"nominalVoltageOperator\":\">\",\"type\":\"FILTERS\"}"));

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
    public void testExportContingencies() throws Exception {
        String lineFilters = "{\n" +
                "  \"equipmentID\": \"NHV1.*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"" +
                "}";

        String twtFilters = "{\n" +
                "  \"equipmentID\": \"NHV1.*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"" +
                "}";

        String generatorFilters = "{\n" +
                "  \"equipmentID\": \".*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": \"*\"," +
                "  \"nominalVoltageOperator\": \"=\"" +
                "}";

        String svcFilters = "{\n" +
                "  \"equipmentID\": \".*\"," +
                "  \"equipmentName\": \".*\"," +
                "  \"equipmentType\": \"STATIC_VAR_COMPENSATOR\"," +
                "  \"nominalVoltage\": \".*\"," +
                "  \"nominalVoltageOperator\": \"=\"" +
                "}";

        String scFilters = "{\n" +
                "  \"equipmentID\": \".*\"," +
                "  \"equipmentName\": \".*\"," +
                "  \"equipmentType\": \"SHUNT_COMPENSATOR\"," +
                "  \"nominalVoltage\": \".*\"," +
                "  \"nominalVoltageOperator\": \"=\"" +
                "}";

        String hvdcFilters = "{\n" +
                "  \"equipmentID\": \".*\"," +
                "  \"equipmentName\": \".*\"," +
                "  \"equipmentType\": \"HVDC_LINE\"," +
                "  \"nominalVoltage\": \".*\"," +
                "  \"nominalVoltageOperator\": \"=\"" +
                "}";

        String bbsFilters = "{\n" +
                "  \"equipmentID\": \".*\"," +
                "  \"equipmentName\": \".*\"," +
                "  \"equipmentType\": \"BUSBAR_SECTION\"," +
                "  \"nominalVoltage\": \".*\"," +
                "  \"nominalVoltageOperator\": \"=\"" +
                "}";

        String dlFilters = "{\n" +
                "  \"equipmentID\": \".*\"," +
                "  \"equipmentName\": \".*\"," +
                "  \"equipmentType\": \"DANGLING_LINE\"," +
                "  \"nominalVoltage\": \".*\"," +
                "  \"nominalVoltageOperator\": \"=\"" +
                "}";

        testExportContingencies("lineFilters", lineFilters, "[{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]");
        testExportContingencies("generatorFilters", generatorFilters, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]");
        testExportContingencies("svcFilters", svcFilters, " []");
        testExportContingencies("scFilters", scFilters, " []");
        testExportContingencies("hvdcFilters", hvdcFilters, " []");
        testExportContingencies("bbsFilters", bbsFilters, " []");
        testExportContingencies("dlFilters", dlFilters, " []");
    }

    private void testExportContingencies(String filtersName, String content, String expectedContent) throws Exception {
        // put new data
        mvc.perform(put("/" + VERSION + "/filters-contingency-lists/" + filtersName)
                .content(content)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // export contingencies
        mvc.perform(get("/" + VERSION + "/contingency-lists/" + filtersName + "/export?networkUuid=" + NETWORK_UUID)
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
        assertEquals(null, contingencyListAttributes2.getName());
        assertEquals(null, contingencyListAttributes2.getType());
    }
}
