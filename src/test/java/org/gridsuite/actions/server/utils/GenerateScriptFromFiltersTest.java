/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.utils;

import org.gridsuite.actions.server.FormToGroovyScript;
import org.gridsuite.actions.server.dto.FormContingencyList;
import org.gridsuite.actions.server.dto.NumericalFilter;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
class GenerateScriptFromFiltersTest {
    @Test
    void generateScriptTestInjection() {
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
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
            "GENERATOR", new NumericalFilter(NumericalFilterOperator.EQUALITY, 90., null), null, null,
            countries, new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.danglingLines) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV == 225.0)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
            "DANGLING_LINE", new NumericalFilter(NumericalFilterOperator.EQUALITY, 225., null), null, null,
                new HashSet<>(), new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.staticVarCompensators) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
            "STATIC_VAR_COMPENSATOR", null, null, null,
                new HashSet<>(), new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.shuntCompensators) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV < 90.0)\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
            "SHUNT_COMPENSATOR", new NumericalFilter(NumericalFilterOperator.LESS_THAN, 90., null), null, null,
                new HashSet<>(), new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.shuntCompensators) {\n" +
                "  if ((equipment.terminal.voltageLevel.nominalV > 90.0)\n" +
                "     ) {\n" +
                "        contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "SHUNT_COMPENSATOR", new NumericalFilter(NumericalFilterOperator.GREATER_THAN, 90., null), null, null,
                    new HashSet<>(), new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.busbarSections) {\n" +
            "  if ((equipment.terminal.voltageLevel.nominalV >= 63.0)\n" +
            "      && injectionMatch(equipment.terminal, ['FR','BE'])\n" +
            "     ) {\n" +
            "        contingency(equipment.id) { equipments equipment.id }\n" +
            "  }\n" +
            "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
            "BUSBAR_SECTION", new NumericalFilter(NumericalFilterOperator.GREATER_OR_EQUAL, 63., null), null, null,
            countries, new HashSet<>(), new HashSet<>())));
    }

    @Test
    void generateScriptTestHvdcLine() {
        FormToGroovyScript formToScript = new FormToGroovyScript();
        LinkedHashSet<String> countries = new LinkedHashSet<>();
        countries.add("FR");
        countries.add("BE");
        LinkedHashSet<String> countries2 = new LinkedHashSet<>();
        countries2.add("ES");

        assertEquals("for (equipment in network.hvdcLines) {\n" +
                "  if (\n" +
                "  (hvdcLineMatch(equipment, ['FR','BE'], []))\n" +
                "       ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "HVDC_LINE", null, null, null,
                    new HashSet<>(), countries, new HashSet<>())));

        assertEquals("for (equipment in network.hvdcLines) {\n" +
                "  if ((equipment.nominalV <= 225.0)\n" +
                "        && (hvdcLineMatch(equipment, ['FR','BE'], []))\n" +
                "       ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "HVDC_LINE", new NumericalFilter(NumericalFilterOperator.LESS_OR_EQUAL, 225., null), null, null,
                    new HashSet<>(), countries, new HashSet<>())));

        assertEquals("for (equipment in network.hvdcLines) {\n" +
                "  if ((equipment.nominalV <= 225.0)\n" +
                "        && (hvdcLineMatch(equipment, ['FR','BE'], ['ES']))\n" +
                "       ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "HVDC_LINE", new NumericalFilter(NumericalFilterOperator.LESS_OR_EQUAL, 225., null), null, null,
                    new HashSet<>(), countries, countries2)));

        assertEquals("for (equipment in network.hvdcLines) {\n" +
                "  if ((equipment.nominalV <= 225.0)\n" +
                "        && (hvdcLineMatch(equipment, [], ['ES']))\n" +
                "       ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "HVDC_LINE", new NumericalFilter(NumericalFilterOperator.LESS_OR_EQUAL, 225., null), null, null,
                    new HashSet<>(), new HashSet<>(), countries2)));
    }

    @Test
    void generateScriptTestLine() {
        FormToGroovyScript formToScript = new FormToGroovyScript();
        LinkedHashSet<String> countries = new LinkedHashSet<>();
        countries.add("FR");
        countries.add("BE");
        LinkedHashSet<String> countries2 = new LinkedHashSet<>();
        countries2.add("ES");
        countries2.add("CA");

        assertEquals("for (equipment in network.lines) {\n" +
                "  if (\n" +
                "       (equipment.terminal1.voltageLevel.nominalV == 225.0\n" +
                "            || equipment.terminal2.voltageLevel.nominalV == 225.0)\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "LINE", null, new NumericalFilter(NumericalFilterOperator.EQUALITY, 225., null), null,
                    new HashSet<>(), new HashSet<>(), new HashSet<>())));

        assertEquals("for (equipment in network.lines) {\n" +
                "  if (\n" +
                "  (lineMatch(equipment, ['FR','BE'], ['ES','CA']))\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "LINE", null, null, null,
                    new HashSet<>(), countries, countries2)));

        assertEquals("for (equipment in network.lines) {\n" +
                "  if (\n" +
                "  (lineMatch(equipment, ['FR','BE'], []))\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "LINE", null, null, null,
                    new HashSet<>(), countries, new HashSet<>())));

        assertEquals("for (equipment in network.lines) {\n" +
                "  if (\n" +
                "  (lineMatch(equipment, [], ['ES','CA']))\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(new FormContingencyList(
                "LINE", null, null, null,
                    new HashSet<>(), new HashSet<>(), countries2)));
    }

    @Test
    void generateScriptTest2WTransfo() {
        FormToGroovyScript formToScript = new FormToGroovyScript();
        LinkedHashSet<String> countries = new LinkedHashSet<>();
        countries.add("FR");
        countries.add("BE");
        countries.add("ZA");
        NumericalFilter ge225 = new NumericalFilter(NumericalFilterOperator.GREATER_OR_EQUAL, 225., null);
        NumericalFilter lt400 = new NumericalFilter(NumericalFilterOperator.LESS_THAN, 400., null);
        NumericalFilter range225400 = new NumericalFilter(NumericalFilterOperator.RANGE, 225., 400.);
        NumericalFilter range2063 = new NumericalFilter(NumericalFilterOperator.RANGE, 20., 63.);

        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, null, null,
                        new HashSet<>(), new HashSet<>(), new HashSet<>()
                )));

        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       (equipment.terminal1.voltageLevel.nominalV >= 225.0\n" +
                "            || equipment.terminal2.voltageLevel.nominalV >= 225.0)\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, ge225, null,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // same filter, but in second position => same result
        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       (equipment.terminal1.voltageLevel.nominalV >= 225.0\n" +
                "            || equipment.terminal2.voltageLevel.nominalV >= 225.0)\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, null, ge225,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // just one range on filter 1
        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       ((equipment.terminal1.voltageLevel.nominalV >= 225.0 && equipment.terminal1.voltageLevel.nominalV <= 400.0)\n" +
                "            || (equipment.terminal2.voltageLevel.nominalV >= 225.0 && equipment.terminal2.voltageLevel.nominalV <= 400.0))\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, range225400, null,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // same on filter 2
        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       ((equipment.terminal1.voltageLevel.nominalV >= 225.0 && equipment.terminal1.voltageLevel.nominalV <= 400.0)\n" +
                "            || (equipment.terminal2.voltageLevel.nominalV >= 225.0 && equipment.terminal2.voltageLevel.nominalV <= 400.0))\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, null, range225400,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // simple operator on filter 1 & 2
        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       ((equipment.terminal1.voltageLevel.nominalV < 400.0 && equipment.terminal2.voltageLevel.nominalV >= 225.0)\n" +
                "            || (equipment.terminal1.voltageLevel.nominalV >= 225.0 && equipment.terminal2.voltageLevel.nominalV < 400.0))\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, lt400, ge225,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // switch
        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       ((equipment.terminal1.voltageLevel.nominalV >= 225.0 && equipment.terminal2.voltageLevel.nominalV < 400.0)\n" +
                "            || (equipment.terminal1.voltageLevel.nominalV < 400.0 && equipment.terminal2.voltageLevel.nominalV >= 225.0))\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, ge225, lt400,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // simple operator on filter 1, range on filter 2
        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       ((equipment.terminal1.voltageLevel.nominalV < 400.0 && equipment.terminal2.voltageLevel.nominalV >= 225.0 && equipment.terminal2.voltageLevel.nominalV <= 400.0)\n" +
                "            || (equipment.terminal1.voltageLevel.nominalV >= 225.0 && equipment.terminal1.voltageLevel.nominalV <= 400.0 && equipment.terminal2.voltageLevel.nominalV < 400.0))\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, lt400, range225400,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // switch
        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       ((equipment.terminal1.voltageLevel.nominalV >= 225.0 && equipment.terminal1.voltageLevel.nominalV <= 400.0 && equipment.terminal2.voltageLevel.nominalV < 400.0)\n" +
                "            || (equipment.terminal1.voltageLevel.nominalV < 400.0 && equipment.terminal2.voltageLevel.nominalV >= 225.0 && equipment.terminal2.voltageLevel.nominalV <= 400.0))\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, range225400, lt400,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // range operator on filter 1 & 2
        assertEquals("for (equipment in network.twoWindingsTransformers) {\n" +
                "  if (\n" +
                "       ((equipment.terminal1.voltageLevel.nominalV >= 225.0 && equipment.terminal1.voltageLevel.nominalV <= 400.0 && equipment.terminal2.voltageLevel.nominalV >= 20.0 && equipment.terminal2.voltageLevel.nominalV <= 63.0)\n" +
                "            || (equipment.terminal1.voltageLevel.nominalV >= 20.0 && equipment.terminal1.voltageLevel.nominalV <= 63.0 && equipment.terminal2.voltageLevel.nominalV >= 225.0 && equipment.terminal2.voltageLevel.nominalV <= 400.0))\n" +
                "       && transfoMatch(equipment, ['FR','BE','ZA'])\n" +
                "     ) {\n" +
                "           contingency(equipment.id) { equipments equipment.id }\n" +
                "  }\n" +
                "}\n\n\n", formToScript.generateGroovyScriptFromForm(
                    new FormContingencyList(
                        "TWO_WINDINGS_TRANSFORMER", null, range225400, range2063,
                        countries, new HashSet<>(), new HashSet<>()
                )));

        // just for better coverage: RANGE has no basic operator
        assertEquals("", range2063.operator());
    }
}
