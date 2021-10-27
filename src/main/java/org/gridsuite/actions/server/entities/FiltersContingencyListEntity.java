/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.gridsuite.actions.server.dto.FiltersContingencyListAttributes;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.collections4.SetUtils.emptyIfNull;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "filters_contingency_list", indexes = @Index(name = "filter_contingency_list_name_idx", columnList = "name"))
public class FiltersContingencyListEntity extends AbstractContingencyEntity {

    @Column(name = "equipmentId")
    private String equipmentId;

    @Column(name = "equipmentName")
    private String equipmentName;

    @Column(name = "equipmentType")
    private String equipmentType;

    @Column(name = "nominalVoltage")
    private double nominalVoltage;

    @Column(name = "nominalVoltageOperator")
    private String nominalVoltageOperator;

    @Column(name = "country")
    @ElementCollection
    @CollectionTable(foreignKey = @ForeignKey(name = "filtersContingencyListEntity_countries_fk"), indexes = {@Index(name = "filtersContingencyListEntity_countries_idx", columnList = "filters_contingency_list_entity_id")})
    private Set<String> countries;

    public FiltersContingencyListEntity(FiltersContingencyListAttributes filtersContingencyListAttributes) {
        super();
        init(filtersContingencyListAttributes);
    }

    /* called in constructor so it is final */
    final void init(FiltersContingencyListAttributes filtersContingencyListAttributes) {
        super.update(filtersContingencyListAttributes);
        this.equipmentId = filtersContingencyListAttributes.getEquipmentID();
        this.equipmentName = filtersContingencyListAttributes.getEquipmentName();
        this.equipmentType = filtersContingencyListAttributes.getEquipmentType();
        this.nominalVoltage = filtersContingencyListAttributes.getNominalVoltage();
        this.nominalVoltageOperator = filtersContingencyListAttributes.getNominalVoltageOperator();
        this.countries = new HashSet<>(emptyIfNull(filtersContingencyListAttributes.getCountries()));
    }

    public FiltersContingencyListEntity update(FiltersContingencyListAttributes filtersContingencyListAttributes) {
        init(filtersContingencyListAttributes);
        return this;
    }
}
