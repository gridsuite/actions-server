/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessException;
import lombok.NonNull;

import java.util.Objects;
import java.util.UUID;

public class ActionsException extends AbstractBusinessException {

    private static final String NOT_FOUND_SUFFIX = "' not found";

    private final ActionsBusinessErrorCode errorCode;

    public ActionsException(ActionsBusinessErrorCode errorCode, String message) {
        super(Objects.requireNonNull(message, "message must not be null"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public static ActionsException createContingencyListNotFound(@NonNull UUID id) {
        return new ActionsException(ActionsBusinessErrorCode.CONTINGENCY_LIST_NOT_FOUND,
                "Contingency list '" + id + NOT_FOUND_SUFFIX);
    }

    public static ActionsException createContingencyListNotFound(@NonNull String type, @NonNull UUID id) {
        return new ActionsException(ActionsBusinessErrorCode.CONTINGENCY_LIST_NOT_FOUND,
                type + " '" + id + NOT_FOUND_SUFFIX);
    }

    public static ActionsException createNetworkNotFound(@NonNull UUID networkUuid) {
        return new ActionsException(ActionsBusinessErrorCode.NETWORK_NOT_FOUND,
                "Network '" + networkUuid + NOT_FOUND_SUFFIX);
    }

    public static ActionsException createInvalidPayload(@NonNull String message) {
        return new ActionsException(ActionsBusinessErrorCode.INVALID_CONTINGENCY_LIST_PAYLOAD, message);
    }

    @NonNull
    @Override
    public ActionsBusinessErrorCode getBusinessErrorCode() {
        return errorCode;
    }
}
