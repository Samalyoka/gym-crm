# Gym CRM System

Gym CRM built with Spring Core and Hibernate (JPA) on PostgreSQL.
Manages trainees, trainers and trainings.

## Tech stack

- Java 21, Maven
- Spring Framework 6.1 (Core, ORM, transactions), no Spring Boot
- Hibernate 6.5 with PostgreSQL, HikariCP connection pool
- SLF4J + Logback, JUnit 5, Mockito, H2 (tests only), JaCoCo

## Architecture

Hexagonal (onion) layout, dependencies point towards the domain model:

- `domain.model` - records (`Trainee`, `Trainer`, `Training`, `TrainingType`, `User`)
- `application.port.out` - DAO interfaces
- `application.service` - business logic, authentication, validation, transactions (`@Transactional`)
- `infrastructure.persistence` - Hibernate DAOs, JPA entities (`entity` package) and the mapper between entities and records
- `infrastructure.config` - Spring configuration (data source, Hibernate, transactions)
- `infrastructure.facade` - `GymFacade`, a single entry point for all 18 operations

JPA entities are separate classes in the infrastructure layer, so the domain model stays free of persistence annotations.

## Database

Create a database and run the schema:

```sql
CREATE DATABASE gymcrm;
```

then execute `src/main/resources/db/schema.sql` against `gymcrm`.
The script creates the tables and fills the constant `training_type` table.

Connection settings are in `src/main/resources/application.properties`.
The password is read from the `DB_PASSWORD` environment variable (default `postgres`) and is never committed.
Hibernate runs with `hbm2ddl.auto=validate`: it checks the schema but does not change it.

## Run

```
export DB_PASSWORD=your_password
mvn clean compile
```

Run `com.epam.gymcrm.Main` for a demo scenario that goes through the main operations.

## Tests

```
mvn clean test
```

- Service and facade tests use Mockito mocks.
- DAO tests are integration tests on in-memory H2 with the same Hibernate configuration.
- JaCoCo report: `target/site/jacoco/index.html`.

## Operations

All operations except profile creation require username and password.

1. Create trainer profile
2. Create trainee profile
3. Trainee username/password matching
4. Trainer username/password matching
5. Select trainer profile by username
6. Select trainee profile by username
7. Trainee password change
8. Trainer password change
9. Update trainer profile
10. Update trainee profile
11. Activate/de-activate trainee
12. Activate/de-activate trainer
13. Delete trainee profile by username (trainings are deleted by cascade)
14. Get trainee trainings list by criteria (from date, to date, trainer name, training type)
15. Get trainer trainings list by criteria (from date, to date, trainee name)
16. Add training
17. Get trainers not assigned to a trainee
18. Update trainee's trainers list

Username (`First.Last`, with a serial number if taken) and a random 10-character password are generated on profile creation.
Activate/de-activate flips the flag on every call.
