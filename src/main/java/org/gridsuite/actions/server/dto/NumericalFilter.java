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
public class NumericalFilter {
    NumericalFilterOperator type;
    Double value1;
    Double value2;

    public String operator() {
        if (type == NumericalFilterOperator.EQUALITY) {
            return "==";
        } else if (type == NumericalFilterOperator.GREATER_THAN) {
            return ">";
        } else if (type == NumericalFilterOperator.GREATER_OR_EQUAL) {
            return ">=";
        } else if (type == NumericalFilterOperator.LESS_THAN) {
            return "<";
        } else if (type == NumericalFilterOperator.LESS_OR_EQUAL) {
            return "<=";
        }
        return "";
    }
}

