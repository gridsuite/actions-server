/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.Date;
import java.util.UUID;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contingency list attributes")
public class ContingencyListAttributes implements ContingencyList {

    @Schema(description = "list id")
    private UUID id;

    @Schema(description = "List name")
    private String name;

    @Schema(description = "List type")
    private ContingencyListType type;

    @Schema(description = "Creation Date")
    Date creationDate;

    @Schema(description = "Modification Date")
    Date modificationDate;

    @Schema(description = "Description")
    String description;

    @Schema(description = "user id")
    private String userId;

    @Schema(description = "private")
    @JsonProperty("private")
    private boolean isPrivate;
}
