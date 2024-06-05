/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.contingency.list.ContingencyList;
import com.powsybl.contingency.dsl.ContingencyDslLoader;
import com.powsybl.iidm.network.Network;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.gridsuite.actions.server.utils.ContingencyListType;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Getter
@NoArgsConstructor
@Schema(description = "Script contingency list")
public class ScriptContingencyList extends AbstractContingencyList {

    @Schema(description = "Script")
    private String script;

    public ScriptContingencyList(UUID uuid, Instant date, String script) {
        super(new ContingencyListMetadataImpl(uuid, ContingencyListType.SCRIPT, date));
        this.script = script;
    }

    public ScriptContingencyList(String script) {
        this(null, null, script);
    }

    @Override
    public ContingencyList toPowsyblContingencyList(Network network) {
        ImportCustomizer customizer = new ImportCustomizer();
        customizer.addImports("org.gridsuite.actions.server.utils.FiltersUtils");
        customizer.addStaticStars("org.gridsuite.actions.server.utils.FiltersUtils");
        List<Contingency> contingencyList = new ContingencyDslLoader(script).load(network, customizer);
        return ContingencyList.of(contingencyList.toArray(Contingency[]::new));
    }

    //TODO this a temporary workaround to get elements not found in the network
    // this should be deleted when a fix is added to powsybl
    public Map<String, Set<String>> getNotFoundElements(Network network) {
        Map<String, Set<String>> contingencyEquipmentMap = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("contingency\\('([^']+)'\\)\\s*\\{\\s*equipments\\s*'([^']+)'");
        Matcher matcher = pattern.matcher(script);

        while (matcher.find()) {
            String contingencyId = matcher.group(1);
            String equipmentsIds = matcher.group(2);
            String[] equipmentsArray = equipmentsIds.split(",\\s*"); // Splitting multiple IDs by comma
            Set<String> equipmentSet = Arrays.stream(equipmentsArray)
                    .filter(id -> network.getIdentifiable(id) == null).collect(Collectors.toSet());

            if (!CollectionUtils.isEmpty(equipmentSet)) {
                contingencyEquipmentMap.put(contingencyId, equipmentSet);
            }
        }

        return contingencyEquipmentMap;
    }
}
