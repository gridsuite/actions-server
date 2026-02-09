package org.gridsuite.actions.server.configs;

import org.gridsuite.actions.api.ContingencyListEvaluator;
import org.gridsuite.actions.api.ContingencyListEvaluatorFactory;
import org.gridsuite.filter.FilterLoader;
import org.gridsuite.filter.api.FilterEvaluator;
import org.gridsuite.filter.api.FilterEvaluatorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActionsConfig {
    @Bean
    public ContingencyListEvaluator contingencyListEvaluator(
        FilterEvaluator filterEvaluator
    ) {
        return ContingencyListEvaluatorFactory.create(filterEvaluator);
    }

    @Bean
    public FilterEvaluator filterEvaluator(
        FilterLoader filterLoader
    ) {
        return FilterEvaluatorFactory.create(filterLoader);
    }
}
