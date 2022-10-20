/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.test;

import org.gridsuite.actions.server.FormToGroovyScript;
import org.gridsuite.actions.server.dto.FormContingencyList;
import org.gridsuite.actions.server.dto.NumericalFilter;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;
import org.junit.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class GenerateScriptFromFiltersTest {
    @Test
    public void generateScriptTest() {
        FormToGroovyScript formToScript = new FormToGroovyScript();

        LinkedHashSet<String> countries = new LinkedHashSet<>();
        countries.add("FR");
        countries.add("BE");

        assertEquals("for (equipment in network.generators) {\n" +
                "  if ((equipment.terminal.voltageLevel.nominalV == 90.0)\n" +
                "      && injectionMatch(equipment.terminal, ['FR','BE'])\n" +
                "     ) {\n" +
                "        contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "GENERATOR", new NumericalFilter(NumericalFilterOperator.EQUALITY, 90., null), null,
            countries, new HashSet<>())));

        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                new FormContingencyList(null,
                    "TWO_WINDINGS_TRANSFORMER", null, null,
                    new HashSet<>(), new HashSet<>()
            )));

        assertEquals("for (equipment in network.hvdcLines) {\n" +
            "  if (\n" +
            "  (hvdcLineMatch(equipment, ['FR','BE'], []))\n" +
            "       ) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "HVDC_LINE", null, null,
            countries, new HashSet<>())));

        assertEquals("for (equipment in network.hvdcLines) {\n" +
                "  if ((equipment.nominalV <= 225.0)\n" +
                "        && (hvdcLineMatch(equipment, ['FR','BE'], []))\n" +
                "       ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
                "HVDC_LINE", new NumericalFilter(NumericalFilterOperator.LESS_OR_EQUAL, 225., null), null,
                countries, new HashSet<>())));

        assertEquals("for (equipment in network.danglingLines) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV == 225.0)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
            null,
            "DANGLING_LINE", new NumericalFilter(NumericalFilterOperator.EQUALITY, 225., null), null,
                new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.staticVarCompensators) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "STATIC_VAR_COMPENSATOR", null, null,
                new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.shuntCompensators) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV < 90.0)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "SHUNT_COMPENSATOR", new NumericalFilter(NumericalFilterOperator.LESS_THAN, 90., null), null,
                new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.shuntCompensators) {\n" +
                "  if ((equipment.terminal.voltageLevel.nominalV > 90.0)\n" +
                "     ) {\n" +
                "        contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
                "SHUNT_COMPENSATOR", new NumericalFilter(NumericalFilterOperator.GREATER_THAN, 90., null), null,
                    new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.lines) {\n" +
            "  if (\n" +
            "        (equipment.terminal1.voltageLevel.nominalV == 225.0 || equipment.terminal2.voltageLevel.nominalV == 225.0)\n" +
            "       ) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "LINE", new NumericalFilter(NumericalFilterOperator.EQUALITY, 225., null), null,
                new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.lines) {\n" +
                "  if (\n" +
                "  (lineMatch(equipment, ['FR','BE'], []))\n" +
                "       ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
                "LINE", null, null,
                countries, new HashSet<>()
                )));

        assertEquals("for (equipment in network.busbarSections) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV >= 63.0)\n" +
            "      && injectionMatch(equipment.terminal, ['FR','BE'])\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "BUSBAR_SECTION", new NumericalFilter(NumericalFilterOperator.GREATER_OR_EQUAL, 63., null), null,
            countries, new HashSet<>())));
    }
}
