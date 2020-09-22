/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + ActionsApi.API_VERSION)
@Transactional
@Api(value = "Actions server")
@ComponentScan(basePackageClasses = ContingencyListService.class)
public class ContingencyListController {

    private final ContingencyListService service;

    public ContingencyListController(ContingencyListService service) {
        this.service = service;
    }

    @GetMapping(value = "contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all contingency lists", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "All contingency lists")})
    public ResponseEntity<List<ContingencyList>> getContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyLists());
    }

    @GetMapping(value = "contingency-lists/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get contingency list by name", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The contingency list")})
    public ResponseEntity<ContingencyList> getContingencyList(@PathVariable("name") String name) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyList(name).orElse(null));
    }

    @PutMapping(value = "script-contingency-lists/{name}", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Create a script contingency list", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The contingency list have been created successfully")})
    public void createScriptContingencyList(@PathVariable("name") String name, @RequestBody(required = false) String script) {
        service.createScriptContingencyList(name, script);
    }
}
