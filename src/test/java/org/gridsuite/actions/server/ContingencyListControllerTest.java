/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import com.powsybl.contingency.contingency.list.identifier.IdBasedNetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifier;
import com.powsybl.contingency.contingency.list.identifier.NetworkElementIdentifierList;
import com.powsybl.contingency.json.ContingencyJsonModule;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.ShuntTestCaseFactory;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import jakarta.servlet.ServletException;
import org.gridsuite.actions.server.dto.*;
import org.gridsuite.actions.server.entities.FormContingencyListEntity;
import org.gridsuite.actions.server.entities.NumericalFilterEntity;
import org.gridsuite.actions.server.entities.ScriptContingencyListEntity;
import org.gridsuite.actions.server.repositories.FormContingencyListRepository;
import org.gridsuite.actions.server.repositories.IdBasedContingencyListRepository;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;
import org.gridsuite.actions.utils.MatcherJson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;
import java.util.stream.Collectors;

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
@ContextConfiguration(classes = {ActionsApplication.class, TestChannelBinderConfiguration.class})
public class ContingencyListControllerTest {

    private static final long TIMEOUT = 1000;

    private static final UUID NETWORK_UUID = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
    private static final UUID NETWORK_UUID_2 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e5");
    private static final UUID NETWORK_UUID_3 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e6");
    private static final UUID NETWORK_UUID_4 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e7");
    private static final UUID NETWORK_UUID_5 = UUID.fromString("0313daa6-9419-4d4f-8ed1-af555998665f");
    private static final String VARIANT_ID_1 = "variant_1";
    private static final String USER_ID_HEADER = "userId";

    private String elementUpdateDestination = "element.update";

    private Network network;

    private static final double EPSILON = .001;

    @Autowired
    private ScriptContingencyListRepository scriptContingencyListRepository;

    @Autowired
    private FormContingencyListRepository formContingencyListRepository;

    @Autowired
    private IdBasedContingencyListRepository idBasedContingencyListRepository;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NetworkStoreService networkStoreService;

    @Autowired
    private OutputDestination output;

    @After
    public void tearDown() {
        List<String> destinations = List.of(elementUpdateDestination);

        cleanDB();
        assertQueuesEmptyThenClear(destinations, output);
    }

    private void cleanDB() {
        scriptContingencyListRepository.deleteAll();
        formContingencyListRepository.deleteAll();
        idBasedContingencyListRepository.deleteAll();
    }

    private void assertQueuesEmptyThenClear(List<String> destinations, OutputDestination output) {
        try {
            destinations.forEach(destination -> assertNull("Should not be any messages in queue " + destination + " : ", output.receive(TIMEOUT, destination)));
        } catch (NullPointerException e) {
            // Ignoring
        } finally {
            output.clear(); // purge in order to not fail the other tests
        }
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
        Network network5 = EurostagTutorialExample1Factory.createWithFixedCurrentLimits(new NetworkFactoryImpl());
        given(networkStoreService.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreService.getNetwork(NETWORK_UUID_2, PreloadingStrategy.COLLECTION)).willReturn(network2);
        given(networkStoreService.getNetwork(NETWORK_UUID_3, PreloadingStrategy.COLLECTION)).willReturn(network3);
        given(networkStoreService.getNetwork(NETWORK_UUID_4, PreloadingStrategy.COLLECTION)).willReturn(network4);
        given(networkStoreService.getNetwork(NETWORK_UUID_5, PreloadingStrategy.COLLECTION)).willReturn(network5);

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModule(new ContingencyJsonModule());

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
                "  \"nominalVoltage\": {" +
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
                "  \"countries1\": [\"FR\", \"IT\", \"NL\"]" +
                "}";

        // LOAD is not a supported type => error case
        String formContingencyListError = "{\n" +
                "  \"equipmentType\": \"LOAD\"," +
                "  \"nominalVoltage\": {" +
                "    \"type\": \"EQUALITY\"," +
                "    \"value1\": \"380\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": []" +
                "}";

        UUID scriptId = addNewScriptContingencyList(script);

        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                .content(formContingencyList)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID ticId = objectMapper.readValue(res, FormContingencyList.class).getId();

        // check first form insert
        mvc.perform(get("/" + VERSION + "/form-contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries1\":[], \"countries2\":[],\"metadata\":{\"type\":\"FORM\"}}]", false));

        mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                .content(formContingencyList2)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Throwable e = null;
        try {
            mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                            .content(formContingencyListError)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk());
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof ServletException);

        // Check data
        mvc.perform(get("/" + VERSION + "/contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"type\":\"SCRIPT\"},{\"type\":\"FORM\"},{\"type\":\"FORM\"}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\", \"metadata\":{\"type\":\"SCRIPT\"}}]", false));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries1\":[],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}},{" +
                        "\"equipmentType\":\"LINE\",\"nominalVoltage\":null,\"nominalVoltage1\":{\"type\":\"LESS_OR_EQUAL\",\"value1\":225.0,\"value2\":null},\"nominalVoltage2\":null,\"countries\":[],\"countries1\":[\"IT\",\"FR\",\"NL\"],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}}]", false));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + scriptId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\", \"metadata\":{\"type\":\"SCRIPT\"}}", false));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + ticId)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries1\":[],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}}", false));

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

    public String genFormContingencyList(EquipmentType type, Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        // single nominalVoltage => no range allowed
        assertNotEquals(nominalVoltageOperator, RANGE);

        switch (type) {
            case LINE:
                return genFormContingencyListForLine(nominalVoltage, nominalVoltageOperator, countries);
            case HVDC_LINE:
                return genFormContingencyListForHVDC(nominalVoltage, nominalVoltageOperator, countries);
            case TWO_WINDINGS_TRANSFORMER:
                return genFormContingencyListFor2WT(nominalVoltage, nominalVoltageOperator, countries);
            default:
                return genFormContingencyListForOthers(type, nominalVoltage, nominalVoltageOperator, countries);
        }
    }

    public String genFormContingencyListForLine(Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        return genFormContingencyList(EquipmentType.LINE, -1.0, -1.0, EQUALITY, nominalVoltage, -1.0, nominalVoltageOperator, -1.0, -1.0, EQUALITY, Collections.emptySet(), countries, Collections.emptySet());
    }

    public String genFormContingencyListForHVDC(Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        return genFormContingencyList(EquipmentType.HVDC_LINE, nominalVoltage, -1.0, nominalVoltageOperator, -1.0, -1.0, EQUALITY, -1.0, -1.0, EQUALITY, Collections.emptySet(), countries, Collections.emptySet());
    }

    public String genFormContingencyListFor2WT(Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        return genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, nominalVoltage, -1.0, nominalVoltageOperator, -1.0, -1.0, EQUALITY, countries, Collections.emptySet(), Collections.emptySet());
    }

    public String genFormContingencyListForOthers(EquipmentType type, Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator,
                                                  Set<String> countries) {
        return genFormContingencyList(type, nominalVoltage, -1.0, nominalVoltageOperator, -1.0, -1.0, EQUALITY, -1.0, -1.0, EQUALITY, countries, Collections.emptySet(), Collections.emptySet());
    }

    public String genFormContingencyList(EquipmentType type,
                                         Double value01, Double value02, NumericalFilterOperator operator0,
                                         Double value11, Double value12, NumericalFilterOperator operator1,
                                         Double value21, Double value22, NumericalFilterOperator operator2,
                                         Set<String> countries, Set<String> countries1, Set<String> countries2) {
        String jsonData = "{" + jsonVal("equipmentType", type.name(), true);
        // value01 == -1 => no filter on voltage-level (for all equipments except lines)
        if (value01 == -1.) {
            jsonData += "\"nominalVoltage\": null,";
        } else {
            jsonData += "\"nominalVoltage\": {"
                    + jsonVal("type", operator0.name(), true)
                    + jsonVal("value1", value01, operator0 == RANGE);
            if (operator0 == RANGE) {
                jsonData += jsonVal("value2", value02, false);
            }
            jsonData += "},";
        }
        // value11 == -1 => no first filter on voltage-level (lines)
        if (value11 == -1.) {
            jsonData += "\"nominalVoltage1\": null,";
        } else {
            jsonData += "\"nominalVoltage1\": {"
                    + jsonVal("type", operator1.name(), true)
                    + jsonVal("value1", value11, operator1 == RANGE);
            if (operator1 == RANGE) {
                jsonData += jsonVal("value2", value12, false);
            }
            jsonData += "},";
        }
        // value21 == -1 => no second filter on voltage-level (lines)
        if (value21 == -1.) {
            jsonData += "\"nominalVoltage2\": null,";
        } else {
            jsonData += "\"nominalVoltage2\": {"
                    + jsonVal("type", operator2.name(), true)
                    + jsonVal("value1", value21, operator2 == RANGE);
            if (operator2 == RANGE) {
                jsonData += jsonVal("value2", value22, false);
            }
            jsonData += "},";
        }
        jsonData += "\"countries\": [" + (!countries.isEmpty() ? "\"" + join(countries, "\",\"") + "\"" : "") + "], "; // all equipments except lines
        jsonData += "\"countries1\": [" + (!countries1.isEmpty() ? "\"" + join(countries1, "\",\"") + "\"" : "") + "], "; // lines
        jsonData += "\"countries2\": [" + (!countries2.isEmpty() ? "\"" + join(countries2, "\",\"") + "\"" : "") + "]}"; // lines
        return jsonData;
    }

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDateFormContingencyList() throws Exception {
        String userId = "userId";
        String list = genFormContingencyList(EquipmentType.LINE, 11., EQUALITY, Set.of());
        UUID id = addNewFormContingencyList(list);
        ContingencyListMetadataImpl attributes = getMetadata(id);

        assertEquals(id, attributes.getId());
        Date baseModificationDate = attributes.getModificationDate();

        mvc.perform(put("/" + VERSION + "/form-contingency-lists/" + id)
                .content(list)
                .contentType(APPLICATION_JSON)
                .header(USER_ID_HEADER, "userId"))
                .andExpect(status().isOk());

        Message<byte[]> message = output.receive(TIMEOUT, elementUpdateDestination);
        assertEquals(id, message.getHeaders().get(NotificationService.HEADER_ELEMENT_UUID));
        assertEquals(userId, message.getHeaders().get(NotificationService.HEADER_MODIFIED_BY));

        attributes = getMetadata(id);
        assertTrue(baseModificationDate.getTime() < attributes.getModificationDate().getTime());
    }

    private UUID addNewFormContingencyList(String form) throws Exception {

        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(form)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        FormContingencyList list = objectMapper.readValue(res, FormContingencyList.class);
        FormContingencyList original = objectMapper.readValue(form, FormContingencyList.class);
        compareFormContingencyList(original, list);
        return list.getId();
    }

    private UUID addNewScriptContingencyList(String script) throws Exception {
        String res = mvc.perform(post("/" + VERSION + "/script-contingency-lists")
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
    public void testExportContingenciesLine() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");

        String lineForm = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, noCountries);
        String lineForm1 = genFormContingencyList(EquipmentType.LINE, 100., LESS_THAN, noCountries);
        String lineForm2 = genFormContingencyList(EquipmentType.LINE, 380., EQUALITY, noCountries);
        String lineForm3 = genFormContingencyList(EquipmentType.LINE, 390., GREATER_OR_EQUAL, noCountries);
        String lineForm4 = genFormContingencyList(EquipmentType.LINE, 390., LESS_OR_EQUAL, noCountries);
        String lineForm5 = genFormContingencyList(EquipmentType.LINE, 100., GREATER_THAN, noCountries);
        String lineForm6 = genFormContingencyList(EquipmentType.LINE, -1., GREATER_THAN, france);

        testExportContingencies(lineForm, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"LINE\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm1, " []", NETWORK_UUID);
        testExportContingencies(lineForm2, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"LINE\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm3, " []", NETWORK_UUID);
        testExportContingencies(lineForm4, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"LINE\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm5, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"LINE\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]}]", NETWORK_UUID);
        testExportContingencies(lineForm6, " [{\"id\":\"NHV1_NHV2_2\",\"elements\":[{\"id\":\"NHV1_NHV2_2\",\"type\":\"LINE\"}]},{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]}]", NETWORK_UUID);
    }

    @Test
    public void testExportContingencies2WTransfoWith1NumFilter() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        // with this network (EurostagTutorialExample1Factory::create), we have 2 FR substations and 2 2WT Transfos:
        // - NGEN_NHV1  term1: 24 kV term2: 380 kV
        // - NHV2_NLOAD term1: 380 kV term2: 150 kV
        final String bothMatch = "[{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]}]";
        final String matchLOAD = "[{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]}]";
        final String matchGEN = "[{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]}]";
        final String noMatch = "[]";

        // single voltage filter
        String twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., EQUALITY, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 10., GREATER_THAN, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 10., EQUALITY, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 24., EQUALITY, noCountries);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 150., LESS_THAN, noCountries);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 150., LESS_OR_EQUAL, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 150., EQUALITY, noCountries);
        testExportContingencies(twtForm, matchLOAD, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 380., GREATER_THAN, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 380., GREATER_OR_EQUAL, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 400., GREATER_OR_EQUAL, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
        // range
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 0., 24., RANGE, -1., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 24., 150., RANGE, -1., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 225., 380., RANGE, -1., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 400., 500., RANGE, -1., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
    }

    @Test
    public void testExportContingencies2WTransfoWith2NumFilter() throws Exception {
        Set<String> noCountries = Collections.emptySet();

        final String matchLOAD = "[{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]}]";
        final String matchGEN = "[{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]}]";
        final String noMatch = "[]";

        // 2 voltage filters
        String twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 400., 500., RANGE, 24., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 150., 400., RANGE, 24., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 150., 400., RANGE, 150., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, matchLOAD, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 150., 400., RANGE, 33., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
    }

    @Test
    public void testExportContingencies2WTransfoWithCountryFilter() throws Exception {
        Set<String> france = Collections.singleton("FR");
        Set<String> franceAndMore = Set.of("FR", "ZA", "ES");
        Set<String> belgium = Collections.singleton("BE");
        Set<String> italy = Collections.singleton("IT");
        Set<String> belgiumAndFrance = Set.of("FR", "BE");

        final String bothMatch = "[{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]},{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]}]";
        final String matchLOAD = "[{\"id\":\"NHV2_NLOAD\",\"elements\":[{\"id\":\"NHV2_NLOAD\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]}]";
        final String matchGEN = "[{\"id\":\"NGEN_NHV1\",\"elements\":[{\"id\":\"NGEN_NHV1\",\"type\":\"TWO_WINDINGS_TRANSFORMER\"}]}]";
        final String noMatch = "[]";

        String twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, france);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, franceAndMore);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, belgium);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);

        // NETWORK_UUID5 : a 2-country network (one substation FR, one BE)
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, belgium);
        testExportContingencies(twtForm, matchLOAD, NETWORK_UUID_5);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, france);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID_5);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, belgiumAndFrance);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID_5);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, italy);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID_5);
    }

    @Test
    public void testExportContingenciesGenerator() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");
        Set<String> belgium = Collections.singleton("BE");

        String generatorForm1 = genFormContingencyList(EquipmentType.GENERATOR, -1., EQUALITY, france);
        String generatorForm4 = genFormContingencyList(EquipmentType.GENERATOR, 10., LESS_THAN, noCountries);
        String generatorForm5 = genFormContingencyList(EquipmentType.GENERATOR, -1., GREATER_THAN, france);
        String generatorForm6 = genFormContingencyList(EquipmentType.GENERATOR, -1., GREATER_THAN, belgium);
        System.out.println("generatorForm1=>" + generatorForm1);
        System.out.println("generatorForm4=>" + generatorForm4);
        System.out.println("generatorForm5=>" + generatorForm5);
        System.out.println("generatorForm6=>" + generatorForm6);
        testExportContingencies(generatorForm1, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);

        // test export on specific variant where generator 'GEN2' has been removed
        testExportContingencies(generatorForm1, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID, VARIANT_ID_1);
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        testExportContingencies(generatorForm4, " []", NETWORK_UUID);
        testExportContingencies(generatorForm5, " [{\"id\":\"GEN\",\"elements\":[{\"id\":\"GEN\",\"type\":\"GENERATOR\"}]},{\"id\":\"GEN2\",\"elements\":[{\"id\":\"GEN2\",\"type\":\"GENERATOR\"}]}]", NETWORK_UUID);
        testExportContingencies(generatorForm6, " []", NETWORK_UUID);
    }

    @Test
    public void testExportContingenciesSVC() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");
        Set<String> belgium = Collections.singleton("BE");

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
    public void testExportContingenciesShuntCompensator() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        String scForm1 = genFormContingencyList(EquipmentType.SHUNT_COMPENSATOR, -1., EQUALITY, noCountries);
        String scForm4 = genFormContingencyList(EquipmentType.SHUNT_COMPENSATOR, 300., EQUALITY, noCountries);
        testExportContingencies(scForm1, " [{\"id\":\"SHUNT\",\"elements\":[{\"id\":\"SHUNT\",\"type\":\"SHUNT_COMPENSATOR\"}]}]", NETWORK_UUID_4);
        testExportContingencies(scForm4, " []", NETWORK_UUID_4);
    }

    @Test
    public void testExportContingenciesHVDC() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        String hvdcForm1 = genFormContingencyList(EquipmentType.HVDC_LINE, -1., EQUALITY, noCountries);
        String hvdcForm4 = genFormContingencyList(EquipmentType.HVDC_LINE, 400., EQUALITY, noCountries);
        String hvdcForm5 = genFormContingencyList(EquipmentType.HVDC_LINE, 300., LESS_THAN, noCountries);
        testExportContingencies(hvdcForm1, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcForm4, " [{\"id\":\"L\",\"elements\":[{\"id\":\"L\",\"type\":\"HVDC_LINE\"}]}]", NETWORK_UUID_2);
        testExportContingencies(hvdcForm5, " []", NETWORK_UUID_2);
    }

    @Test
    public void testExportContingenciesBusBar() throws Exception {
        Set<String> noCountries = Collections.emptySet();

        String bbsForm = genFormContingencyList(EquipmentType.BUSBAR_SECTION, -1., EQUALITY, noCountries);
        testExportContingencies(bbsForm, " []", NETWORK_UUID);
    }

    @Test
    public void testExportContingenciesDanglingLine() throws Exception {
        Set<String> noCountries = Collections.emptySet();

        String dlForm = genFormContingencyList(EquipmentType.DANGLING_LINE, -1., EQUALITY, noCountries);
        testExportContingencies(dlForm, " []", NETWORK_UUID);
    }

    void compareFormContingencyList(FormContingencyList expected, FormContingencyList current) {
        // Ideally we shouldn't do that... it's because in the app null <=> [] for country list
        Set<String> expectedCountries = expected.getCountries();
        if (expectedCountries == null) {
            expectedCountries = Collections.emptySet();
        }
        Set<String> currentCountries = current.getCountries();
        if (currentCountries == null) {
            currentCountries = Collections.emptySet();
        }
        Set<String> expectedCountries1 = expected.getCountries1();
        if (expectedCountries1 == null) {
            expectedCountries1 = Collections.emptySet();
        }
        Set<String> currentCountries1 = current.getCountries1();
        if (currentCountries1 == null) {
            currentCountries1 = Collections.emptySet();
        }
        assertEquals(expectedCountries, currentCountries);
        assertEquals(expectedCountries1, currentCountries1);
        assertEquals(expected.getEquipmentType(), current.getEquipmentType());
        if (expected.getNominalVoltage() == null || current.getNominalVoltage() == null) {
            assertNull(expected.getNominalVoltage());
            assertNull(current.getNominalVoltage());
        } else {
            assertEquals(expected.getNominalVoltage().getValue1(), current.getNominalVoltage().getValue1());
        }
        if (expected.getNominalVoltage1() == null || current.getNominalVoltage1() == null) {
            assertNull(expected.getNominalVoltage1());
            assertNull(current.getNominalVoltage1());
        } else {
            assertEquals(expected.getNominalVoltage1().getValue1(), current.getNominalVoltage1().getValue1());
        }
    }

    @Test
    public void modifyFormContingencyList() throws Exception {
        String userId = "userId";
        UUID id = addNewFormContingencyList(genFormContingencyList(EquipmentType.LINE,
                10., GREATER_OR_EQUAL,
                Collections.emptySet()));

        String newFilter = genFormContingencyList(EquipmentType.LINE,
                12., LESS_OR_EQUAL,
                Collections.emptySet());

        mvc.perform(put("/" + VERSION + "/form-contingency-lists/" + id)
                .content(newFilter)
                .contentType(APPLICATION_JSON)
                .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk());

        Message<byte[]> message = output.receive(TIMEOUT, elementUpdateDestination);
        assertEquals(id, message.getHeaders().get(NotificationService.HEADER_ELEMENT_UUID));
        assertEquals(userId, message.getHeaders().get(NotificationService.HEADER_MODIFIED_BY));

        String res = mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + id)
                        .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        compareFormContingencyList(objectMapper.readValue(newFilter, FormContingencyList.class),
                objectMapper.readValue(res, FormContingencyList.class));

        mvc.perform(put("/" + VERSION + "/form-contingency-lists/" + UUID.randomUUID())
                .content(newFilter)
                .contentType(APPLICATION_JSON)
                .header(USER_ID_HEADER, userId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void modifyScriptContingencyList() throws Exception {
        String userId = "userId";
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
                .contentType(APPLICATION_JSON)
                .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk());

        Message<byte[]> message = output.receive(TIMEOUT, elementUpdateDestination);
        assertEquals(id, message.getHeaders().get(NotificationService.HEADER_ELEMENT_UUID));
        assertEquals(userId, message.getHeaders().get(NotificationService.HEADER_MODIFIED_BY));

        String res = mvc.perform(get("/" + VERSION + "/script-contingency-lists/" + id)
                .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        compareScriptList(objectMapper.readValue(newScript, ScriptContingencyList.class),
                objectMapper.readValue(res, ScriptContingencyList.class));

        mvc.perform(put("/" + VERSION + "/script-contingency-lists/" + UUID.randomUUID())
                .content(newScript)
                .contentType(APPLICATION_JSON)
                .header(USER_ID_HEADER, userId))
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
        // search matching equipments
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
    public void testExportContingenciesInfos() throws Exception {
        ScriptContingencyList scriptContingencyList = new ScriptContingencyList("contingency('NHV1_NHV2_1') {\n" +
                "    equipments 'NHV1_NHV2_1'}\n" +
                "contingency('TEST2') {\n" +
                "    equipments 'TEST2'}");

        UUID scriptContingencyId = addNewScriptContingencyList(objectMapper.writeValueAsString(scriptContingencyList));

        mvc.perform(get("/" + VERSION + "/contingency-lists/contingency-infos/" + scriptContingencyId + "/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"contingency\":{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]},\"notFoundElements\":null},{\"id\":\"TEST2\",\"contingency\":null,\"notFoundElements\":[\"TEST2\"]}]"));
        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + scriptContingencyId))
                .andExpect(status().isOk());

        Date date = new Date();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, date, "NHV1_NHV2_1", "Test");
        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        mvc.perform(get("/" + VERSION + "/contingency-lists/contingency-infos/" + contingencyListId + "/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"contingency\":{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]},\"notFoundElements\":null},{\"id\":\"Test\",\"contingency\":null,\"notFoundElements\":[\"Test\"]}]"));
        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
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

        ContingencyListMetadataImpl attributes = getMetadata(id);
        assertEquals(attributes.getId(), id);
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
    public void testExportContingencies3() {
        Throwable e = null;
        UUID id = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e8");
        String lineFilters = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, Collections.emptySet());
        try {
            testExportContingencies(lineFilters, "", id);
        } catch (Throwable ex) {
            e = ex;
        }
        assertTrue(e instanceof ServletException);
        assertEquals("Request processing failed: com.powsybl.commons.PowsyblException: Network '7928181c-7977-4592-ba19-88027e4254e8' not found", e.getMessage());
    }

    @Test
    public void testCreateContingencyBadOperator() throws Exception {
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

    @Test
    public void testCreateContingencyNoValue1() throws Exception {
        String formContingencyList = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"EQUALITY\"," +
                "    \"value1\": \"null\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"nominalVoltage2\": {" +
                "    \"type\": \"LESS_OR_EQUAL\"," +
                "    \"value1\": \"null\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries1\": [\"FR\", \"BE\"]" +
                "}";
        // creation
        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(formContingencyList)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID clId = objectMapper.readValue(res, FormContingencyList.class).getId();
        // retrieve it, no numeric filters created (because of null values)
        String noNominalFilter1Response = "{\"equipmentType\":\"LINE\",\"nominalVoltage\":null,\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[],\"countries1\":[\"BE\",\"FR\"],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}}";
        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + clId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(noNominalFilter1Response, false));

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + clId)).andExpect(status().isOk());
    }

    @Test
    public void testCreateContingencyNoValue2ForRange() throws Exception {
        String formContingencyList = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"RANGE\"," +
                "    \"value1\": \"63.\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"nominalVoltage2\": {" +
                "    \"type\": \"RANGE\"," +
                "    \"value1\": \"44.\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries1\": [\"FR\", \"BE\"]" +
                "}";
        // creation
        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(formContingencyList)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID clId = objectMapper.readValue(res, FormContingencyList.class).getId();
        // retrieve it, no numeric filters created (because of null values)
        String noNominalFilter1Response = "{\"equipmentType\":\"LINE\",\"nominalVoltage\":null,\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[],\"countries1\":[\"BE\",\"FR\"],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}}";
        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + clId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(noNominalFilter1Response, false));

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + clId)).andExpect(status().isOk());
    }

    @Test
    public void contingencyListAttributesTest() {
        UUID contingencyListAttrId = UUID.randomUUID();
        ContingencyListMetadataImpl contingencyListAttributes = new ContingencyListMetadataImpl(contingencyListAttrId, ContingencyListType.SCRIPT, null);
        assertEquals(contingencyListAttrId, contingencyListAttributes.getId());
        assertEquals(ContingencyListType.SCRIPT, contingencyListAttributes.getType());
        ContingencyListMetadataImpl contingencyListAttributes2 = new ContingencyListMetadataImpl();
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
        entity.setCountries1(Set.of("FRANCE", "ITALY"));

        assertEquals("LINE", entity.getEquipmentType());
        assertEquals(225., entity.getNominalVoltage1().getValue1(), 0.1);
        assertEquals(EQUALITY, entity.getNominalVoltage1().getOperator());
        assertTrue(entity.getCountries1().contains("FRANCE"));
        assertTrue(entity.getCountries1().contains("ITALY"));
    }

    @Test
    public void replaceFormWithScriptTest() throws Exception {
        String userId = "userId";
        String form = "{\n" +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": {" +
                "    \"type\": \"GREATER_THAN\"," +
                "    \"value1\": \"100\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        // Put data
        UUID id = addNewFormContingencyList(form);

        // replace with groovy script
        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists/" + id + "/replace-with-script")
                .header(USER_ID_HEADER, userId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Message<byte[]> message = output.receive(TIMEOUT, elementUpdateDestination);
        assertEquals(id, message.getHeaders().get(NotificationService.HEADER_ELEMENT_UUID));
        assertEquals(userId, message.getHeaders().get(NotificationService.HEADER_MODIFIED_BY));

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
                "  \"nominalVoltage\": {" +
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

        String newUuid = mvc.perform(post("/" + VERSION + "/form-contingency-lists?duplicateFrom=" + id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNotNull(newUuid);
        mvc.perform(post("/" + VERSION + "/form-contingency-lists?duplicateFrom=" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void duplicateScriptContingencyList() throws Exception {
        String script = "{ " + "\"script\" : \"contingency('NHV1_NHV2_1') {" +
                "     equipments 'NHV1_NHV2_1'}\"" +
                "}";
        UUID id = addNewScriptContingencyList(script.strip());

        String newUuid = mvc.perform(post("/" + VERSION + "/script-contingency-lists?duplicateFrom=" + id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNotNull(newUuid);
        mvc.perform(post("/" + VERSION + "/script-contingency-lists" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    public IdBasedContingencyList createIdBasedContingencyList(UUID listId, Date modificationDate, String... identifiers) {
        List< NetworkElementIdentifier > networkElementIdentifiers = Arrays.stream(identifiers).map(id -> new NetworkElementIdentifierList(List.of(new IdBasedNetworkElementIdentifier(id)), id)).collect(Collectors.toList());
        return new IdBasedContingencyList(listId, modificationDate, new IdentifierContingencyList(listId != null ? listId.toString() : "defaultName", networkElementIdentifiers));
    }

    public int getContingencyListsCount() throws Exception {
        String res = mvc.perform(get("/" + VERSION + "/contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        List<IdBasedContingencyList> contingencyListAttributes = objectMapper.readValue(res, new TypeReference<>() {
        });
        return contingencyListAttributes.size();
    }

    private void matchContingencyListMetadata(ContingencyListMetadata metadata1, ContingencyListMetadata metadata2) {
        assertEquals(metadata1.getId(), metadata2.getId());
        assertEquals(metadata1.getType(), metadata2.getType());
        assertTrue((metadata1.getModificationDate().getTime() - metadata2.getModificationDate().getTime()) < 2000);
    }

    private void matchIdBasedContingencyList(IdBasedContingencyList cl1, IdBasedContingencyList cl2) {
        matchContingencyListMetadata(cl1.getMetadata(), cl2.getMetadata());
        assertTrue(new MatcherJson<>(objectMapper, cl1.getIdentifierContingencyList()).matchesSafely(cl2.getIdentifierContingencyList()));
    }

    private void matchScriptContingencyList(ScriptContingencyList cl1, ScriptContingencyList cl2) {
        matchContingencyListMetadata(cl1.getMetadata(), cl2.getMetadata());
        assertTrue(cl1.getScript().contains(cl1.getScript()));
    }

    @Test
    public void createIdBasedContingencyList() throws Exception {
        Date modificationDate = new Date();
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
    public void createIdBasedContingencyListError() throws Exception {
        Date modificationDate = new Date();

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
    public void duplicateBasedContingencyList() throws Exception {
        Date modificationDate = new Date();
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
    public void exportIdBasedContingencyList() throws Exception {
        Date modificationDate = new Date();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "NHV1_NHV2_1");

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();

        mvc.perform(get("/" + VERSION + "/contingency-lists/" + contingencyListId + "/export?networkUuid=" + NETWORK_UUID + (VARIANT_ID_1 != null ? "&variantId=" + VARIANT_ID_1 : ""))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());
    }

    @Test
    public void modifyIdBasedContingencyList() throws Exception {
        Date modificationDate = new Date();
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
}
