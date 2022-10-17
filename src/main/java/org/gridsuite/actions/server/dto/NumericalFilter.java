/**
 *  Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import lombok.*;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class NumericalFilter {
    NumericalFilterOperator operator;
    Double value1;
    Double value2;
}

