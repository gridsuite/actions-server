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
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.ShuntTestCaseFactory;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.gridsuite.actions.server.dto.ContingencyListAttributes;
import org.gridsuite.actions.server.dto.FormContingencyList;
import org.gridsuite.actions.server.dto.ScriptContingencyList;
import org.gridsuite.actions.server.entities.FormContingencyListEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.FormContingencyListRepository;
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
    private static final String VARIANT_ID_1 = "variant_1";

    private Network network;

    private static final double EPSILON = .001;

    @Autowired
    private ScriptContingencyListRepository scriptContingencyListRepository;

    @Autowired
    private FormContingencyListRepository formContingencyListRepository;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NetworkStoreService networkStoreService;

    @After
    public void cleanDB() {
        scriptContingencyListRepository.deleteAll();
        formContingencyListRepository.deleteAll();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        network = EurostagTutorialExample1Factory.createWithMoreGenerators(new NetworkFactoryImpl());
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_ID_1);
        network.getVariantManager().setWorkingVariant(VARIANT_ID_1);
        // remove generator 'GEN2' from network in variant VARIANT_ID_1
        network.getGenerator("GEN2").remove();
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

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
                "\"script\" : \"contingency('NHV1_NHV2_1') {" +
                "     equipments 'NHV1_NHV2_1'}\"" +
                "}";

        String formContingencyList = "{\n" +
                "  \"equipmentID\": \"GEN*\"," +
                "  \"equipmentName\": \"GEN*\"," +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        String formContingencyList2 = "{\n" +
                "  \"equipmentID\": \"LINE*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage\": \"225\"," +
                "  \"nominalVoltageOperator\": \"<=\"," +
                "  \"countries\": [\"FR\", \"IT\", \"NL\"]" +
                "}";

        String formContingencyList3 = "{\n" +
                "  \"equipmentID\": \"LOAD*\"," +
                "  \"equipmentName\": \"*\"," +
                "  \"equipmentType\": \"LOAD\"," +
                "  \"nominalVoltage\": \"380\"," +
                "  \"nominalVoltageOperator\": \"=\"," +
                "  \"countries\": []" +
                "}";

        UUID scriptId = addNewScriptContingencyList(script);

        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists/")
                .content(formContingencyList)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID ticId = objectMapper.readValue(res, FormContingencyList.class).getId();

        mvc.perform(post("/" + VERSION + "/form-contingency-lists/")
                .content(formContingencyList2)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(post("/" + VERSION + "/form-contingency-lists/")
                .content(formContingencyList3)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Check data
        mvc.perform(get("/" + VERSION + "/contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"type\":\"SCRIPT\"},{\"type\":\"FORM\"},{\"type\":\"FORM\"},{\"type\":\"FORM\"}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}]", false));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FORM\"},{\"equipmentID\":\"LOAD*\",\"equipmentName\":\"*\",\"equipmentType\":\"LOAD\",\"nominalVoltage\":380.0,\"nominalVoltageOperator\":\"=\",\"countries\":[],\"type\":\"FORM\"},{\"equipmentID\":\"LINE*\",\"equipmentName\":\"*\",\"equipmentType\":\"LINE\",\"nominalVoltage\":225.0,\"nominalVoltageOperator\":\"<=\",\"countries\":[\"IT\",\"FR\",\"NL\"],\"type\":\"FORM\"}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + scriptId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}", false));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + ticId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"equipmentID\":\"GEN*\",\"equipmentName\":\"GEN*\",\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":100.0,\"nominalVoltageOperator\":\">\",\"countries\":[\"BE\",\"FR\"],\"type\":\"FORM\"}", false));

        // check not found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + notFoundId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + notFoundId)
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

    public StringBuilder jsonVal(String id, Double val, boolean trailingComma) {
        return new StringBuilder("\"").append(id).append("\": ").append(val).append(trailingComma ? ", " : "");
    }

    public String genFormContingencyList(String equipmentId, String equipmentName, EquipmentType type,
                                         Integer nominalVoltage, String nominalVoltageOperator, Set<String> countries) {
        return "{" +
                jsonVal("equipmentID", equipmentId, true) +
                jsonVal("equipmentName", equipmentName, true) +
                jsonVal("equipmentType", type.name(), true) +
                jsonVal("nominalVoltage", "" + nominalVoltage, true) +
                jsonVal("nominalVoltageOperator", nominalVoltageOperator, true) +
                "\"countries\": [" + (!countries.isEmpty() ? "\"" + join(countries, "\",\"") + "\"" : "") + "]}";

    }

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDateFormContingencyList() throws Exception {
        String list = genFormContingencyList("*", "*", EquipmentType.LINE, 11, "=", Set.of());

        UUID id = addNewFormContingencyList(list);
        ContingencyListAttributes attributes = getMetadata(id);

        assertEquals(id, attributes.getId());
        Date baseCreationDate = attributes.getCreationDate();
        Date baseModificationDate = attributes.getModificationDate();

        mvc.perform(put("/" + VERSION + "/form-contingency-lists/" + id)
                .content(list)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        attributes = getMetadata(id);
        assertEquals(baseCreationDate, attributes.getCreationDate());
        assertTrue(baseModificationDate.getTime() < attributes.getModificationDate().getTime());
    }

    private UUID addNewFormContingencyList(String form) throws Exception {
        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists/")
                .content(form)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        FormContingencyList list = objectMapper.readValue(res, FormContingencyList.class);
        FormContingencyList original = objectMapper.readValue(form, FormContingencyList.class);
        compareFormContingencyList(original, list);
        return list.getId();
    }

    private UUID addNewScriptContingencyList(String script) throws Exception {
        String res = mvc.perform(post("/" + VERSION + "/script-contingency-lists/")
                .content(script)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        ScriptContingencyList list = objectMapper.readValue(res, ScriptContingencyList.class);
        compareScriptList(objectMapper.readValue(script, ScriptContingencyList.class), list);
        return list.getId();
    }

    private UUID addNewScriptContingencyListWithId(String script, UUID id) throws Exception {
        String res = mvc.perform(post("/" + VERSION + "/script-contingency-lists?id=" + id)
                .content(script)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        ScriptContingencyList list = objectMapper.readValue(res, ScriptContingencyList.class);
        compareScriptList(objectMapper.readValue(script, ScriptContingencyList.class), list);
        return list.getId();
    }

    @Test
    public void testExportContingencies1() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");
        Set<String> belgium = Collections.singleton("BE");
        String lineForm = genFormContingencyList("*", "*", EquipmentType.LINE, -1, "=", noCountries);
        String lineForm1 = genFormContingencyList("NHV1*", "*", EquipmentType.LINE, 100, "<", noCountries);
        String lineForm2 = genFormContingencyList("NHV1*", "*", EquipmentType.LINE, 380, "=", noCountries);
        String lineForm3 = genFormContingencyList("NHV1*", "*", EquipmentType.LINE, 390, ">=", noCountries);
        String lineForm4 = genFormContingencyList("NHV1*", "*", EquipmentType.LINE, 390, "<=", noCountries);
        String lineForm5 = genFormContingencyList("*", "*", EquipmentType.LINE, 100, ">", noCountries);
        String lineForm6 = genFormContingencyList("UNKNOWN", "NVH1*", EquipmentType.LINE, 100, ">", noCountries);
        String lineForm7 = genFormContingencyList("*", "*", EquipmentType.LINE, -1, ">", france);

        testExportContingencies(lineForm, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm1, " []", NETWORK_UUID);
        testExportContingencies(lineForm2, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm3, " []", NETWORK_UUID);
        testExportContingencies(lineForm4, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm5, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm6, " []", NETWORK_UUID);
        testExportContingencies(lineForm7, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        String twtForm0 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtForm1 = "{\"equipmentID\": \"NGEN_NHV1\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtForm2 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"NGEN_NHV1\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"*\"}";
        String twtForm3 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"TWO_WINDINGS_TRANSFORMER\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \">\"}";
        String twtForm4 = genFormContingencyList("*", "*", EquipmentType.TWO_WINDINGS_TRANSFORMER, -1, ">", france);
        testExportContingencies(twtForm0, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(twtForm1, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(twtForm2, " []", NETWORK_UUID);
        testExportContingencies(twtForm3, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(twtForm4, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        String generatorForm1 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorForm2 = "{\"equipmentID\": \"GEN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorForm3 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"GEN\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String generatorForm4 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"*\", \"equipmentType\": \"GENERATOR\", \"nominalVoltage\": \"10\",\"nominalVoltageOperator\": \"<\"}";
        String generatorForm5 = genFormContingencyList("*", "*", EquipmentType.GENERATOR, -1, ">", france);
        String generatorForm6 = genFormContingencyList("*", "*", EquipmentType.GENERATOR, -1, ">", belgium);
        testExportContingencies(generatorForm1, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);

        // test export on specific variant where generator 'GEN2' has been removed
        testExportContingencies(generatorForm1, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID, VARIANT_ID_1);
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        testExportContingencies(generatorForm2, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies(generatorForm3, " []", NETWORK_UUID);
        testExportContingencies(generatorForm4, " []", NETWORK_UUID);
        testExportContingencies(generatorForm5, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies(generatorForm6, " []", NETWORK_UUID);

        String svcForm1 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcForm2 = "{\"equipmentID\": \"SVC3\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcForm3 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"SVC2*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String svcForm4 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"STATIC_VAR_COMPENSATOR\", \"nominalVoltage\": \"100\",\"nominalVoltageOperator\": \"<\"}";
        String svcForm5 = genFormContingencyList("*", "*", EquipmentType.STATIC_VAR_COMPENSATOR, -1, "<", france);
        String svcForm6 = genFormContingencyList("*", "*", EquipmentType.STATIC_VAR_COMPENSATOR, -1, "<", belgium);
        testExportContingencies(svcForm1, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}," +
                "{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies(svcForm2, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies(svcForm3, " []", NETWORK_UUID_3);
        testExportContingencies(svcForm4, " []", NETWORK_UUID_3);
        testExportContingencies(svcForm5, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]},{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies(svcForm6, " []", NETWORK_UUID_3);
    }

    @Test
    public void testExportContingencies2() throws Exception {
        String scForm1 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scForm2 = "{\"equipmentID\": \"SHUNT*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scForm3 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"SHUNT*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String scForm4 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"SHUNT_COMPENSATOR\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"=\"}";
        testExportContingencies(scForm1, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies(scForm2, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies(scForm3, " []", NETWORK_UUID_4);
        testExportContingencies(scForm4, " []", NETWORK_UUID_4);

        String hvdcForm1 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcForm2 = "{\"equipmentID\": \"L*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcForm3 = "{\"equipmentID\": \"UNKNOWN\", \"equipmentName\": \"L*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcForm4 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"400\",\"nominalVoltageOperator\": \"=\"}";
        String hvdcForm5 = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"HVDC_LINE\", \"nominalVoltage\": \"300\",\"nominalVoltageOperator\": \"<\"}";
        testExportContingencies(hvdcForm1, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcForm2, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcForm3, " []", NETWORK_UUID_2);
        testExportContingencies(hvdcForm4, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcForm5, " []", NETWORK_UUID_2);

        String bbsForm = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"BUSBAR_SECTION\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        testExportContingencies(bbsForm, " []", NETWORK_UUID);

        String dlForm = "{\"equipmentID\": \"*\", \"equipmentName\": \"*\", \"equipmentType\": \"DANGLING_LINE\", \"nominalVoltage\": \"-1\",\"nominalVoltageOperator\": \"=\"}";
        testExportContingencies(dlForm, " []", NETWORK_UUID);
    }

    void compareFormContingencyList(FormContingencyList expected, FormContingencyList current) throws JsonProcessingException {
        if (null == expected.getCountries()) {
            assertTrue(current.getCountries().isEmpty());
        } else {
            assertEquals(expected.getCountries(), current.getCountries());
        }
        assertEquals(expected.getEquipmentName(), current.getEquipmentName());
        assertEquals(expected.getEquipmentType(), current.getEquipmentType());
        assertEquals(expected.getEquipmentID(), current.getEquipmentID());
        assertTrue(Math.abs(expected.getNominalVoltage() - current.getNominalVoltage()) < EPSILON);
        assertEquals(expected.getNominalVoltageOperator(), current.getNominalVoltageOperator());
    }

    @Test
    public void modifyFormContingencyList() throws Exception {
        UUID id = addNewFormContingencyList(genFormContingencyList("equiId", "equiName", EquipmentType.LINE,
                10, "=>",
                Collections.emptySet()));

        String newFilter = genFormContingencyList("equiIdBis", "equiNameBis", EquipmentType.LINE,
                12, "<=",
                Collections.emptySet());

        mvc.perform(put("/" + VERSION + "/form-contingency-lists/" + id)
                .content(newFilter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        String res = mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + id)
                .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        compareFormContingencyList(objectMapper.readValue(newFilter, FormContingencyList.class),
                objectMapper.readValue(res, FormContingencyList.class));

        mvc.perform(put("/" + VERSION + "/form-contingency-lists/" + UUID.randomUUID())
                .content(newFilter)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void modifyScriptContingencyList() throws Exception {
        UUID id = addNewScriptContingencyList("{ \n" +
                "\"script\" : \"contingency('NHV1_NHV2_1') {" +
                "     equipments 'NHV1_NHV2_1'}\"" +
                "}");

        String newScript = "{\n" +
                "\"script\" : \"contingency('NHV1_NHV2_2') {" +
                "     equipments 'NHV1_NHV2_2'}\"" +
                "}";

        mvc.perform(put("/" + VERSION + "/script-contingency-lists/" + id)
                .content(newScript)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        String res = mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + id)
                .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        compareScriptList(objectMapper.readValue(newScript, ScriptContingencyList.class),
                objectMapper.readValue(res, ScriptContingencyList.class));

        mvc.perform(put("/" + VERSION + "/script-contingency-lists/" + UUID.randomUUID())
                .content(newScript)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private void compareScriptList(ScriptContingencyList expected, ScriptContingencyList current) {
        assertEquals(expected.getScript(), current.getScript());
    }

    private void testExportContingencies(String content, String expectedContent, UUID networkId) throws Exception {
        testExportContingencies(content, expectedContent, networkId, null);
    }

    private void testExportContingencies(String content, String expectedContent, UUID networkId, String variantId) throws Exception {
        // put the data
        UUID formContingencyListId = addNewFormContingencyList(content);

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + formContingencyListId + "/export?networkUuid=" + networkId + (variantId != null ? "&variantId=" + variantId : ""))
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(expectedContent));
        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + formContingencyListId))
                .andExpect(status().isOk());
    }

    @Test
    public void emptyScriptTest() throws Exception {
        UUID id = addNewScriptContingencyList("{" +
                "\"script\" : \"\"}");

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + id + "/export")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]"));

        ContingencyListAttributes attributes = getMetadata(id);
        assertEquals(attributes.getId(), id);
    }

    private ContingencyListAttributes getMetadata(UUID id) throws Exception {
        var res = mvc.perform(get("/" + VERSION + "/contingency-lists/metadata?ids=" + id))
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
        String lineFilters = genFormContingencyList("*", "*", EquipmentType.LINE, -1, "=", Collections.emptySet());
        try {
            testExportContingencies(lineFilters, "", id);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Network '7928181c-7977-4592-ba19-88027e4254e8' not found", e.getMessage());
    }

    @Test
    public void testExportContingencies4() {
        Throwable e = null;
        String lineFilters = genFormContingencyList("*", "*", EquipmentType.LINE, 200, "$", Collections.emptySet());
        try {
            testExportContingencies(lineFilters, "", NETWORK_UUID);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Unknown nominal voltage operator", e.getMessage());
    }

    @Test
    public void contingencyListAttributesTest() {
        UUID contingencyListAttrId = UUID.randomUUID();
        ContingencyListAttributes contingencyListAttributes = new ContingencyListAttributes(contingencyListAttrId, ContingencyListType.SCRIPT, null, null);
        assertEquals(contingencyListAttrId, contingencyListAttributes.getId());
        assertEquals(ContingencyListType.SCRIPT, contingencyListAttributes.getType());
        ContingencyListAttributes contingencyListAttributes2 = new ContingencyListAttributes();
        assertNull(contingencyListAttributes2.getId());
        assertNull(contingencyListAttributes2.getType());
    }

    @Test
    public void scriptContingencyListEntityTest() {
        ScriptContingencyListEntity entity = new ScriptContingencyListEntity();
        entity.setScript("");

        assertEquals("", entity.getScript());
    }

    @Test
    public void formContingencyListEntityTest() {
        FormContingencyListEntity entity = new FormContingencyListEntity();
        entity.setEquipmentId("id1");
        entity.setEquipmentName("name1");
        entity.setEquipmentType("LINE");
        entity.setNominalVoltage(225.);
        entity.setNominalVoltageOperator("=");
        entity.setCountries(Set.of("FRANCE", "ITALY"));

        assertEquals("id1", entity.getEquipmentId());
        assertEquals("name1", entity.getEquipmentName());
        assertEquals("LINE", entity.getEquipmentType());
        assertEquals(225., entity.getNominalVoltage(), 0.1);
        assertEquals("=", entity.getNominalVoltageOperator());
        assertTrue(entity.getCountries().contains("FRANCE"));
        assertTrue(entity.getCountries().contains("ITALY"));
    }

    @Test
    public void replaceFormWithScriptTest() throws Exception {
        String form = "{\n" +
                "  \"equipmentID\": \"GEN*\"," +
                "  \"equipmentName\": \"GEN*\"," +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": \"100\"," +
                "  \"nominalVoltageOperator\": \">\"," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        // Put data
        UUID id = addNewFormContingencyList(form);

        // replace with groovy script
        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists/" + id + "/replace-with-script"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID newId = objectMapper.readValue(res, ScriptContingencyList.class).getId();

        // check form list tic not found
        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + id)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // check script tic is found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + newId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }

    @Test
    public void copyFormToScriptTest() throws Exception {
        String form = new StringJoiner(",\n", "{\n", "\n}")
                .add("  \"equipmentID\": \"GEN*\"")
                .add("  \"equipmentName\": \"GEN*\"")
                .add("  \"equipmentType\": \"GENERATOR\"")
                .add("  \"nominalVoltage\": \"100\"")
                .add("  \"nominalVoltageOperator\": \">\"")
                .add("  \"countries\": [\"FR\", \"BE\"]")
                .toString();

        // Put data
        UUID firstUUID = addNewFormContingencyList(form);

        // new script from form
        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists/" + firstUUID + "/new-script"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID newId = objectMapper.readValue(res, ScriptContingencyList.class).getId();

        // check script newId is found
        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + newId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        // check form list firstUUID found
        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + firstUUID)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void addScriptWithIdTest() throws Exception {
        UUID id = UUID.fromString("abcdef01-1234-5678-abcd-e123456789aa");

        String script = "{ \n" +
                "\"script\" : \"contingency('NHV1_NHV2_1') {" +
                "     equipments 'NHV1_NHV2_1'}\"" +
                "}";

        UUID scriptId = addNewScriptContingencyListWithId(script, id);
        assertEquals(scriptId, id);
    }
}
