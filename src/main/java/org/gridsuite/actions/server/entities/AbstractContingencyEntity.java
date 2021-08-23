/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.actions.server.dto.ContingencyList;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import java.util.Date;
import java.util.UUID;

import static javax.persistence.TemporalType.TIMESTAMP;

/**
 * @author Jacques Borsenberger <jacques.borsenberger at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractContingencyEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "name")
    private String name;

    @CreatedDate
    @Temporal(TIMESTAMP)
    @Column(name = "creationDate", updatable = false)
    private Date creationDate;

    @LastModifiedDate
    @Temporal(TIMESTAMP)
    @Column(name = "modificationDate")
    private Date modificationDate;

    @Column()
    private String description;

    @Column(name = "userId", nullable = false)
    private String userId;

    @Column(name = "isPrivate", nullable = false)
    private boolean isPrivate;

    protected final void init(ContingencyList attributes) {
        name = attributes.getName();
        description = attributes.getDescription();
        userId = attributes.getUserId();
        isPrivate = attributes.isPrivate();
    }

    protected void update(ContingencyList attributes) {
        init(attributes);
    }
}
