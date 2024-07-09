package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.Contingency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/*
    @author Jamal KHEYYAD <jamal.kheyyad at rte-international.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Contingencies {
    List<Contingency> contingenciesFound;
    List<UUID> contingenciesNotFound;
}
