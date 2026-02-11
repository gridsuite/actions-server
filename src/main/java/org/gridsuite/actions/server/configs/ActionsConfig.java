package org.gridsuite.actions.server.configs;

import org.gridsuite.actions.ContingencyListEvaluator;
import org.gridsuite.filter.FilterLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionsConfig {
    @Bean
    public ContingencyListEvaluator contingencyListEvaluator(
        FilterLoader filterProvider
    ) {
        return new ContingencyListEvaluator(filterProvider);
    }
}
