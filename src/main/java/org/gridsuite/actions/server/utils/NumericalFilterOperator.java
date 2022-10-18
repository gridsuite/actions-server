/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.utils;

import com.powsybl.commons.PowsyblException;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */

public enum NumericalFilterOperator {
    EQUALITY,
    GREATER_THAN,
    GREATER_OR_EQUAL,
    LESS_THAN,
    LESS_OR_EQUAL,
    RANGE;

    public static String toScript(NumericalFilterOperator op) {
        switch (op) {
            case EQUALITY:
                return "==";
            case GREATER_THAN:
                return ">";
            case GREATER_OR_EQUAL:
                return ">=";
            case LESS_THAN:
                return "<";
            case LESS_OR_EQUAL:
                return "<=";
            case RANGE:
                return "range";
            default:
                throw new PowsyblException("Unknown operator");
        }
    }
}
