/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.actions.server.utils.ContingencyListType;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContingencyListAttributes {
    private String name;
    private ContingencyListType type;
}
