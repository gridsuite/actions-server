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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinTable(
            name = "filter_based_contingency_filer_metadata",
            joinColumns = @JoinColumn(
                    name = "filter_based_contingency_id",
                    foreignKey = @ForeignKey(name = "filter_based_contingency_id_fk")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "filter_metadata_id",
                    foreignKey = @ForeignKey(name = "filter_metadata_id_fk")
            ),
            uniqueConstraints = @UniqueConstraint(
                    name = "filter_based_contingency_filter_metadata_uc",
                    columnNames = {"filter_based_contingency_id", "filter_metadata_id"}
            )
    )
    private List<FilterMetaDataEntity> filtersListEntities;

    public FilterBasedContingencyListEntity(FilterBasedContingencyList contingencyList) {
        super();
        if (CollectionUtils.isEmpty(contingencyList.getFilters())) {
            return;
        }

        init(contingencyList);
    }

    private void init(FilterBasedContingencyList contingencyList) {
        filtersListEntities = new ArrayList<>();
        contingencyList.getFilters().forEach(f ->
            filtersListEntities.add(
                new FilterMetaDataEntity(UUID.randomUUID(), f.getId(), f.getName(), f.getEquipmentType()))
        );
    }

    public FilterBasedContingencyListEntity update(FilterBasedContingencyList contingencyList) {
        init(contingencyList);
        return this;
    }
}
