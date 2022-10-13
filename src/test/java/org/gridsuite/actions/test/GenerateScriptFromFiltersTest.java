/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.test;

import org.gridsuite.actions.server.FormToGroovyScript;
import org.gridsuite.actions.server.dto.FormContingencyList;
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
                "      && isLocatedIn(['FR','BE'], equipment)\n" +
                "     ) {\n" +
                "        contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "GENERATOR",
            90,
            "=",
            countries, new HashSet<>())));

        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(
                new FormContingencyList(null,
                    "TWO_WINDINGS_TRANSFORMER",
                    -1,
                    ">=",
                    new HashSet<>(), new HashSet<>()
            )));

        assertEquals("for (equipment in network.hvdcLines) {\n" +
            "  if (\n" +
            "  (isLocatedIn(['FR','BE'], equipment.converterStation1)\n" +
            "         || isLocatedIn(['FR','BE'], equipment.converterStation2))) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "HVDC_LINE",
            -1,
            "<=",
            countries, new HashSet<>())));

        assertEquals("for (equipment in network.hvdcLines) {\n" +
                "  if ((equipment.nominalV <= 225.0)\n" +
                "        && (isLocatedIn(['FR','BE'], equipment.converterStation1)\n" +
                "         || isLocatedIn(['FR','BE'], equipment.converterStation2))) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
                "HVDC_LINE",
                225,
                "<=",
                countries, new HashSet<>())));

        assertEquals("for (equipment in network.danglingLines) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV == 225.0)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
            null,
            "DANGLING_LINE",
            225,
            "=",
                new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.staticVarCompensators) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "STATIC_VAR_COMPENSATOR",
            -1,
            "=",
                new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.shuntCompensators) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV < 90.0)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "SHUNT_COMPENSATOR",
            90,
            "<",
                new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.lines) {\n" +
            "  if ((equipment.terminal1.voltageLevel.nominalV == 225.0\n" +
            "          || equipment.terminal2.voltageLevel.nominalV == 225.0)\n" +
            "     ) {\n" +
            "           contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "LINE",
            225,
            "=",
                new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.lines) {\n" +
                "  if (isLocatedIn(['FR','BE'], equipment)\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
                "LINE",
                -1,
                "=",
                countries, new HashSet<>()
                )));

        assertEquals("for (equipment in network.busbarSections) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV >= 63.0)\n" +
            "      && isLocatedIn(['FR','BE'], equipment)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(null,
            "BUSBAR_SECTION",
            63,
            ">=",
            countries, new HashSet<>())));
    }
}
