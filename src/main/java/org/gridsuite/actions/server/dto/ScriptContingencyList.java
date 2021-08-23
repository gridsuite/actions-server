/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.UUID;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Script contingency list")
public class ScriptContingencyList implements ContingencyList {

    @Schema(description = "list id")
    private UUID id;

    @Schema(description = "List name")
    private String name;

    @Schema(description = "Script")
    private String script;

    @Schema(description = "Type")
    @Override
    public ContingencyListType getType() {
        return ContingencyListType.SCRIPT;
    }

    @Schema(description = "description")
    private String description;

    @Schema(description = "user id")
    private String userId;

    @Schema(description = "private")
    private boolean isPrivate;
}
