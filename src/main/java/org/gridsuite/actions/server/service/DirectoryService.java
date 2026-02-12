/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Caroline Jeandat {@literal <caroline.jeandat at rte-france.com>}
 */
@Service
public class DirectoryService {
    static final String DIRECTORY_API_VERSION = "v1";
    private static final String DELIMITER = "/";

    @Getter
    private final String baseUri;
    private final RestTemplate restTemplate;

    @Autowired
       public DirectoryService(
            @Value("${gridsuite.services.directory-server.base-uri:http://directory-server}") String baseUri,
            RestTemplateBuilder restTemplateBuilder) {
        this.baseUri = baseUri;
        this.restTemplate = restTemplateBuilder.build();
    }

    public String getElementName(UUID elementUuid) {
        Objects.requireNonNull(elementUuid);

        URI path = UriComponentsBuilder
                .fromPath(DELIMITER + DIRECTORY_API_VERSION + "/elements/{elementUuid}")
                .buildAndExpand(elementUuid)
                .toUri();

        try {
            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(
                            getBaseUri() + path,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() { }
                    );

            Map<String, Object> responseBody = response.getBody();
            return responseBody != null ? (String) responseBody.get("elementName") : null;

        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}
