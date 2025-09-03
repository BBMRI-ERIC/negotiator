# Contribution guidelines

We are really glad you are seeing this and are considering contributing to the development of the
Negotiator.
We welcome any pull requests that help us grow as open-source software!

## Development Environment

Our core developers use the [IntelliJ IDEA](https://www.jetbrains.com/idea/) and UNIX based Operating systems.
and can therefore be more helpful in debugging any potential problems in your local development
environment.

### Running the backend in development mode

> [!Warning] Prerequisites
> - Java 21
> - Maven 3.9 and newer
> - Docker Engine 27.0 and newer (Application will spin up a PostgreSQL databse using
    the [testcontainers](https://testcontainers.com) framework)
> - Unallocated Ports 8081 and 5432

To run the backend in development mode, run the following command.
Spring applications support hot reload with some limitations,
for a detailed explanation refer to
their [documentation](https://docs.spring.io/spring-boot/reference/using/devtools.html).

```shell
mvn clean spring-boot:test-run -Dspring-boot.run.profiles=dev 
```

> [!TIP]
> The Database is available at the URL visible bellow with default credentials negotiator:negotiator:
> `jdbc:postgresql://localhost:5432/negotiator`

### Known problems

#### WSL

The current development environment setup is problematic with the WSL.
Make sure your networking configuration is properly adjusted, especially when using an OIDC provider or a UI client.

For WSL debugging you can use the following command to run the backend in debug mode:

```shell
# This script is used to perform remote debugging of a Spring Boot application in debug mode in a WSL or Linux environment
mvn clean spring-boot:test-run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

Additionally, enable restart in your IDE so that it can pick up the changes you make in application-dev.yaml:

```yaml
spring:
  devtools:
    restart:
      enabled: true
```

## Code style

For maintaining a uniform code style through the code base, please adhere to the
[Google Java Style](https://google.github.io/styleguide/javaguide.html).
To enforce this, we have set up a GitHub action that checks the code style of any modified files
using the [fmt-maven-plugin](https://github.com/spotify/fmt-maven-plugin).
You can also set up an IntelliJ IDEA plugin
[google-java-format](https://github.com/google/google-java-format).

## Commit messages

Please follow
the [Conventional Commits specification](https://www.conventionalcommits.org/en/v1.0.0/#summary).

## Versioning

For versioning of releases and tags please follow [Semantic Versioning](https://semver.org/).

## 🧑‍💻 Submitting changes

We use a simple Git flow based on short-lived feature branches and a stable `main` branch.  
All work should be done in a `feature/<your-branch>` branch, created from `main`, and submitted as a pull request (
PR).  
Please ensure your PR includes a clear title using [Conventional Commits](https://www.conventionalcommits.org/) (e.g.
`feat: add PDF export`).  
PRs should be rebased or squashed before merging to keep the commit history clean.  
All contributions are reviewed before merging into `main`, which is always deployable.

Please send us a GitHub Pull Request with a clear description.
We have also provided
a [template](https://github.com/BBMRI-ERIC/negotiator-v3/blob/master/.github/pull_request_template.md)
with a checklist to help you with providing a high-quality contribution.

## Releasing

Currently, the project does not require a regular release schedule.
Changelog is kept in the form of GitHub releases and their description.
To create a release, simply go to Releases → Draft a new release
→ Choose new tag → Generate release notes → Edit to follow structure above → Publish release

### Release Categories

- **Added:** for new features/additions
- **Fixed:** for bug fixes
- **Changed:** for changes in existing functionality
- **Deprecated:** for soon-to-be removed features
- **Removed:** for now removed features
- **Internal:** for chores and other changes that do not impact the functionality

### Breaking Changes

For breaking changes that require a major version bump (following [Semantic Versioning](https://semver.org/)):

- Use the **BREAKING:** prefix in the category description
- Clearly explain what changed and how to migrate
- Include migration steps or links to migration guides when applicable

Example:

- **BREAKING - Removed:** Legacy API endpoints */api/v1/* - migrate to */api/v2/*
- **BREAKING - Changed:** Configuration format updated - see migration guide in docs

---
Below is an example of a release description, please follow this structure!

### What's New in v2.0.0

- **Added:** export to CSV
- **BREAKING - Changed:** Authentication now requires API keys instead of username/password -
  see [authentication guide](/auth)
- **Fixed:** issue with date parsing
- **BREAKING - Removed:** Legacy endpoints */api/v1/users* and */api/v1/projects* - use */api/v2/users* and
  */api/v2/projects* instead
- **Internal:** refactor of logging module

Thanks to Alice and Bob for contributions!

⚠️ **This is a major release with breaking changes. Please review the migration steps above before upgrading.**

---

                        Good luck and thank you! 🙇🏻‍♂️
