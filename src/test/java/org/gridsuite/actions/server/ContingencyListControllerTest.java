/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import javax.inject.Inject;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@RunWith(SpringRunner.class)
@WebMvcTest(ContingencyListController.class)
@ContextConfiguration(classes = {ActionsApplication.class})
public class ContingencyListControllerTest extends AbstractEmbeddedCassandraSetup {

    @Autowired
    private MockMvc mvc;

    @Inject
    private ScriptContingencyListRepository repository;

    @Inject
    private ContingencyListService service;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test() throws Exception {
        String script =
            "contingency 'c1' {" +
            "     equipments: 'c1'" +
            "}";

        mvc.perform(post("/" + VERSION + "/script-contingency-lists/foo")
                .content(script)
                .contentType(TEXT_PLAIN))
                .andExpect(status().isOk());

        mvc.perform(get("/" + VERSION + "/contingency-lists")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"name\":\"foo\",\"script\":\"contingency 'c1' {     equipments: 'c1'}\"}]"));

        mvc.perform(get("/" + VERSION + "/contingency-lists/foo")
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"name\":\"foo\",\"script\":\"contingency 'c1' {     equipments: 'c1'}\"}"));
    }
}
