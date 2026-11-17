/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.error;

import com.powsybl.ws.commons.error.BusinessErrorCode;

/**
 * Business error codes emitted by the actions service.
 */
public enum ActionsBusinessErrorCode implements BusinessErrorCode {
    CONTINGENCY_LIST_NOT_FOUND("actions.contingencyListNotFound"),
    INVALID_CONTINGENCY_LIST_PAYLOAD("actions.invalidContingencyListPayload"),
    NETWORK_NOT_FOUND("actions.networkNotFound");

    private final String code;

    ActionsBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}
