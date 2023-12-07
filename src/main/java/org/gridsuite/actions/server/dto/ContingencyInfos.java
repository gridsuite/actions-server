package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.Contingency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContingencyInfos {
    private String id;
    private Contingency contingency;
    private Set<String> notFoundElements;

    public ContingencyInfos(Contingency contingency, Set<String> notFoundElements) {
        this.id = contingency == null ? null : contingency.getId();
        this.contingency = contingency;
        this.notFoundElements = notFoundElements;
    }
}
