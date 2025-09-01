/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.actions.server.dto.FilterBasedContingencyList;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "filter_based_contingency_list")
public class FilterBasedContingencyListEntity extends AbstractContingencyEntity {

    @OneToMany(cascade = CascadeType.ALL)
    private List<FilterEntity> filtersListEntities;

    public FilterBasedContingencyListEntity(FilterBasedContingencyList contingencyList) {
        super();
        if (CollectionUtils.isEmpty(contingencyList.getFilterList())) {
            return;
        }
        filtersListEntities = new ArrayList<>();
        contingencyList.getFilterList().forEach(f -> filtersListEntities.add(new FilterEntity(UUID.randomUUID(), f)));
    }
}
