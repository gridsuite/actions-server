/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.configs;

import com.datastax.oss.driver.api.core.CqlSession;
import org.gridsuite.actions.server.repositories.ScriptContingencyListRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.config.SessionFactoryFactoryBean;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;


/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Configuration
@PropertySource(value = {"classpath:cassandra.properties"})
@PropertySource(value = {"file:/config/cassandra.properties"}, ignoreResourceNotFound = true)
@EnableCassandraRepositories(basePackageClasses = {ScriptContingencyListRepository.class})
public class CassandraConfig {

    static final String KEYSPACE_NAME = "actions";

    @Bean
    public CqlSessionFactoryBean session(Environment env) {

        CqlSessionFactoryBean session = new CqlSessionFactoryBean();
        session.setContactPoints(env.getRequiredProperty("cassandra.contact-points"));
        session.setPort(Integer.parseInt(env.getRequiredProperty("cassandra.port")));
        session.setLocalDatacenter("datacenter1");
        session.setKeyspaceName(KEYSPACE_NAME);
        return session;
    }

    @Bean
    public CassandraMappingContext mappingContext() {
        return new CassandraMappingContext();
    }

    @Bean
    public CassandraConverter converter(CassandraMappingContext mappingContext) {
        return new MappingCassandraConverter(mappingContext);
    }

    @Bean
    public SessionFactoryFactoryBean sessionFactory(CqlSession session, CassandraConverter converter) {
        SessionFactoryFactoryBean sessionFactory = new SessionFactoryFactoryBean();
        sessionFactory.setSession(session);
        sessionFactory.setConverter(converter);
        return sessionFactory;
    }

    @Bean
    public CassandraAdminTemplate cassandraTemplate(CqlSession session, CassandraConverter converter) {
        return new CassandraAdminTemplate(session, converter);
    }
}
