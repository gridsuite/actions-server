/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.utils;

import org.gridsuite.actions.server.utils.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionUtilsTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<ExceptionUtils> constructor = ExceptionUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertThrows(InvocationTargetException.class, constructor::newInstance);

        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof AssertionError);
            assertEquals("Utility class should not be instantiated", cause.getMessage());
        }
    }

    @Test
    void testThrowNotFound() {
        String resourceId = "123";
        String resourceType = "Resource";

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            ExceptionUtils.throwNotFound(resourceId, resourceType);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode(), "The HTTP status should be NOT FOUND.");
        assertEquals(resourceType + " " + resourceId + " not found", exception.getReason(), "The exception message should match the expected message.");
    }
}
