/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.contingency.Contingency;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.actions.server.dto.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + ActionsApi.API_VERSION)
@Tag(name = "Actions server")
@ComponentScan(basePackageClasses = ContingencyListService.class)
public class ContingencyListController {

    private final ContingencyListService service;

    public ContingencyListController(ContingencyListService service) {
        this.service = service;
    }

    @GetMapping(value = "script-contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all script contingency lists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All script contingency lists")})
    public ResponseEntity<List<ScriptContingencyList>> getScriptContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getScriptContingencyLists());
    }

    @GetMapping(value = "filters-contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all filters contingency lists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All filters contingency lists")})
    public ResponseEntity<List<FiltersContingencyList>> getFilterContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getFilterContingencyLists());
    }

    @GetMapping(value = "contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all contingency lists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All contingency lists")})
    public ResponseEntity<List<ContingencyListAttributes>> getContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyLists());
    }

    @GetMapping(value = "script-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get script contingency list by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list"),
        @ApiResponse(responseCode = "404", description = "The script contingency list does not exists")})
    public ResponseEntity<ScriptContingencyList> getScriptContingencyList(@PathVariable("id") UUID id) {
        return service.getScriptContingencyList(id).map(contingencyList -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(contingencyList))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "filters-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get filters contingency list by name")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filters contingency list"),
        @ApiResponse(responseCode = "404", description = "The filters contingency list does not exists")})
    public ResponseEntity<FiltersContingencyList> getFilterContingencyList(@PathVariable("id") UUID id) {
        return service.getFiltersContingencyList(id).map(contingencyList -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(contingencyList))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "contingency-lists/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Export a contingency list to PowSyBl JSON format")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The contingency list on PowSyBl JSON format")})
    public ResponseEntity<List<Contingency>> exportContingencyList(@PathVariable("id") UUID id,
                                                                   @RequestParam(value = "networkUuid", required = false) UUID networkUuid) {
        return service.exportContingencyList(id, networkUuid).map(contingencies -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(contingencies))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "script-contingency-lists/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been created successfully")})
    public ResponseEntity<ScriptContingencyList> createScriptContingencyList(@RequestBody(required = false) ScriptContingencyList script) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.createScriptContingencyList(script));
    }

    @PutMapping(value = "script-contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been modified successfully")})
    public ResponseEntity<Void> modifyScriptContingencyList(@PathVariable UUID id, @RequestBody(required = false) ScriptContingencyList script) {
        try {
            service.modifyScriptContingencyList(id, script);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "filters-contingency-lists/", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a filters contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filters contingency list have been created successfully")})
    public ResponseEntity<FiltersContingencyList> createFilterContingencyList(@RequestBody(required = true) FiltersContingencyListAttributes filtersContingencyListAttributes) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.createFilterContingencyList(filtersContingencyListAttributes));
    }

    @PutMapping(value = "filters-contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a filters contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filters contingency list have been modified successfully")})
    public ResponseEntity<Void> modifyFilterContingencyList(@PathVariable UUID id, @RequestBody(required = true) FiltersContingencyListAttributes filtersContingencyListAttributes) {
        try {
            service.modifyFilterContingencyList(id, filtersContingencyListAttributes);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(value = "contingency-lists/{id}")
    @Operation(summary = "delete the contingency list")
    @ApiResponse(responseCode = "200", description = "The contingency list has been deleted")
    public ResponseEntity<Void> deleteContingencyList(@PathVariable("id") UUID id) {
        try {
            service.deleteContingencyList(id);
            return ResponseEntity.ok().build();
        } catch (EmptyResultDataAccessException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "filters-contingency-lists/{id}/replace-with-script")
    @Operation(summary = "Replace a filters contingency list with a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The filters contingency list have been replaced successfully")})
    public ResponseEntity<ScriptContingencyList> replaceFilterContingencyListWithScript(@PathVariable("id") UUID id) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.replaceFilterContingencyListWithScript(id));
    }

    @PostMapping(value = "filters-contingency-lists/{id}/new-script/{scriptName}")
    @Operation(summary = "Create a new script contingency list from a filters contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been created successfully")})
    public ResponseEntity<ScriptContingencyList> newScriptFromFiltersContingencyList(@PathVariable("id") UUID id,
                                                                                     @PathVariable("scriptName") String scriptName) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.newScriptFromFiltersContingencyList(id, scriptName));
    }

    @GetMapping(value = "metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all contingency lists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "contingency lists metadata"),
        @ApiResponse(responseCode = "404", description = "The contingency list does not exists")})
    public ResponseEntity<List<ContingencyListAttributes>> getContingencyLists(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyLists(ids));
    }
}
