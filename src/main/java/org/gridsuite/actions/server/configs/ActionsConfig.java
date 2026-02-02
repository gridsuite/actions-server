package org.gridsuite.actions.server.configs;

import org.gridsuite.actions.ContingencyListEvaluator;
import org.gridsuite.actions.FilterProviderI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionsConfig {
    @Bean
    public ContingencyListEvaluator contingencyListEvaluator(
        FilterProviderI filterProvider
    ) {
        return new ContingencyListEvaluator(filterProvider);
    }
}
