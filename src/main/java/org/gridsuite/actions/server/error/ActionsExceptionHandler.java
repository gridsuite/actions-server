/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessExceptionHandler;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import com.powsybl.ws.commons.error.ServerNameProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ActionsExceptionHandler
    extends AbstractBusinessExceptionHandler<ActionsException, ActionsBusinessErrorCode> {

    public ActionsExceptionHandler(ServerNameProvider serverNameProvider) {
        super(serverNameProvider);
    }

    @NonNull
    @Override
    protected ActionsBusinessErrorCode getBusinessCode(ActionsException ex) {
        return ex.getBusinessErrorCode();
    }

    @Override
    protected HttpStatus mapStatus(ActionsBusinessErrorCode errorCode) {
        return switch (errorCode) {
            case CONTINGENCY_LIST_NOT_FOUND, NETWORK_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_CONTINGENCY_LIST_PAYLOAD -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(ActionsException.class)
    protected ResponseEntity<PowsyblWsProblemDetail> handleActionsException(
        ActionsException exception, HttpServletRequest request) {
        return super.handleDomainException(exception, request);
    }
}
