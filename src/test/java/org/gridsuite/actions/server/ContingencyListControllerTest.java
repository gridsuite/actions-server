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
import org.gridsuite.actions.server.entities.NumericalFilterEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.FormContingencyListRepository;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;
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
import static org.gridsuite.actions.server.utils.NumericalFilterOperator.*;
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
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"GREATER_THAN\"," +
                "    \"value1\": \"100\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        String formContingencyList2 = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"LESS_OR_EQUAL\"," +
                "    \"value1\": \"225\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": [\"FR\", \"IT\", \"NL\"]" +
                "}";

        String formContingencyList3 = "{\n" +
                "  \"equipmentType\": \"LOAD\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"EQUALITY\"," +
                "    \"value1\": \"380\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": []" +
                "}";

        UUID scriptId = addNewScriptContingencyList(script);

        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists/")
                .content(formContingencyList)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID ticId = objectMapper.readValue(res, FormContingencyList.class).getId();

        // check first form insert
        mvc.perform(get("/" + VERSION + "/form-contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"equipmentType\":\"GENERATOR\",\"nominalVoltage1\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries2\":[],\"type\":\"FORM\"}]", false));

        mvc.perform(post("/" + VERSION + "/form-contingency-lists/")
                .content(formContingencyList2)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

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
                .andExpect(content().json("[{\"equipmentType\":\"GENERATOR\",\"nominalVoltage1\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries2\":[],\"type\":\"FORM\"},{" +
                        "\"equipmentType\":\"LINE\",\"nominalVoltage1\":{\"type\":\"LESS_OR_EQUAL\",\"value1\":225.0,\"value2\":null},\"nominalVoltage2\":null,\"countries\":[\"IT\",\"FR\",\"NL\"],\"countries2\":[],\"type\":\"FORM\"},{" +
                        "\"equipmentType\":\"LOAD\",\"nominalVoltage1\":{\"type\":\"EQUALITY\",\"value1\":380.0,\"value2\":null},\"nominalVoltage2\":null,\"countries\":[],\"countries2\":[],\"type\":\"FORM\"}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + scriptId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\",\"type\":\"SCRIPT\"}", false));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + ticId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"equipmentType\":\"GENERATOR\",\"nominalVoltage1\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries2\":[],\"type\":\"FORM\"}", false));

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

    public String genFormContingencyList(EquipmentType type,
                                         Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        String jsonData = "{" + jsonVal("equipmentType", type.name(), true);
        if (nominalVoltage == -1.) {
            jsonData += "\"nominalVoltage1\": null,";
        } else {
            jsonData += "\"nominalVoltage1\": {"
                    + jsonVal("type", nominalVoltageOperator.name(), true)
                    + jsonVal("value1", nominalVoltage, false)
                    + "},";
        }
        jsonData += "\"countries\": [" + (!countries.isEmpty() ? "\"" + join(countries, "\",\"") + "\"" : "") + "]}";
        return jsonData;
    }

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDateFormContingencyList() throws Exception {
        String list = genFormContingencyList(EquipmentType.LINE, 11., EQUALITY, Set.of());

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
        String lineForm = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, noCountries);
        String lineForm1 = genFormContingencyList(EquipmentType.LINE, 100., LESS_THAN, noCountries);
        String lineForm2 = genFormContingencyList(EquipmentType.LINE, 380., EQUALITY, noCountries);
        String lineForm3 = genFormContingencyList(EquipmentType.LINE, 390., GREATER_OR_EQUAL, noCountries);
        String lineForm4 = genFormContingencyList(EquipmentType.LINE, 390., LESS_OR_EQUAL, noCountries);
        String lineForm5 = genFormContingencyList(EquipmentType.LINE, 100., GREATER_THAN, noCountries);
        String lineForm6 = genFormContingencyList(EquipmentType.LINE, -1., GREATER_THAN, france);

        testExportContingencies(lineForm, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm1, " []", NETWORK_UUID);
        testExportContingencies(lineForm2, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm3, " []", NETWORK_UUID);
        testExportContingencies(lineForm4, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm5, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm6, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        String twtForm0 = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., EQUALITY, noCountries);
        String twtForm1 = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 10., GREATER_THAN, noCountries);
        String twtForm2 = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, france);
        testExportContingencies(twtForm0, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(twtForm1, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);
        testExportContingencies(twtForm2, " [{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"BRANCH\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"BRANCH\"}]}]", NETWORK_UUID);

        String generatorForm1 = genFormContingencyList(EquipmentType.GENERATOR, -1., EQUALITY, noCountries);
        String generatorForm4 = genFormContingencyList(EquipmentType.GENERATOR, 10., LESS_THAN, noCountries);
        String generatorForm5 = genFormContingencyList(EquipmentType.GENERATOR, -1., GREATER_THAN, france);
        String generatorForm6 = genFormContingencyList(EquipmentType.GENERATOR, -1., GREATER_THAN, belgium);
        testExportContingencies(generatorForm1, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);

        // test export on specific variant where generator 'GEN2' has been removed
        testExportContingencies(generatorForm1, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID, VARIANT_ID_1);
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        testExportContingencies(generatorForm4, " []", NETWORK_UUID);
        testExportContingencies(generatorForm5, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies(generatorForm6, " []", NETWORK_UUID);

        String svcForm1 = genFormContingencyList(EquipmentType.STATIC_VAR_COMPENSATOR, -1., EQUALITY, noCountries);
        String svcForm4 = genFormContingencyList(EquipmentType.STATIC_VAR_COMPENSATOR, 100., LESS_THAN, noCountries);
        String svcForm5 = genFormContingencyList(EquipmentType.STATIC_VAR_COMPENSATOR, -1., LESS_THAN, france);
        String svcForm6 = genFormContingencyList(EquipmentType.STATIC_VAR_COMPENSATOR, -1., LESS_THAN, belgium);
        testExportContingencies(svcForm1, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}," +
                "{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies(svcForm4, " []", NETWORK_UUID_3);
        testExportContingencies(svcForm5, " [{\"id\":\"SVC3\",\"elements\":[{\"id\":\"SVC3\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]},{\"id\":\"SVC2\",\"elements\":[{\"id\":\"SVC2\",\"type\":\"STATIC_VAR_COMPENSATOR\"}]}]", NETWORK_UUID_3);
        testExportContingencies(svcForm6, " []", NETWORK_UUID_3);
    }

    @Test
    public void testExportContingencies2() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        String scForm1 = genFormContingencyList(EquipmentType.SHUNT_COMPENSATOR, -1., EQUALITY, noCountries);
        String scForm4 = genFormContingencyList(EquipmentType.SHUNT_COMPENSATOR, 300., EQUALITY, noCountries);
        testExportContingencies(scForm1, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies(scForm4, " []", NETWORK_UUID_4);

        String hvdcForm1 = genFormContingencyList(EquipmentType.HVDC_LINE, -1., EQUALITY, noCountries);
        String hvdcForm4 = genFormContingencyList(EquipmentType.HVDC_LINE, 400., EQUALITY, noCountries);
        String hvdcForm5 = genFormContingencyList(EquipmentType.HVDC_LINE, 300., LESS_THAN, noCountries);
        testExportContingencies(hvdcForm1, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcForm4, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcForm5, " []", NETWORK_UUID_2);

        String bbsForm = genFormContingencyList(EquipmentType.BUSBAR_SECTION, -1., EQUALITY, noCountries);
        testExportContingencies(bbsForm, " []", NETWORK_UUID);

        String dlForm = genFormContingencyList(EquipmentType.DANGLING_LINE, -1., EQUALITY, noCountries);
        testExportContingencies(dlForm, " []", NETWORK_UUID);
    }

    void compareFormContingencyList(FormContingencyList expected, FormContingencyList current) {
        if (null == expected.getCountries()) {
            assertTrue(current.getCountries().isEmpty());
        } else {
            assertEquals(expected.getCountries(), current.getCountries());
        }
        assertEquals(expected.getEquipmentType(), current.getEquipmentType());
        if (expected.getNominalVoltage1() == null || current.getNominalVoltage1() == null) {
            assertNull(expected.getNominalVoltage1());
            assertNull(current.getNominalVoltage1());
        } else {
            assertEquals(expected.getNominalVoltage1().getValue1(), current.getNominalVoltage1().getValue1());
        }
    }

    @Test
    public void modifyFormContingencyList() throws Exception {
        UUID id = addNewFormContingencyList(genFormContingencyList(EquipmentType.LINE,
                10., GREATER_OR_EQUAL,
                Collections.emptySet()));

        String newFilter = genFormContingencyList(EquipmentType.LINE,
                12., LESS_OR_EQUAL,
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
        String lineFilters = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, Collections.emptySet());
        try {
            testExportContingencies(lineFilters, "", id);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof NestedServletException);
        assertEquals("Request processing failed; nested exception is com.powsybl.commons.PowsyblException: Network '7928181c-7977-4592-ba19-88027e4254e8' not found", e.getMessage());
    }

    @Test
    public void testExportContingenciesBadOperator() throws Exception {
        String lineFilters = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"BAD_OP\"," +
                "    \"value1\": \"100\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        mvc.perform(post("/" + VERSION + "/form-contingency-lists/")
                        .content(lineFilters)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
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
        entity.setEquipmentType("LINE");
        entity.setNominalVoltage1(new NumericalFilterEntity(null, EQUALITY, 225., null));
        entity.setCountries(Set.of("FRANCE", "ITALY"));

        assertEquals("LINE", entity.getEquipmentType());
        assertEquals(225., entity.getNominalVoltage1().getValue1(), 0.1);
        assertEquals(EQUALITY, entity.getNominalVoltage1().getOperator());
        assertTrue(entity.getCountries().contains("FRANCE"));
        assertTrue(entity.getCountries().contains("ITALY"));
    }

    @Test
    public void replaceFormWithScriptTest() throws Exception {
        String form = "{\n" +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"GREATER_THAN\"," +
                "    \"value1\": \"100\"," +
                "    \"value2\": \"null\"" +
                "  }," +
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
        String form = "{\n" +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"GREATER_THAN\"," +
                "    \"value1\": \"100\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";
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

    @Test
    public void duplicateFormContingencyList() throws Exception {
        String list = genFormContingencyList(EquipmentType.LINE, 11., EQUALITY, Set.of());
        UUID id = addNewFormContingencyList(list);

        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists?duplicateFrom=" + id + "&id=" + UUID.randomUUID()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String returnedEquipmentType = objectMapper.readValue(res, FormContingencyList.class).getEquipmentType();
        assertEquals(returnedEquipmentType, EquipmentType.LINE.toString());

        mvc.perform(post("/" + VERSION + "/form-contingency-lists?duplicateFrom=" + UUID.randomUUID() + "&id=" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void duplicateScriptContingencyList() throws Exception {
        String script = "{ " + "\"script\" : \"contingency('NHV1_NHV2_1') {" +
                "     equipments 'NHV1_NHV2_1'}\"" +
                "}";
        UUID id = addNewScriptContingencyList(script.strip());

        String res = mvc.perform(post("/" + VERSION + "/script-contingency-lists?duplicateFrom=" + id + "&id=" + UUID.randomUUID()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String returnedScript = objectMapper.readValue(res, ScriptContingencyList.class).getScript();
        assertTrue(returnedScript.contains("equipments 'NHV1_NHV2_1'"));

        mvc.perform(post("/" + VERSION + "/script-contingency-lists?duplicateFrom=" + UUID.randomUUID() + "&id=" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
