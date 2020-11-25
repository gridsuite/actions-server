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
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
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

        mvc.perform(put("/" + VERSION + "/script-contingency-lists/foo")
                .content(script)
                .contentType(TEXT_PLAIN))
                .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/script-contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\"}]"));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/bar")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"foo\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\"}"));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo/export")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]")); // there is no network so all contingencies are invalid

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo/export?networkUuid=" + NETWORK_UUID)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"BRANCH\"}]}]"));

        mvc.perform(post("/" + VERSION + "/script-contingency-lists/baz/rename")
                .content("{\"newContingencyListName\": \"bar\"}")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound());

        mvc.perform(post("/" + VERSION + "/contingency-lists/foo/rename")
                .content("{\"newContingencyListName\": \"bar\"}")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/bar")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"bar\",\"script\":\"contingency('NHV1_NHV2_1') {     equipments 'NHV1_NHV2_1'}\"}"));

        mvc.perform(delete("/" + VERSION + "/contingency-lists/bar"))
                .andExpect(status().isOk());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/foo"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void emptyScriptTest() throws Exception {
        mvc.perform(put("/" + VERSION + "/script-contingency-lists/foo")
                .content("")
                .contentType(TEXT_PLAIN))
                .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/script-contingency-lists/foo/export")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }
}
