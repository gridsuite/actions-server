/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
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

    @Column(name = "filter_ids")
    @ElementCollection(targetClass = UUID.class)
    @CollectionTable(name = "filter_based_contingency_list_filter",
        joinColumns = @JoinColumn(name = "filter_based_contingency_list_id"),
        foreignKey = @ForeignKey(name = "filter_based_contingency_list_id_fk"))
    private List<UUID> filtersIds;

    public FilterBasedContingencyListEntity(FilterBasedContingencyList contingencyList) {
        super();
        if (CollectionUtils.isEmpty(contingencyList.getFilters())) {
            return;
        }

        init(contingencyList);
    }

    private void init(FilterBasedContingencyList contingencyList) {
        filtersIds = new ArrayList<>();
        contingencyList.getFilters().forEach(filterAttributes -> filtersIds.add(filterAttributes.id()));
    }

    public FilterBasedContingencyListEntity update(FilterBasedContingencyList contingencyList) {
        init(contingencyList);
        return this;
    }
}
