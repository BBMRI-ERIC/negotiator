# Conventions brief — Flyway migration + JPA entity package

Read-only recon for the *Definition schema and entities* slab
(`.scratch/state-machine-implementation/issues/08-definition-schema-and-entities.md`).
Every claim below is cited `path:line`. Paths are relative to the repo root
`/home/claude/repos/bbmri-eric-negotiator/` unless absolute.

Where the codebase is inconsistent I say so, and name **the dominant pattern** and
**the most recent pattern** separately — for a new package, prefer the most recent.

---

## 0. Three things in the task prompt that are wrong

Stated up front so they are not re-derived.

1. **There is no partial-index precedent in this codebase.** A repo-wide grep for
   `CREATE [UNIQUE] INDEX` finds exactly two statements, neither with a `WHERE` clause:
   `backend/src/main/resources/db/migration/V28.0__create_template_table.sql:13` and
   `backend/src/main/resources/db/migration/V32.1__add_redelivery_reference_to_delivery.sql:4-5`.
   There is **no `CREATE UNIQUE INDEX` anywhere**, and no `WHERE` on any index. The
   partial unique indexes ticket 08 needs (`is_global_default`, the two guard-wiring
   `order` indexes) will be the first in the repo. Follow §2.5 for the naming to use.
2. **`scripts/test-backend.sh` does not exist at the repo root.** It exits 127. The
   script ships with the `focused-backend-tests` skill at
   `/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh` —
   recorded at `.scratch/state-machine-implementation/parity-gate.md:76-78`. See §4.4.
3. **`negotiation_resource_link.current_state` has no CHECK constraint.** Only
   `negotiation.current_state` does. See §5.

---

## 1. Flyway

### 1.1 Where migrations live

| Location | Purpose |
|---|---|
| `backend/src/main/resources/db/migration/` | The real, versioned schema. 51 files. |
| `backend/src/main/resources/db/dev/migration/R__Initial_data.sql` | Repeatable dev seed. |
| `backend/src/main/resources/db/test/migration/R__Initial_data.sql` | Repeatable test seed. |

`backend/target/classes/db/migration/` is build output — ignore it. So are the
copies under `.claude/worktrees/*` (they are separate git worktrees, not the project).

### 1.2 The real file-naming rule

Flyway's own rule, not a house style: `V<version>__<description>.sql`, where
`<version>` is dot-separated numeric parts compared part-by-part numerically.
The repo uses a **`<major>.<minor>` two-part version**, where the major is bumped for
a new logical change and the minor for follow-ups to it. That is why `V28.0`…`V28.5`
exist as a family
(`backend/src/main/resources/db/migration/V28.0__create_template_table.sql` through
`V28.5__populate_ui_settings_favicon.sql`).

Two-part is the **dominant and the most recent** form (V9.0 onward, and every file
since V16). Five early files use a bare integer with no dot — `V3__`, `V6__`, `V8__`,
`V15__`, `V20__`. `V20__add_org_res_ntw_new_columns.sql` sorts *before* `V20.1__` and
`V20.3__`, which is the intent; it is Flyway's numeric-part comparison, not luck.

Gaps are normal and harmless: there is no `V20.0`, `V20.2`, `V21.1`, `V32.0`.

Description casing is inconsistent — most are `lower_snake_case`
(`add_webhook_secret`), a minority are `Capitalised_snake`
(`V12.0__Add_sync_job.sql`, `V9.0__Remove_unnecessary_tables.sql`). **Dominant and
most recent: lower_snake_case.** One file has a stray dot in the description,
`V12.1__Add_sync_job._v1.sql` — an accident, do not imitate.

`B1__Baseline_migration.sql` is a Flyway *baseline* migration (prefix `B`), the
dumped pre-Flyway schema.

### 1.3 Current highest version → next free

Highest applied version is **`V35.0`**
(`backend/src/main/resources/db/migration/V35.0__migrate_custom_event_type_to_ping.sql`).

**Next free version: `V36.0`.** Suggested filename following house style:
`backend/src/main/resources/db/migration/V36.0__add_lifecycle_definition_schema.sql`.

### 1.4 How the dev/test seeds relate to `db/migration/`

They are *additional Flyway locations*, layered on top by profile — not a separate
mechanism:

- **dev:** `spring.flyway.locations: classpath:db/migration/,db/dev/migration`
  (`backend/src/main/resources/application-dev.yaml:22-23`).
- **test:** `spring.flyway.locations: classpath:db/migration/, classpath:db/test/migration`
  plus `clean-disabled: false` (`backend/src/main/resources/application-test.yaml:1-4`).
- **prod:** `classpath:db/migration/,filesystem:/app/data`
  (`backend/src/main/resources/application-prod.yaml:15-16`).

Both seeds are `R__` (repeatable): Flyway runs them **after all versioned migrations**,
and **re-runs them whenever their checksum changes**. Practical consequence for this
slab: a new table added in `V36.0` exists before the seed runs, so the seed *may*
insert into it — but ticket 08 is additive-only and nothing reads the new tables, so
**do not touch either `R__Initial_data.sql`.** Also note the test seed is a plain
`insert into …` script with hard-coded ids
(`backend/src/main/resources/db/test/migration/R__Initial_data.sql:61`,
`:111`) and is depended on by ~30 tests; changing it is a suite-wide blast radius.

`clean-disabled: false` in the test profile exists because the test context installs a
clean-then-migrate strategy:

```java
// backend/src/test/java/eu/bbmri_eric/negotiator/config/FlywayConfig.java:34-40
@Bean
public FlywayMigrationStrategy cleanMigrateStrategy() {
  return flyway -> { flyway.clean(); flyway.migrate(); };
}
```

So every test context build drops and re-applies the whole migration chain — a broken
`V36.0` fails *every* Testcontainer test, not just new ones.

### 1.5 The test context customizer and `loadTestData`

`backend/src/test/java/eu/bbmri_eric/negotiator/config/EnablePostgresTestContainerContextCustomizerFactory.java`

- `@EnabledPostgresTestContainer` (`:30-37`) is the meta-annotation that activates it:
  `@ActiveProfiles("test")`, `@DirtiesContext(AFTER_CLASS)`, `@Import(FlywayConfig.class)`.
- `createContextCustomizer` (`:39-52`) reads `loadTestData()` off whichever of
  `@RepositoryTest` / `@IntegrationTest` is present (`shouldLoadData`, `:54-68`).
- `customizeContext` (`:78-93`) injects the container's JDBC url/user/password and
  `spring.test.database.replace=NONE`, then:

```java
// :88-90
if (!loadTestData) {
  properties.put("spring.flyway.locations", "classpath:db/migration/");
}
```

**So: `loadTestData = false` (the default) overrides the yaml and runs `db/migration/`
only — an empty schema. `loadTestData = true` leaves the yaml value in place, i.e.
`db/migration/` + `db/test/migration/R__Initial_data.sql` — the seeded corpus.**

Container: a single static `PostgreSQLContainer<>("postgres:16-alpine")`, lazily
started and shared across the JVM
(`backend/src/test/java/eu/bbmri_eric/negotiator/config/PostgresContainerManager.java:47-58`).
It is a **real Postgres 16** — Postgres-specific DDL (`jsonb`, partial indexes,
`GENERATED BY DEFAULT AS IDENTITY`) is fine.

### 1.6 Versions from `backend/pom.xml`

| Thing | Version | Cite |
|---|---|---|
| Flyway (`flyway-core` + `flyway-database-postgresql`) | **11.10.0** | `backend/pom.xml:23`, `:228-236` |
| Testcontainers (`junit-jupiter` + `postgresql`) | **1.21.1** | `backend/pom.xml:22`, `:444-455` |
| Spring Boot parent | 3.5.15 | `backend/pom.xml:15-17` |
| Java | 21 | `backend/pom.xml:20` |
| PostgreSQL JDBC | 42.7.11 | `backend/pom.xml:52`, `:243-245` |
| hypersistence-utils-hibernate-63 | 3.9.10 | `backend/pom.xml:35`, `:238-241` |
| Lombok | 1.18.30 | `backend/pom.xml:44`, `:344-346` |
| surefire | 3.5.5 | `backend/pom.xml:86-90` |

---

## 2. DDL idiom in existing migrations

### 2.1 Casing and quoting

- **No identifier is ever double-quoted.** `grep '"'` across all migrations returns
  zero hits — including `B1__Baseline_migration.sql`. Everything is unquoted, hence
  folded to lower case by Postgres. *Consequence for this slab: a column literally
  named `order` would be a Postgres reserved word and would need quoting, which would
  be the first quoted identifier in the repo. Prefer `sort_order`/`ordinal`/`position`.*
- Keyword casing is **inconsistent**. Two camps:
  - `UPPERCASE` keywords, e.g. `V23.0__add_webhooks.sql`, `V13.0__add_info_submission.sql`,
    `V34.0__add_webhook_secret.sql`, `V32.1`, `V32.2`, `V35.0`.
  - `lowercase` keywords, e.g. `V16.0__move_resources_and_human_readable_to_negotiation.sql`,
    `V29.0__add_negotiation_display_id.sql`, `B1__Baseline_migration.sql`.
  **Dominant and most recent: UPPERCASE keywords** (every file from V31 onward).
- Table and column names: `snake_case`, singular table names (`negotiation`, `webhook`,
  `template`, `webhook_secret`, `information_submission`).
- Postgres-native types are used (`VARCHAR(n)`, `BIGINT`, `BOOLEAN`, `TEXT`,
  `TIMESTAMP`, `JSONB`, `BYTEA`), not portable/ANSI aliases.

### 2.2 Primary keys — three idioms, all present

| Idiom | Example |
|---|---|
| `id SERIAL PRIMARY KEY` / `BIGSERIAL PRIMARY KEY` | `V23.0__add_webhooks.sql:3`; `V28.0__create_template_table.sql:5` |
| `BIGINT GENERATED BY DEFAULT AS IDENTITY` + named `CONSTRAINT pk_x PRIMARY KEY (id)` | `V13.0__add_info_submission.sql:3,8` |
| `id VARCHAR(36) [NOT NULL] PRIMARY KEY` for UUID-keyed tables | `V23.0__add_webhooks.sql:12`; `V34.0__add_webhook_secret.sql:2` |
| Composite PK on a link table | `V16.0__...:11` — `primary key (negotiation_id, resource_id)` |

`B1__Baseline_migration.sql` uses the legacy dump form: an explicit
`create sequence x_id_seq` (`:109-116`) + `alter sequence … OWNED BY …` +
`alter table ONLY x alter COLUMN id SET DEFAULT nextval('x_id_seq'::regclass)`
(`:329-350`). **Do not imitate B1** — it is a pre-Flyway dump.

**Dominant + most recent for a new numeric-PK table: `BIGSERIAL PRIMARY KEY`**
(`V28.0__create_template_table.sql:5`, the most recent table creation with a numeric
key). It pairs with `GenerationType.IDENTITY` on the entity (§3.4).

### 2.3 Foreign keys — two idioms

Inline, unnamed (older, `V16.0__...:8-9`):

```sql
create table negotiation_resource_link
(
    negotiation_id character varying(255) not null references negotiation (id),
    resource_id    bigint                 not null references resource (id),
    current_state  varchar(255),
    primary key (negotiation_id, resource_id)
);
```

Named constraint, which is **dominant and most recent** — either inline in the
`CREATE TABLE` or as a follow-up `ALTER TABLE … ADD CONSTRAINT`:

```sql
-- backend/src/main/resources/db/migration/V23.0__add_webhooks.sql:10-19
CREATE TABLE delivery
(
    id               VARCHAR(36) PRIMARY KEY,
    webhook_id       BIGINT    NOT NULL,
    content          JSON      NOT NULL,
    http_status_code INT,
    error_message    VARCHAR(255),
    at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook FOREIGN KEY (webhook_id) REFERENCES webhook (id) ON DELETE CASCADE
);
```

```sql
-- backend/src/main/resources/db/migration/V34.0__add_webhook_secret.sql:7-12
ALTER TABLE webhook
ADD COLUMN secret_id VARCHAR(36);

ALTER TABLE webhook
ADD CONSTRAINT fk_webhook_secret
FOREIGN KEY (secret_id) REFERENCES webhook_secret(id);
```

FK constraint naming is inconsistent: `fk_webhook`, `fk_webhook_secret` (lower snake,
most recent), `fkey_discovery_service_id` (`V16.0:4`),
`FK_INFORMATIONSUBMISSION_ON_NEGOTIATION` (screaming, `V13.0:12` — a Liquibase-style
import, an outlier). **Use `fk_<table>_<column-or-target>` lower snake.**
`ON DELETE CASCADE` appears only in `V23.0:18`; otherwise no delete rules are declared.

### 2.4 The exact idiom for adding a nullable column (what §5 needs)

Most recent and cleanest, from `V32.1__add_redelivery_reference_to_delivery.sql:1-5`:

```sql
ALTER TABLE delivery
ADD COLUMN IF NOT EXISTS redelivery_of_delivery_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_delivery_redelivery_of
ON delivery (redelivery_of_delivery_id);
```

The three-step "add nullable, backfill, set NOT NULL" pattern is
`V32.2__add_event_type_to_delivery.sql:1-9` — **ticket 08 explicitly forbids the third
step here** (`.scratch/state-machine-implementation/issues/08-definition-schema-and-entities.md:19`).

`IF NOT EXISTS` / `IF EXISTS` guards appear on the most recent statements (V32.1,
V23.0:1) but not universally. Harmless; use them.

### 2.5 Indexes

Only two exist, both non-partial, both `idx_`-prefixed:

- `CREATE INDEX idx_template_name ON template(name);` — `V28.0__create_template_table.sql:13`
  (preceded by a `--` comment explaining why, `:12`).
- `CREATE INDEX IF NOT EXISTS idx_delivery_redelivery_of ON delivery (redelivery_of_delivery_id);`
  — `V32.1:4-5`.

Naming convention in force: **`idx_<table>_<column(s)>`**. For the new partial unique
indexes (no precedent — see §0.1), the consistent extension is
`uq_<table>_<columns>` or `idx_<table>_<columns>` with an explicit `WHERE`, e.g.

```sql
CREATE UNIQUE INDEX uq_state_machine_definition_global_default
ON state_machine_definition (is_global_default)
WHERE is_global_default;
```

Uniqueness elsewhere is expressed as a **column-level `UNIQUE`**
(`V28.0:6` — `name VARCHAR(255) NOT NULL UNIQUE`), never as a standalone unique index.
For `(family_key, version)` — a plain multi-column uniqueness with no predicate — the
in-house idiom would be a table constraint; there is no existing example of a
multi-column `UNIQUE` in the migrations, so either form is a first.

### 2.6 CHECK constraints

Every CHECK in the repo is an enum-value whitelist, all in the same verbose
`::character varying[]` cast form inherited from the original pg_dump. Example, and
the one that matters for §5:

```sql
-- backend/src/main/resources/db/migration/V22.0__add_draft_state_to_check_constraint.sql:1-5
ALTER TABLE negotiation DROP CONSTRAINT negotiation_current_state_check;

ALTER TABLE negotiation
    ADD CONSTRAINT negotiation_current_state_check CHECK (((current_state)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, ...])::text[])));
```

Naming: `<table>_<column>_check` — Postgres' own default name, consistently kept
(`B1:105,128,137,159,249,250,308`, `V19.0:9`, `V12.1:7`, `V22.0`). *Ticket 08 adds
nothing that needs a CHECK; enum-valued columns in the new schema (`scope`,
`required_authority`) would follow §3.6 (`@Enumerated(STRING)` + plain `VARCHAR`) and
existing practice is inconsistent about whether such a column gets a CHECK —
`webhook_delivery.event_type` (`V32.2:2`) has none, `ui_parameter.type` (`V19.0:9`)
does. **Most recent pattern: no CHECK.** ADR 0009's rollout also plans to drop the
existing state CHECKs (`.scratch/state-machine-redesign/issues/11-migration-rollout-path.md:35`).*

### 2.7 jsonb DDL

Three occurrences only:

- `B1__Baseline_migration.sql:124` — `payload jsonb,` (the `negotiation.payload` column)
- `B1__Baseline_migration.sql:258` — `payload jsonb NOT NULL,`
- `V13.0__add_info_submission.sql:7` — `payload        JSONB,`

Plus one plain `JSON` (not `jsonb`): `V23.0__add_webhooks.sql:14` —
`content JSON NOT NULL` on the delivery table.

**Dominant: `JSONB`. Use `JSONB` for the new `params` columns.**

---

## 3. Entity idiom

### 3.1 Package layout

`backend/src/main/java/eu/bbmri_eric/negotiator/` — one package per bounded feature,
entity + repository + service + controller + DTOs **all in the same flat package**:

```
attachment/ common/ discovery/ email/ form/ governance/ info_requirement/
info_submission/ negotiation/ notification/ post/ settings/ template/ user/ webhook/
```

Sub-packages appear only when a feature is large (`negotiation/dto`, `negotiation/mappers`,
`negotiation/pdf`, `negotiation/request`, `negotiation/state_machine/{negotiation,resource}`;
`form/repository`, `form/value_set`; `governance/{network,organization,resource}`;
`webhook/event`). **Package names are `lower_snake_case`** (`info_submission`,
`state_machine`, `value_set`) — unusual for Java, but consistent.

Spring Modulith is on the classpath (`backend/pom.xml:146-147,199`) and three packages
carry a `package-info.java`:
`negotiation/package-info.java:1` — `@ApplicationModule(type = ApplicationModule.Type.OPEN)`,
plus `common/package-info.java` and `governance/package-info.java`. There is one
modulith test, `backend/src/test/java/eu/bbmri_eric/negotiator/notification/NotificationModuleTest.java`.
A new top-level package needs no `package-info.java` unless you want module semantics.

Component/entity scanning is glob-based and covers any new sub-package:

```java
// backend/src/main/java/eu/bbmri_eric/negotiator/common/configuration/BaseConfig.java:18-20
@EnableJpaAuditing
@EntityScan(basePackages = {"eu.bbmri_eric.negotiator.*"})
@EnableJpaRepositories(basePackages = {"eu.bbmri_eric.negotiator.*"})
```

Note the `*`: entities placed **directly** in `eu.bbmri_eric.negotiator` would not be
scanned. Any new package one level down is picked up automatically — no config change.

**Visibility idiom (relevant to ticket 08's "no production code outside package X
reads this"):** the most recent feature packages make the entity and repository
**package-private** — `class Template` (`template/Template.java:37`) and
`interface TemplateRepository` (`template/TemplateRepository.java:13`) have no `public`
modifier. That is the strongest in-language enforcement available and it is already
house style. Older packages are all `public`.

### 3.2 Base / audit superclass

`backend/src/main/java/eu/bbmri_eric/negotiator/common/AuditEntity.java:23-44`:

```java
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditEntity {
  @CreatedDate @Exclude private LocalDateTime creationDate;
  @LastModifiedDate @Exclude private LocalDateTime modifiedDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  @CreatedBy @Exclude private Person createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "modified_by")
  @LastModifiedBy @Exclude private Person modifiedBy;
  ...
}
```

Extending it costs four DB columns: `creation_date`, `modified_date`, `created_by
BIGINT REFERENCES person(id)`, `modified_by BIGINT REFERENCES person(id)`. It is
**not** universally used — `Template`, `Delivery`, `WebhookSecret`,
`InformationSubmission`, `NegotiationResourceLink` do not extend it; `Negotiation`
(`negotiation/Negotiation.java:53`) and `Attachment` (`attachment/Attachment.java:203`)
do. **Most recent pattern: do not extend `AuditEntity`** unless the feature genuinely
needs who/when auditing. Ticket 08's definition rows plausibly do; that's a design
call, not a convention.

### 3.3 Lombok

Universal. The canonical recent stack (most recent entity, `template/Template.java:30-37`):

```java
@Entity
@Table(name = "template")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder(access = AccessLevel.PROTECTED)
class Template { ... }
```

`webhook/Delivery.java:111-116` is the same shape minus the builder, plus
`@EqualsAndHashCode`. `webhook/WebhookSecret.java:107-112` uses `@Data`. Older entities
use unrestricted `@NoArgsConstructor @AllArgsConstructor @Builder @Getter @Setter`
(`negotiation/Negotiation.java:45-52`, `attachment/Attachment.java:195-202`).

**Dominant: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`. Most recent:
the same with `access = AccessLevel.PROTECTED` on both constructors.**
`equals`/`hashCode` are hand-written on entities that need them
(`Negotiation.java:245-260`, `Template.java:73-87`, `NegotiationResourceLink.java:41-52`),
not Lombok-generated — except `Delivery` (`@EqualsAndHashCode`) and `WebhookSecret` (`@Data`).

### 3.4 Id generation

Three strategies, by key type:

| Key | Annotation | Cite |
|---|---|---|
| `Long`, DB `BIGSERIAL`/`IDENTITY` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | `template/Template.java:39-41`; `info_submission/InformationSubmission.java:63-65` |
| `String` UUID | `@Id @GeneratedValue(generator = "uuid") @UuidGenerator @Column(name = "id")` | `negotiation/Negotiation.java:55-59`; `attachment/Attachment.java:205-209` |
| `String` UUID (terser, most recent) | `@Id @GeneratedValue @UuidGenerator private String id;` | `webhook/WebhookSecret.java:114` |
| `String` UUID, app-assigned | `@Id private String id;` + `@PrePersist` generating `UUID.randomUUID().toString()` | `webhook/Delivery.java:119`, `:165-170` |

**For new numeric-PK definition tables: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;`
against `BIGSERIAL PRIMARY KEY`.** That is both the dominant and the most recent
numeric idiom.

### 3.5 Naming strategy — the `familyKey` → `family_key` question

**Yes, it is automatic. `@Column` is not required and is usually omitted.**

There is **no** `spring.jpa.hibernate.naming.physical-strategy` / `implicit-strategy`
configured anywhere — a grep across `backend/src` (all `*.yaml`, `*.java`, `*.xml`) and
`backend/pom.xml` returns nothing. Spring Boot's default therefore applies:
`CamelCaseToUnderscoresNamingStrategy`, which lower-snake-cases every implicit name.

Proof in the code, `webhook/Delivery.java`:
- `:122` `private Long webhookId;` → column `webhook_id` (`V23.0:13`)
- `:136` `private Integer httpStatusCode;` → `http_status_code` (`V23.0:15`)
- `:139` `private String errorMessage;` → `error_message` (`V23.0:16`)
- `:142` `private String redeliveryOfDeliveryId;` → `redelivery_of_delivery_id` (`V32.1:2`)

None of them carry a `@Column(name = …)`. Likewise `WebhookSecret.encryptedSecret` →
`encrypted_secret` (`WebhookSecret.java:117` vs `V34.0:3`).

`@Column(name = …)` is used only when the Java name **deliberately differs** from the
column — e.g. `@Column(name = "is_editable") private boolean editable;`
(`info_submission/InformationSubmission.java:83-84`), or as redundant belt-and-braces
on `@Id` fields (`Negotiation.java:58`). `@Column` *is* used without `name` to carry
`nullable`, `unique`, `columnDefinition`, `updatable`
(`Template.java:45,49,57`; `Delivery.java:121,128,131`).

`@Table(name = …)` is likewise optional — present on `Attachment`, `Template`,
`WebhookSecret`, and **required** on `Delivery` (`@Table(name = "webhook_delivery")`,
`Delivery.java:116`) because the class was renamed away from the table; absent on
`Negotiation`, `NegotiationResourceLink`, `InformationSubmission`.

**So: name the fields in camelCase, write the DDL in snake_case, and do not add
`@Column(name=…)`.**

### 3.6 Enums

Uniformly `@Enumerated(EnumType.STRING)` against a `VARCHAR` column. No
`@Convert`/`AttributeConverter` for enums anywhere, no ordinal mapping.

```java
// backend/src/main/java/eu/bbmri_eric/negotiator/webhook/Delivery.java:131-133
@Column(nullable = false)
@Enumerated(EnumType.STRING)
private WebhookEventType eventType;
```

```java
// backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/NegotiationResourceLink.java:24-25
@Enumerated(EnumType.STRING)
private NegotiationResourceState currentState;
```

Also `Negotiation.java:103-105`. Enum classes themselves live beside the entity and
often carry `@Getter` + label/description fields
(`negotiation/state_machine/negotiation/NegotiationState.java:5-29`).

### 3.7 jsonb mapping — **the codebase is split; pick one deliberately**

**Idiom A — hypersistence-utils (dominant, 2 of 3 sites).**
Dependency already present: `io.hypersistence:hypersistence-utils-hibernate-63:3.9.10`
(`backend/pom.xml:238-241`, version property `:35`).

```java
// backend/src/main/java/eu/bbmri_eric/negotiator/info_submission/InformationSubmission.java:79-81
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
...
@Type(JsonType.class)
@Column(columnDefinition = "json")
private String payload;
```

`Negotiation` uses the same type but with two extra pieces of machinery — a class-level
`@Convert` and a `ColumnTransformer` — because it also exposes a `@Formula` over the
same column:

```java
// backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/Negotiation.java:51 (class level)
@Convert(converter = JsonType.class, attributeName = "json")
...
// :91-97
@Formula(value = "JSON_EXTRACT_PATH_TEXT(payload, 'project', 'title')")
private String title;

@Type(JsonType.class)
@Column(columnDefinition = "json")
@ColumnTransformer(read = "payload::text", write = "?::json")
private String payload;
```

Note both declare `columnDefinition = "json"` while the actual DDL column is `jsonb`
(`B1:124`, `V13.0:7`). That mismatch is inert because `ddl-auto` is never `create`
(no `spring.jpa.hibernate.ddl-auto` is set anywhere; Flyway owns the schema).

**Idiom B — Hibernate 6 native (most recent, 1 site, no extra dependency).**

```java
// backend/src/main/java/eu/bbmri_eric/negotiator/webhook/Delivery.java:103-104, 124-125
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
...
@JdbcTypeCode(SqlTypes.JSON)
private String content;
```

Two annotations, no `@Column`, no `columnDefinition`. `Attachment` uses the same
`@JdbcTypeCode` mechanism for a different type
(`attachment/Attachment.java:223-225`, `@JdbcTypeCode(Types.VARBINARY)` + `columnDefinition = "BYTEA"`).

**Recommendation for the new `params` columns: Idiom B.** It is the most recent, needs
no third-party import, and works against a `JSONB` column. Exact stack to copy:

```java
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@JdbcTypeCode(SqlTypes.JSON)
private String params;   // DDL: params JSONB
```

If you prefer to match the dominant pattern instead, copy `InformationSubmission`
verbatim (`@Type(JsonType.class)` + `@Column(columnDefinition = "json")`) — the
dependency coordinates are `io.hypersistence:hypersistence-utils-hibernate-63:3.9.10`,
already declared, nothing to add. **Do not mix the two in one package.**

All four jsonb/binary sites map to **`String`** (or `byte[]`), never to a POJO or
`Map`. Keep that.

---

## 4. Repositories and tests

### 4.1 Repository idiom

- Spring Data JPA interfaces, named `<Entity>Repository`, living in the same package
  as the entity. 23 of them; full list under
  `backend/src/main/java/eu/bbmri_eric/negotiator/*/[*/]*Repository.java`.
- Always annotated `@Repository` even though it is optional.
- Minimal case: `webhook/WebhookSecretRepository.java:6-7` —
  `@Repository public interface WebhookSecretRepository extends JpaRepository<WebhookSecret, String> {}`.
- Typical case adds `JpaSpecificationExecutor` and derived query methods with javadoc:
  `template/TemplateRepository.java:12-31` (and note it is **package-private** —
  the most recent example).
- `negotiation/NegotiationRepository.java` is the maximal example: `@Query` with both
  JPQL (`:21`, `:41-49`) and `nativeQuery = true` (`:25-38`), derived methods
  (`:24`, `:51`), and projection-returning finders (`:19`).
- **Custom-impl pattern:** exactly one instance in the repo —
  `governance/network/stats/NetworkStatsRepositoryImpl.java` (the Spring Data
  `<Interface>Impl` fragment convention). Not needed for this slab.

### 4.2 `@IntegrationTest` and `@RepositoryTest`

`backend/src/test/java/eu/bbmri_eric/negotiator/util/IntegrationTest.java:14-23`:

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Target(ElementType.TYPE) @Documented @Inherited @Retention(RetentionPolicy.RUNTIME)
@EnabledPostgresTestContainer
public @interface IntegrationTest {
  boolean loadTestData() default false;
}
```

`backend/src/test/java/eu/bbmri_eric/negotiator/util/RepositoryTest.java:14-23` is the
same but `@DataJpaTest(showSql = false)` — persistence slice only. **For this slab,
`@RepositoryTest` is the right annotation** (§4.3).

`loadTestData` semantics are in §1.5: `false` (default) = migrations only, empty
tables, build your own fixtures; `true` = the seeded `R__Initial_data.sql` corpus.

### 4.3 Template tests to copy

All three hit a real Postgres 16 via Testcontainers.

1. **`backend/src/test/java/eu/bbmri_eric/negotiator/integration/repository/AttachmentRepositoriesTest.java`**
   — the cleanest template. `@RepositoryTest` (`:30`), constants for fixture ids
   (`:32-37`), `@Autowired` repository fields with no modifier (`:39-44`),
   `@BeforeEach setUp()` building the object graph. Best model for "new entity
   round-trips through a real DB".
2. **`backend/src/test/java/eu/bbmri_eric/negotiator/integration/repository/NegotiationRepositoryTest.java`**
   — `@RepositoryTest` + `@Import(MockUserDetailsService.class)` (`:40-41`), needed
   whenever auditing (`@CreatedBy`) must resolve a principal. Copy this one if the new
   entities extend `AuditEntity`. Also shows the text-block JSON payload fixture
   (`:57-73`).
3. **`backend/src/test/java/eu/bbmri_eric/negotiator/integration/repository/OrganizationRepositoryTest.java`**
   — `@RepositoryTest(loadTestData = true)` (`:19`), the seeded-corpus variant, with
   assertions against seed row counts (`:32`).

Others in the same directory: `AccessFormElementSetRepoTest`, `NetworkRepositoryTest`,
`PersonRepositoryTest`, `ResourceRepositoryTest`, plus
`backend/src/test/java/eu/bbmri_eric/negotiator/notification/NotificationRepositoryTest.java`
(which `@Autowired`s the raw `DataSource`, `:26`).

### 4.4 How tests are actually run

From `.scratch/state-machine-implementation/parity-gate.md:69-78`: **the Nix dev shell
is not active in an agent session** — bare `mvn`/`java` are not on `PATH`. Every Maven
command runs from the repository root prefixed with `nix develop .#opencode --command`.
`scripts/test-backend.sh` does **not** exist at the repo root (exit 127); the script
lives with the `focused-backend-tests` skill.

Focused run:

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'AttachmentRepositoriesTest'
```

`-f backend` sets the Maven project dir when invoking from the repo root; the selector
is Surefire syntax (`NetworkTest`, `NetworkTest#method`, `'*ServiceTest'`, comma lists).
`--all` is required for a full-suite run. Full Maven output lands in
`backend/target/test-run.log`; read pass/fail out of `backend/target/surefire-reports/`
and check mtimes, not a summary line
(`/home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh:12-41`,
`parity-gate.md:79-81`).

The parity gate this slab must not break
(`.scratch/state-machine-implementation/parity-gate.md:16-22`):

```
nix develop .#opencode --command \
  /home/claude/.claude/skills/focused-backend-tests/scripts/test-backend.sh \
  -f backend 'eu.bbmri_eric.negotiator.characterization.**' -DexcludedGroups=intended-delta
```
Expected: **255 tests in 24 classes, 0 failures, 0 errors, 1 skipped** (~8.5 min).

Formatter — not bound to `test`, run before committing any Java
(`parity-gate.md:124-128`):

```
nix develop .#opencode --command mvn -f backend -q com.spotify.fmt:fmt-maven-plugin:2.25:format
```

Other environment notes worth knowing (`parity-gate.md:82-86`): `backend/target/` gets
polluted by the JDT language server compiling without Lombok (~200 bogus
`cannot find symbol` errors — one `clean` fixes it); Testcontainers needs docker group
membership; `MailConnectException` noise is expected.

---

## 5. The two columns this slab extends

### 5.1 `negotiation.current_state`

- **DDL:** `current_state character varying(255),` —
  `backend/src/main/resources/db/migration/B1__Baseline_migration.sql:123`. Nullable.
- **CHECK constraint:** yes. Created at `B1:128` with 7 values; dropped and re-added
  with `DRAFT` prepended (8 values) at
  `V22.0__add_draft_state_to_check_constraint.sql:1-5`. Constraint name
  `negotiation_current_state_check`. Current permitted set: `DRAFT, SUBMITTED,
  APPROVED, DECLINED, IN_PROGRESS, PAUSED, CONCLUDED, ABANDONED` — exactly the eight
  constants of
  `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/state_machine/negotiation/NegotiationState.java:6-17`.
- **Entity field:** `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/Negotiation.java:103-105`

```java
@Setter(AccessLevel.NONE)
@Enumerated(EnumType.STRING)
private NegotiationState currentState;
```

  The Lombok setter is suppressed because a hand-written `setCurrentState` also appends
  a `NegotiationLifecycleRecord` (`Negotiation.java:132-135`). No `@Column`.

### 5.2 `negotiation_resource_link.current_state`

- **DDL:** `backend/src/main/resources/db/migration/V16.0__move_resources_and_human_readable_to_negotiation.sql:6-12`

```sql
create table negotiation_resource_link
(
    negotiation_id character varying(255) not null references negotiation (id),
    resource_id    bigint                 not null references resource (id),
    current_state  varchar(255),
    primary key (negotiation_id, resource_id)
);
```

- **CHECK constraint: none.** `grep negotiation_resource_link` across all migrations
  returns only `V16.0:6,23,29` — the table is never altered again. The 12-value CHECK
  quoted in `B1:308` belongs to the *old* `resource_state_per_negotiation` table, which
  `V16.0:41` drops. This contradicts the assumption in the task prompt.
- **Entity:** `backend/src/main/java/eu/bbmri_eric/negotiator/negotiation/NegotiationResourceLink.java:16-31`

```java
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class NegotiationResourceLink {
  @EmbeddedId private NegotiationResourceLinkId id;

  @Enumerated(EnumType.STRING)
  private NegotiationResourceState currentState;
  ...
}
```

  The PK is an `@Embeddable` composite of two `@ManyToOne(optional = false) @JoinColumn`
  associations (`negotiation/NegotiationResourceLinkId.java:15-28`), so the entity has
  **no simple id field**. Adding a column here means adding a plain field to
  `NegotiationResourceLink`, not to the id class. No `@Table` annotation — the table
  name is derived from the class name by the default naming strategy (§3.5).

### 5.3 What the pin column should look like

Per ticket 08 line 19, **nullable** on both, no NOT NULL, no backfill. Following §2.4
and §2.3:

```sql
ALTER TABLE negotiation
ADD COLUMN IF NOT EXISTS definition_version_id BIGINT;

ALTER TABLE negotiation
ADD CONSTRAINT fk_negotiation_definition_version
FOREIGN KEY (definition_version_id) REFERENCES state_machine_definition (id);

ALTER TABLE negotiation_resource_link
ADD COLUMN IF NOT EXISTS definition_version_id BIGINT;

ALTER TABLE negotiation_resource_link
ADD CONSTRAINT fk_negotiation_resource_link_definition_version
FOREIGN KEY (definition_version_id) REFERENCES state_machine_definition (id);
```

Entity side — note §3.5, no `@Column(name=…)` needed; a `Long` field named
`definitionVersionId` maps to `definition_version_id` automatically. Whether to map it
as a raw `Long` or a `@ManyToOne` association is a design call: the codebase does both
(`Delivery.webhookId` is a raw `Long`, `Delivery.java:121-122`, while
`InformationSubmission.negotiation` is a `@ManyToOne`,
`InformationSubmission.java:75-77`). A raw `Long` keeps `Negotiation` from importing
the new package, which serves the "no production code outside package X references
these types" gate in §6.

---

## 6. The mechanical guard-test precedent

`backend/src/test/java/eu/bbmri_eric/negotiator/characterization/guard/CharacterizationImportGuardTest.java`
(247 lines, plain JUnit 5 — **no Spring context, no ArchUnit dependency**).

### The technique

It **scans Java source files as text** and fails on regex hits, reporting `file:line`.
Six moving parts, all reusable verbatim:

1. **Locate the scan root from the working directory, not the classpath**, walking
   upwards and trying both `""` and `"backend"` as a module prefix
   (`:184-202`). Rationale is in the javadoc at `:178-183`: the rules are about
   *source text*, and a compiled class no longer shows an import. It throws
   `IllegalStateException` if the root is not found — "the guard must never pass by
   finding nothing" (`:198-201`).
2. **Walk the tree** with `Files.walk`, keep regular `.java` files, sort
   (`:167-176`).
3. **Blank out comments before matching** (`:208-236`) — a line comment or block
   comment mentioning a forbidden name must not itself be a violation. Line numbering
   is preserved by substituting `""` for elided lines.
4. **Rules as two predicates**: `scan(Predicate<Path> fileIsSubjectToRule,
   Predicate<String> lineBreaksRule)` (`:129-144`), collecting
   `record Violation(Path file, int line, String text)` (`:246`).
5. **Explicit, named exemptions** as constants, each with javadoc explaining why:
   the guard file itself (`:54`, skipped at `:133` — it cannot state the names it
   forbids without containing them), a throwaway package (`:45`, tested by
   `isInDumpPackage`, `:162-165`), and a single sanctioned adapter file (`:48`).
6. **A meta-test that the guard is not vacuous** (`:113-127`): asserts the scan found
   ≥2 sources and that each named exemption still exists, so a rename cannot silently
   disable the rule.

Patterns use **word boundaries** deliberately (`:56-69`):

```java
private static final Pattern STATE_MACHINE_LIBRARY =
    Pattern.compile("org\\.springframework\\.statemachine");

private static final List<Pattern> LIFECYCLE_ENUMS =
    List.of(
        Pattern.compile("\\bNegotiationState\\b"),
        Pattern.compile("\\bNegotiationEvent\\b"), ...);
```

— the javadoc at `:59-63` explains that `NegotiationStateChangeEvent` must not be
mistaken for `NegotiationState`.

Failure messages are built by `report(...)` (`:146-160`): headline, violation count,
then one `absolutePath:line` + the offending source line each, then a remedy paragraph
naming the exemptions. That is why a violation is actionable without opening the guard.

### Reusing it for "no production code outside package X references these new types"

Same skeleton, three substitutions:

- **Scan root** → `src/main/java/eu/bbmri_eric/negotiator` instead of
  `src/test/java/eu/bbmri_eric/negotiator/characterization` (change
  `CHARACTERIZATION_PACKAGE_PATH` at `:37-38` and the `resolve("src/test/java")` at
  `:191`).
- **`fileIsSubjectToRule`** → "not inside the new package's own directory" — reuse the
  `isInDumpPackage` shape (`:162-165`), which relativizes against the root and checks
  the first path segment.
- **`lineBreaksRule`** → `\bStateMachineDefinition\b|\bTransition\b|…` word-boundary
  patterns over the new type names.

Keep the comment-blanking (`:208-236`) and the anti-vacuity test (`:113-127`) —
without the latter a mis-resolved root makes the guard pass by scanning nothing.

Placement note from `parity-gate.md:60-64`: the existing guard resolves its root from a
fixed relative path and walks it whole, so classes cannot escape a rule by moving. Put
the new guard wherever you like in `src/test/java`, but resolve its root the same way.

Also relevant if the new test drives events:
`parity-gate.md:90-100` — any characterization class that fires an Event must declare
`@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)` because the Flyway strategy is
clean-and-migrate per context build and the seeded corpus is shared. Read-only tests
may use `BEFORE_CLASS`/`AFTER_CLASS`. A pure repository round-trip test under
`@RepositoryTest` inherits `@DirtiesContext(AFTER_CLASS)` from
`@EnabledPostgresTestContainer`
(`EnablePostgresTestContainerContextCustomizerFactory.java:35`) and needs nothing extra.
