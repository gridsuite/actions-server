/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.commons.PowsyblException;
import org.apache.commons.io.IOUtils;
import org.gridsuite.actions.server.dto.FormContingencyList;
import org.gridsuite.actions.server.dto.NumericalFilter;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;
import org.springframework.core.io.ClassPathResource;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.nio.charset.Charset;

import static java.util.stream.Collectors.joining;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public class FormToGroovyScript {
    private final String transfo2WTemplate;
    private final String injectionTemplate;
    private final String lineTemplate;
    private final String hvdcLineTemplate;

    public FormToGroovyScript() {
        try {
            transfo2WTemplate = IOUtils.toString(new ClassPathResource("transfo2W.st").getInputStream(), Charset.defaultCharset());
            injectionTemplate = IOUtils.toString(new ClassPathResource("injection.st").getInputStream(), Charset.defaultCharset());
            lineTemplate = IOUtils.toString(new ClassPathResource("line.st").getInputStream(), Charset.defaultCharset());
            hvdcLineTemplate = IOUtils.toString(new ClassPathResource("hvdcLine.st").getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new PowsyblException("Unable to load templates for groovy script generation !!");
        }
    }

    private void addNominalVoltage(ST template, NumericalFilter filter, int rank) {
        if (filter == null) {
            return;
        }
        if (filter.getType() == NumericalFilterOperator.RANGE) {
            // range will use >= and <=
            template.add(rank == 0 ? "nominalVMin" : "nominalVMin" + rank, filter.getValue1());
            template.add(rank == 0 ? "nominalVMax" : "nominalVMax" + rank, filter.getValue2());
        } else {
            template.add(rank == 0 ? "nominalV" : "nominalV" + rank, filter.getValue1());
            template.add(rank == 0 ? "nominalVOperator" : "nominalVOperator" + rank, filter.operator());
        }
    }

    private void addCountry(ST template, FormContingencyList formContingencyList) {
        if (!formContingencyList.getCountries().isEmpty()) {
            template.add("countries", formContingencyList.getCountries().stream().collect(joining("','", "['", "']")));
        }
    }

    private void addCountries(ST template, FormContingencyList formContingencyList) {
        if (formContingencyList.getCountries1().isEmpty() && formContingencyList.getCountries2().isEmpty()) {
            return;
        }
        String set1 = formContingencyList.getCountries1().isEmpty() ? "[]" : formContingencyList.getCountries1().stream().collect(joining("','", "['", "']"));
        String set2 = formContingencyList.getCountries2().isEmpty() ? "[]" : formContingencyList.getCountries2().stream().collect(joining("','", "['", "']"));
        // always add both (easier template)
        template.add("countries1", set1);
        template.add("countries2", set2);
    }

    public String generateGroovyScriptFromForm(FormContingencyList formContingencyList) {
        String script;
        String equipmentsCollection;
        boolean twoCountry = false;
        boolean twoVoltage = false;

        switch (EquipmentType.valueOf(formContingencyList.getEquipmentType())) {
            case GENERATOR:
                equipmentsCollection = "generators";
                script = injectionTemplate;
                break;
            case STATIC_VAR_COMPENSATOR:
                equipmentsCollection = "staticVarCompensators";
                script = injectionTemplate;
                break;
            case SHUNT_COMPENSATOR:
                equipmentsCollection = "shuntCompensators";
                script = injectionTemplate;
                break;
            case BUSBAR_SECTION:
                equipmentsCollection = "busbarSections";
                script = injectionTemplate;
                break;
            case DANGLING_LINE:
                equipmentsCollection = "danglingLines";
                script = injectionTemplate;
                break;
            case HVDC_LINE:
                equipmentsCollection = "hvdcLines";
                script = hvdcLineTemplate;
                twoCountry = true;
                break;
            case LINE:
                equipmentsCollection = "lines";
                script = lineTemplate;
                twoCountry = true;
                twoVoltage = true;
                break;
            case TWO_WINDINGS_TRANSFORMER:
                equipmentsCollection = "twoWindingsTransformers";
                script = transfo2WTemplate;
                twoVoltage = true;
                break;
            default:
                throw new PowsyblException("Unknown equipment type");
        }

        ST template = new ST(script);

        template.add("collectionName", equipmentsCollection);

        if (twoCountry) {
            addCountries(template, formContingencyList);
        } else {
            addCountry(template, formContingencyList);
        }

        if (twoVoltage) {
            if (formContingencyList.getNominalVoltage1() != null && formContingencyList.getNominalVoltage2() != null) {
                addNominalVoltage(template, formContingencyList.getNominalVoltage1(), 1);
                addNominalVoltage(template, formContingencyList.getNominalVoltage2(), 2);
            } else if (formContingencyList.getNominalVoltage1() != null) {
                addNominalVoltage(template, formContingencyList.getNominalVoltage1(), 0);
            } else if (formContingencyList.getNominalVoltage2() != null) {
                addNominalVoltage(template, formContingencyList.getNominalVoltage2(), 0);
            }
        } else {
            addNominalVoltage(template, formContingencyList.getNominalVoltage(), 0);
        }

        return template.render();
    }
}
