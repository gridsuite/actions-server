package org.gridsuite.actions.server.configs;

import org.gridsuite.actions.api.ContingencyListEvaluator;
import org.gridsuite.actions.api.ContingencyListEvaluatorFactory;
import org.gridsuite.actions.api.FilterProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionsConfig {
    @Bean
    public ContingencyListEvaluator contingencyListEvaluator(
        FilterProvider filterProvider
    ) {
        return ContingencyListEvaluatorFactory.create(filterProvider);
    }
}
