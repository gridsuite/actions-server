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
    Contingency contingency;
    Set<String> notFoundElements;
}
