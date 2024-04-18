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
