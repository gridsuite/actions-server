/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ExceptionUtils {

    private ExceptionUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static void throwNotFound(String resourceId, String resourceType) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("%s %s not found", resourceType, resourceId));
    }
}
