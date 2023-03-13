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

    @GetMapping(value = "/script-contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all script contingency lists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All script contingency lists")})
    public ResponseEntity<List<ScriptContingencyList>> getScriptContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getScriptContingencyLists());
    }

    @GetMapping(value = "/form-contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all form contingency lists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All form contingency lists")})
    public ResponseEntity<List<FormContingencyList>> getFormContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getFormContingencyLists());
    }

    @GetMapping(value = "/contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all contingency lists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All contingency lists")})
    public ResponseEntity<List<ContingencyListAttributes>> getContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyLists());
    }

    @GetMapping(value = "/script-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get script contingency list by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list"),
        @ApiResponse(responseCode = "404", description = "The script contingency list does not exists")})
    public ResponseEntity<ScriptContingencyList> getScriptContingencyList(@PathVariable("id") UUID id) {
        return service.getScriptContingencyList(id).map(contingencyList -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(contingencyList))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/form-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get form contingency list by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list"),
        @ApiResponse(responseCode = "404", description = "The form contingency list does not exists")})
    public ResponseEntity<FormContingencyList> getFormContingencyList(@PathVariable("id") UUID id) {
        return service.getFormContingencyList(id).map(contingencyList -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(contingencyList))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/contingency-lists/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Export a contingency list to PowSyBl JSON format")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The contingency list on PowSyBl JSON format")})
    public ResponseEntity<List<Contingency>> exportContingencyList(@PathVariable("id") UUID id,
                                                                   @RequestParam(value = "networkUuid", required = false) UUID networkUuid,
                                                                   @RequestParam(value = "variantId", required = false) String variantId) {
        return service.exportContingencyList(id, networkUuid, variantId).map(contingencies -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(contingencies))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/script-contingency-lists", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been created successfully")})
    public ResponseEntity<ScriptContingencyList> createScriptContingencyList(@RequestParam(required = false, value = "id") UUID id,
                                                                             @RequestBody ScriptContingencyList script) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.createScriptContingencyList(id, script));
    }

    @PostMapping(value = "/script-contingency-lists")
    @Operation(summary = "Create a script contingency list from another existing one")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been duplicated successfully"),
                           @ApiResponse(responseCode = "404", description = "Source script contingency list not found")})
    public ResponseEntity<ScriptContingencyList> duplicateScriptContingencyList(@RequestParam("duplicateFrom") UUID sourceListId,
                                                                                @RequestParam(value = "id") UUID id) {
        return service.createScriptContingencyList(sourceListId, id).map(contingencyList -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/script-contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been modified successfully")})
    public ResponseEntity<Void> modifyScriptContingencyList(
            @PathVariable UUID id,
            @RequestBody(required = false) ScriptContingencyList script,
            @RequestHeader("userId") String userId) {
        try {
            service.modifyScriptContingencyList(id, script, userId);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/form-contingency-lists", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list have been created successfully")})
    public ResponseEntity<FormContingencyList> createFormContingencyList(@RequestParam(required = false, value = "id") UUID id,
                                                                           @RequestBody FormContingencyList formContingencyList) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.createFormContingencyList(id, formContingencyList));
    }

    @PostMapping(value = "/identifier-contingency-lists", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an identifier contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The identifier contingency list have been created successfully")})
    public ResponseEntity<IdBasedContingencyList> createIdentifierContingencyList(@RequestParam(required = false, value = "id") UUID id,
                                                                                  @RequestBody IdBasedContingencyList idBasedContingencyList) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.createIdBasedContingencyList(id, idBasedContingencyList));
    }

    @GetMapping(value = "/identifier-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get identifier contingency list by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The identifier contingency list"),
            @ApiResponse(responseCode = "404", description = "The identifier contingency list does not exists")})
    public ResponseEntity<IdBasedContingencyList> getIdentifierContingencyList(@PathVariable("id") UUID id) {
        return service.getIdBasedContingencyList(id).map(contingencyList -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/identifier-contingency-lists")
    @Operation(summary = "Create a identifier contingency list from another existing one")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The identifier contingency list have been duplicated successfully"),
            @ApiResponse(responseCode = "404", description = "Source script contingency list not found")})
    public ResponseEntity<IdBasedContingencyList> duplicateIdentifierContingencyList(@RequestParam("duplicateFrom") UUID sourceListId,
                                                                                  @RequestParam(value = "id") UUID id) {
        return service.createIdBasedContingencyList(sourceListId, id).map(contingencyList -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/form-contingency-lists")
    @Operation(summary = "Create a form contingency list from another existing one")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list have been duplicated successfully"),
                           @ApiResponse(responseCode = "404", description = "Source form contingency list not found")})
    public ResponseEntity<FormContingencyList> duplicateFormContingencyList(@RequestParam("duplicateFrom") UUID sourceListId,
                                                                             @RequestParam(value = "id") UUID id) {
        return service.createFormContingencyList(sourceListId, id).map(contingencyList -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/form-contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list have been modified successfully")})
    public ResponseEntity<Void> modifyFormContingencyList(
            @PathVariable UUID id,
            @RequestBody FormContingencyList formContingencyList,
            @RequestHeader("userId") String userId) {
        try {
            service.modifyFormContingencyList(id, formContingencyList, userId);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(value = "/contingency-lists/{id}")
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

    @PostMapping(value = "/form-contingency-lists/{id}/replace-with-script")
    @Operation(summary = "Replace a form contingency list with a script contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list have been replaced successfully")})
    public ResponseEntity<ScriptContingencyList> replaceFormContingencyListWithScript(@PathVariable("id") UUID id, @RequestHeader("userId") String userId) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.replaceFormContingencyListWithScript(id, userId));
    }

    @PostMapping(value = "/form-contingency-lists/{id}/new-script")
    @Operation(summary = "Create a new script contingency list from a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The script contingency list have been created successfully")})
    public ResponseEntity<ScriptContingencyList> newScriptFromFormContingencyList(@PathVariable("id") UUID id,
                                                                                     @RequestParam(required = false, value = "newId") UUID newId) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.newScriptFromFormContingencyList(id, newId));
    }

    @GetMapping(value = "/contingency-lists/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get contingency lists metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "contingency lists metadata"),
        @ApiResponse(responseCode = "404", description = "The contingency list does not exists")})
    public ResponseEntity<List<ContingencyListAttributes>> getContingencyListsMetadata(@RequestParam("ids") List<UUID> ids) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyLists(ids));
    }
}
