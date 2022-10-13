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

public enum NominalVoltageOperator {
    EQUAL,
    MORE_THAN,
    MORE_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    RANGE;

    public static NominalVoltageOperator fromOperator(String op) {
        switch (op) {
            case "=":
                return EQUAL;
            case ">":
                return MORE_THAN;
            case ">=":
                return MORE_THAN_OR_EQUAL;
            case "<":
                return LESS_THAN;
            case "<=":
                return LESS_THAN_OR_EQUAL;
            default:
                throw new PowsyblException("Unknown nominal voltage operator");
        }
    }
}
