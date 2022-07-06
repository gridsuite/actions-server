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
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.util.Set;
import java.util.UUID;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Form contingency list")
public class FormContingencyList implements ContingencyList {

    @Schema(description = "list id")
    private UUID id;

    @Schema(description = "Equipment type")
    private String equipmentType;

    @Schema(description = "Nominal voltage")
    private double nominalVoltage;

    @Schema(description = "Nominal voltage operator")
    private String nominalVoltageOperator;

    @Schema(description = "Countries")
    private Set<String> countries;

    @Schema(description = "Type")
    @Override
    public ContingencyListType getType() {
        return ContingencyListType.FORM;
    }
}
