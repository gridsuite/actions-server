package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.BatteryContingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.GeneratorContingency;
import com.powsybl.contingency.HvdcLineContingency;
import com.powsybl.contingency.LineContingency;
import com.powsybl.contingency.LoadContingency;
import com.powsybl.contingency.TwoWindingsTransformerContingency;
import com.powsybl.iidm.network.IdentifiableType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IdentifiableAttributes {
    private String id;
    private IdentifiableType type;
    private Double distributionKey;

    public ContingencyElement toContingencyElement() {
        switch (type) {
            case LINE -> {
                return new LineContingency(id);
            }
            case TWO_WINDINGS_TRANSFORMER -> {
                return new TwoWindingsTransformerContingency(id);
            }
            case GENERATOR -> {
                return new GeneratorContingency(id);
            }
            case LOAD -> {
                return new LoadContingency(id);
            }
            case BATTERY -> {
                return new BatteryContingency(id);
            }
            case HVDC_LINE -> {
                return new HvdcLineContingency(id);
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
    }
}
