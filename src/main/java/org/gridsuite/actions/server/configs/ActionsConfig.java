package org.gridsuite.actions.server.configs;

import org.gridsuite.actions.ContingencyListEvaluator;
import org.gridsuite.actions.FilterEvaluatorI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionsConfig {
    @Bean
    public ContingencyListEvaluator contingencyListEvaluator(
        FilterEvaluatorI filterEvaluator
    ) {
        return new ContingencyListEvaluator(filterEvaluator);
    }
}
