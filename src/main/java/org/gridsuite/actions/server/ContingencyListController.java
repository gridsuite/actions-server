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

import org.gridsuite.actions.server.dto.*;
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
    public ResponseEntity<List<ScriptContingencyList>> getScriptContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getScriptContingencyLists());
    }

    @GetMapping(value = "filters-contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all filters contingency lists", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "All filters contingency lists")})
    public ResponseEntity<List<FiltersContingencyList>> getFilterContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getFilterContingencyLists());
    }

    @GetMapping(value = "contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get all contingency lists", response = List.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "All contingency lists")})
    public ResponseEntity<List<ContingencyListAttributes>> getContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyLists());
    }

    @GetMapping(value = "script-contingency-lists/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get script contingency list by name", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The script contingency list"),
                           @ApiResponse(code = 404, message = "The script contingency list does not exists")})
    public ResponseEntity<ScriptContingencyList> getScriptContingencyList(@PathVariable("name") String name) {
        return service.getScriptContingencyList(name).map(contingencyList -> ResponseEntity.ok()
                                                                                     .contentType(MediaType.APPLICATION_JSON)
                                                                                     .body(contingencyList))
                                               .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "filters-contingency-lists/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get filters contingency list by name", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The filters contingency list"),
            @ApiResponse(code = 404, message = "The filters contingency list does not exists")})
    public ResponseEntity<FiltersContingencyList> getFilterContingencyList(@PathVariable("name") String name) {
        return service.getFiltersContingencyList(name).map(contingencyList -> ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "contingency-lists/{name}/export", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @PutMapping(value = "filters-contingency-lists/{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a filters contingency list", response = ContingencyList.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The filters contingency list have been created successfully")})
    public void createFilterContingencyList(@PathVariable("name") String name, @RequestBody(required = true) FiltersContingencyListAttributes filtersContingencyListAttributes) {
        service.createFilterContingencyList(name, filtersContingencyListAttributes);
    }

    @DeleteMapping(value = "contingency-lists/{name}")
    @ApiOperation(value = "delete the contingency list")
    @ApiResponse(code = 200, message = "The contingency list has been deleted")
    public ResponseEntity<Void> deleteContingencyList(@PathVariable("name") String name) {
        service.deleteContingencyList(name);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "contingency-lists/{name}/rename")
    @ApiOperation(value = "Rename contingency list by name")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The contingency list has been renamed"),
            @ApiResponse(code = 404, message = "The contingency list does not exists") })
    public void renameContingencyList(@PathVariable("name") String name,
            @RequestBody RenameContingencyListAttributes renameContingencyListAttributes) {
        service.renameContingencyList(name, renameContingencyListAttributes.getNewContingencyListName());
    }

    @PutMapping(value = "filters-contingency-lists/{name}/replace-with-script")
    @ApiOperation(value = "Replace a filters contingency list with a script contingency list")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The filters contingency list have been replaced successfully")})
    public void replaceFilterContingencyListWithScript(@PathVariable("name") String name) {
        service.replaceFilterContingencyListWithScript(name);
    }

    @PutMapping(value = "filters-contingency-lists/{name}/new-script/{scriptName}")
    @ApiOperation(value = "Create a new script contingency list from a filters contingency list")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The script contingency list have been created successfully")})
    public void newScriptFromFiltersContingencyList(@PathVariable("name") String name,
                                                    @PathVariable("scriptName") String scriptName) {
        service.newScriptFromFiltersContingencyList(name, scriptName);
    }
}
