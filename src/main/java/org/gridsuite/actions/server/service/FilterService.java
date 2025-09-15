/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.actions.server.service;
import lombok.Getter;
import org.gridsuite.actions.server.dto.IdentifiableAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FilterService {
    public static final String FILTER_END_POINT_EVALUATE_IDS = "/filters/evaluate/identifiables";
    public static final String DELIMITER = "/";
    public static final String FILTER_API_VERSION = "v1";

    @Getter
    private final String baseUri;
    private final RestTemplate restTemplate;

    @Autowired
    public FilterService(@Value("${gridsuite.services.filter-server.base-uri:http://filter-server/}") String baseUri,
                         RestTemplate restTemplate) {
        this.baseUri = baseUri;
        this.restTemplate = restTemplate;
    }

    public List<IdentifiableAttributes> evaluateFilters(UUID networkUuid, String variantUuid, List<UUID> filtersUuid) {
        Objects.requireNonNull(networkUuid);
        Objects.requireNonNull(filtersUuid);
        String endPointUrl = getBaseUri() + DELIMITER + FILTER_API_VERSION + FILTER_END_POINT_EVALUATE_IDS;

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(endPointUrl);
        uriComponentsBuilder.queryParam("networkUuid", networkUuid);
        uriComponentsBuilder.queryParam("variantUuid", variantUuid);
        uriComponentsBuilder.queryParam("ids", filtersUuid);
        var uriComponent = uriComponentsBuilder.buildAndExpand();

        ResponseEntity<IdentifiableAttributes[]> response = restTemplate.getForEntity(uriComponent.toUriString(), IdentifiableAttributes[].class);
        return Arrays.stream(Objects.requireNonNull(response.getBody())).toList();
    }
}
