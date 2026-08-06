# Actions Server

[![Actions Status](https://github.com/gridsuite/actions-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/gridsuite/actions-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Aactions-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Aactions-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Description

The **actions-server** is a microservice of the [GridSuite](https://github.com/gridsuite) platform dedicated to **storing and instantiating contingency lists**.

Contingencies are used by security analysis to describe network elements whose loss we want to simulate (e.g. a line trip, a generator outage). The service provides the following capabilities:

- **Store and manage contingency lists** of two kinds: identifier-based and filter-based.
- **Evaluate contingency lists** against a specific network from the network-store, returning the resulting set of [PowSyBl](https://www.powsybl.org) `Contingency` objects.
- **Export contingencies** in PowSyBl JSON format, ready to be consumed by security analysis or sensitivity analysis services.
- **Manage contingency lists** (create, read, update, duplicate, delete) for both types.
- **Notify** other microservices via RabbitMQ when a contingency list is modified.

The evaluation logic (how contingency lists are resolved against a network) is provided by the [gridsuite-actions](https://github.com/gridsuite/actions) library, which the actions-server depends on. The server is responsible for persistence, REST exposure, and service orchestration; the library owns the evaluation logic.

---

## Technical Stack

- Spring Boot (Web, Data JPA, Actuator, Cloud Stream)
- PostgreSQL
- Liquibase
- RabbitMQ via Spring Cloud Stream
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus
- [gridsuite-actions](https://github.com/gridsuite/actions) (core DTO and evaluation library)
- [gridsuite-filter](https://github.com/gridsuite/filter) (filter model used with filter-server)
- [powsybl-network-store-client](https://github.com/powsybl/powsybl-network-store)

---

## Contingency List Types

### Identifier-Based (`IDENTIFIERS`)

An identifier-based contingency list is a named list of contingency groups, where each group directly references a set of **equipment IDs** (strings). Evaluation resolves those IDs against the network using PowSyBl's `IdentifierContingencyList`.

### Filter-Based (`FILTERS`)

A filter-based contingency list references one or more **filter UUIDs** managed by the [filter-server](https://github.com/gridsuite/filter-server). Each filter reference may optionally restrict which **equipment types** (e.g. `LINE`, `GENERATOR`, `BOUNDARY_LINE`) from the filter results should be included in the contingency.

At evaluation time, the actions-server fetches the filter definitions from filter-server, applies the equipment type restrictions, and builds the resulting contingencies. This type is more dynamic: the same contingency list will yield different contingencies depending on the network being studied.

---

## Development Scripts

Build Docker image:

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. After you generated a changeset do not forget to add it to git and in `src/main/resources/db/changelog/db.changelog-master.yml`.

---

## Interactions with Other Microservices

```
┌──────────────────────┐
│    actions-server    │──► network-store-server  (load network for contingency evaluation)
│                      │──► filter-server          (resolve filter definitions for filter-based lists)
└──────────────────────┘
         ▼
      RabbitMQ (element.update — emitted on every contingency list modification)
```

---


## Useful Links

- [PowSyBl contingency API](https://github.com/powsybl/powsybl-core/tree/main/contingency/contingency-api)
