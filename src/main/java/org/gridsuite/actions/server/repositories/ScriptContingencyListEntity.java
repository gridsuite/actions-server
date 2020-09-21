/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.repositories;

import lombok.Getter;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Getter
@Table("script_contingency_list")
public class ScriptContingencyListEntity {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    private String name;

    private String script;

    public ScriptContingencyListEntity(String name, String script) {
        this.name = Objects.requireNonNull(name);
        this.script = script;
    }
}
