/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.contingency.Contingency;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.gridsuite.actions.server.configs.ActionsApi;
import org.gridsuite.actions.server.dto.ContingencyList;
import org.gridsuite.actions.server.dto.GuiContingencyListAttributes;
import org.gridsuite.actions.server.dto.RenameContingencyListAttributes;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + ActionsApi.API_VERSION)
@Api(value = "Actions server")
@ComponentScan(basePackageClasses = ContingencyListService.class)
public class ContingencyListController {

    private final ContingencyListService service;

    public ContingencyListController(ContingencyListService service) {
        this.service = service;
    }

    @GetMapping(value = "script-contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all script contingency lists", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "All script contingency lists")})
    public ResponseEntity<List<ContingencyList>> getScriptContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getScriptContingencyLists());
    }

    @GetMapping(value = "gui-contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all script contingency lists", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "All gui contingency lists")})
    public ResponseEntity<List<ContingencyList>> getGuiContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getGuiContingencyLists());
    }

    @GetMapping(value = "script-contingency-lists/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get script contingency list by name", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The script contingency list"),
                           @ApiResponse(code = 404, message = "The script contingency list does not exists")})
    public ResponseEntity<ContingencyList> getScriptContingencyList(@PathVariable("name") String name) {
        return service.getScriptContingencyList(name).map(contingencyList -> ResponseEntity.ok()
                                                                                     .contentType(MediaType.APPLICATION_JSON)
                                                                                     .body(contingencyList))
                                               .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "gui-contingency-lists/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get gui contingency list by name", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The gui contingency list"),
            @ApiResponse(code = 404, message = "The gui contingency list does not exists")})
    public ResponseEntity<ContingencyList> getGuiContingencyList(@PathVariable("name") String name) {
        return service.getGuiContingencyList(name).map(contingencyList -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "script-contingency-lists/{name}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Export a contingency list to PowSyBl JSON format", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The contingency list on PowSyBl JSON format")})
    public ResponseEntity<List<Contingency>> exportContingencyList(@PathVariable("name") String name,
                                                                   @RequestParam(value = "networkUuid", required = false) UUID networkUuid) {
        return service.exportContingencyList(name, networkUuid).map(contingencies -> ResponseEntity.ok()
                                                                                                   .contentType(MediaType.APPLICATION_JSON)
                                                                                                   .body(contingencies))
                                                               .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "script-contingency-lists/{name}", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Create a script contingency list", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The script contingency list have been created successfully")})
    public void createScriptContingencyList(@PathVariable("name") String name, @RequestBody(required = false) String script) {
        service.createScriptContingencyList(name, script);
    }

    @PutMapping(value = "gui-contingency-lists/{name}", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ApiOperation(value = "Create a gui contingency list", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The gui contingency list have been created successfully")})
    public void createGuiContingencyList(@PathVariable("name") String name, @RequestBody(required = false) GuiContingencyListAttributes guiContingencyListAttributes) {
        service.createGuiContingencyList(name, guiContingencyListAttributes);
    }

    @DeleteMapping(value = "script-contingency-lists/{name}")
    @ApiOperation(value = "delete the contingency list")
    @ApiResponse(code = 200, message = "The contingency list has been deleted")
    public ResponseEntity<Void> deleteContingencyList(@PathVariable("name") String name) {
        service.deleteContingencyList(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "script-contingency-lists/{name}/rename")
    @ApiOperation(value = "Rename contingency list by name")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The contingency list has been renamed"),
            @ApiResponse(code = 404, message = "The contingency list does not exists") })
    public void renameContingencyList(@PathVariable("name") String name,
            @RequestBody RenameContingencyListAttributes renameContingencyListAttributes) {
        service.renameContingencyList(name, renameContingencyListAttributes.getNewContingencyListName());
    }
}
