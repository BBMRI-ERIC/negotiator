# Database creation and updates management

To handle database creation and updates the Negotiator uses the [Flyway](https://documentation.red-gate.com/flyway) tool.
Flyway keeps track of the changes to the Database schema in several migration scripts, each one having a version number. 
Whenever the database is changed, the corresponding migration script must be created and added 
in the correct path of the project.  
When the database is created, Flyway creates a table to store the current version of the schema deployed. When 
a new version of the Negotiator, containing changes to the DB, is deployed, Flyway applies all the missing 
migration scripts and updates its internal table.

## Migration type 

Flyway refers to changes to the database as migrations. 
There are two kind of migrations: versioned and repeatable.
Versioned migrations are applied only once for a database while repeatable are applied each time there is a change in the script.
To read more about migrations see Flyway [documentation](https://documentation.red-gate.com/fd/migrations-184127470.html)

A type of migration used in the Negotiator is the baseline migration.
Initially, Flyway was not adopted, therefore, old instances of the Negotiator didn't have the Flyway table.
In order to integrate Flyway with the databases created with those versions, a baseline migration with schema of the
database till that version has been created.
This tells Flyway that the db already present is the baseline database and it needs to apply only the other migrations.

## Migration file naming

The migrations file needs to follow the naming schema `<Type>_<Version>__<Description>.sql`.
 * Type is the type of migration (V: versioned, U: Undo, R: Repeatable, B: Baseline)
 * Version: the version of the migration with major and minor release (e.g., 2.1). This is not needed for repeatable
 * Description: something to understand what the migration does

## Data

Data are loaded using repeatable migrations. For example, if the Negotiator needs to be initialized with some data
(e.g., default access criteria), repeatable migrations should be created.  
Since repeatable migrations are repeated each time they change (i.e., when the checksum changes), when created 
for production, it is important to write them checking for data already present.
By default, the repeatable migrations are expected to be in the `/app/data` path.

## Paths

The migrations are in `resources/db` path. The subdirectory `migration` contains the schema for the two DBMS used in the 
negotiator. The `dev` and `test` subdirectories contains data (i.e., repeatable migrations) used for development and 
for tests. 

## Creation of a new migration

Whenever a change to the database (i.e., to the JPA entities) is needed, a new migration must be added. 
The migration should provide also updates for old data if necessary. For example, if a new not null column is added to a
table, the migration should also provide update stetements for old records. 
Intellij Ultimate Edition provides a [plugin](https://plugins.jetbrains.com/plugin/8597-flyway-migration-creation) to automatically generate the migrations 
