---
name: Migrate from CommonsLog to Slf4j
about: Technical debt - standardize logging framework across the codebase
title: "[TECH-DEBT] Migrate from Apache Commons Logging to SLF4J"
labels: enhancement, tech-debt, refactoring
assignees: ''

---

## Background

The Negotiator codebase currently uses Apache Commons Logging (via Lombok's `@CommonsLog` annotation) for logging in 62 classes. However, the project already has SLF4J available through Spring Boot's logging infrastructure, and at least one class (`HttpRequestLoggingFilter`) already uses SLF4J directly.

## Problem Statement

Using multiple logging frameworks in the same codebase leads to:

1. **Inconsistency**: Different classes use different logging approaches (`@CommonsLog` vs direct SLF4J usage)
2. **Maintainability issues**: Developers need to understand two different logging APIs
3. **Configuration complexity**: Multiple logging frameworks require careful configuration management
4. **Performance overhead**: Commons Logging uses runtime discovery which adds slight overhead compared to SLF4J's compile-time binding

## Proposed Solution

Migrate all classes from Apache Commons Logging to SLF4J using Lombok's `@Slf4j` annotation.

### Why SLF4J?

1. **Industry Standard**: SLF4J is the de facto standard logging facade in the Java ecosystem
2. **Better Performance**: Uses compile-time binding instead of runtime discovery
3. **Already Available**: Spring Boot includes SLF4J by default with Logback as the implementation
4. **Feature Rich**: Supports modern logging features like structured logging and MDC (Mapped Diagnostic Context)
5. **Better IDE Support**: More comprehensive tooling and IDE integration
6. **Consistency with Spring Boot Best Practices**: Spring Boot's documentation and community favor SLF4J
7. **Parameterized Logging**: SLF4J's parameterized logging syntax is more efficient and readable

### Benefits of Migration

1. **Standardization**: Single logging API across the entire codebase
2. **Alignment with Documentation**: The `.github/instructions/spring.instructions.md` already states "Use SLF4J for all logging"
3. **Improved Developer Experience**: Developers only need to learn one logging API
4. **Better Tooling**: Enhanced debugging and monitoring capabilities with modern SLF4J features
5. **Future-Proof**: Better positioned for future logging enhancements and integrations

## Scope

The migration affects **62 classes** that currently use `@CommonsLog`:

```
backend/src/main/java/eu/bbmri_eric/negotiator/common/EventListener.java
backend/src/main/java/eu/bbmri_eric/negotiator/common/LinkBuilder.java
backend/src/main/java/eu/bbmri_eric/negotiator/common/exceptions/NegotiatorExceptionHandler.java
backend/src/main/java/eu/bbmri_eric/negotiator/common/configuration/ExceptionHandlerFilter.java
backend/src/main/java/eu/bbmri_eric/negotiator/common/configuration/security/oauth2/CustomJWTAuthConverter.java
backend/src/main/java/eu/bbmri_eric/negotiator/common/configuration/security/oauth2/IntrospectionValidator.java
backend/src/main/java/eu/bbmri_eric/negotiator/common/configuration/AsynchronousSpringEventsConfig.java
backend/src/main/java/eu/bbmri_eric/negotiator/common/AuthenticatedUserContext.java
backend/src/main/java/eu/bbmri_eric/negotiator/email/NotificationEmailServiceImpl.java
backend/src/main/java/eu/bbmri_eric/negotiator/email/NotificationEmailController.java
... and 52 more classes
```

## Implementation Tasks

1. Replace `import lombok.extern.apachecommons.CommonsLog;` with `import lombok.extern.slf4j.Slf4j;`
2. Replace `@CommonsLog` annotation with `@Slf4j` annotation
3. Verify that all logging calls remain compatible (both use `log` as the logger instance name)
4. Update documentation in `.github/instructions/spring.instructions.md` to fix the typo
5. Run all tests to ensure no regression
6. Verify logging output in runtime environment

## Technical Considerations

### Low Risk Migration

This is a **low-risk refactoring** because:
- Both Commons Logging and SLF4J use the same `log` variable name
- The logging method signatures are compatible (`log.debug()`, `log.info()`, `log.error()`, etc.)
- SLF4J is already on the classpath via Spring Boot
- No changes to logging configuration files are needed
- No runtime behavior changes expected

### Testing Strategy

- Run existing unit tests to verify no breaking changes
- Run integration tests to ensure logging still works correctly
- Manual verification of log output in development environment
- Verify that Logback configuration continues to work as expected

## Acceptance Criteria

- [ ] All `@CommonsLog` annotations replaced with `@Slf4j`
- [ ] All imports updated from `lombok.extern.apachecommons.CommonsLog` to `lombok.extern.slf4j.Slf4j`
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Logging output verified in runtime environment
- [ ] Documentation updated to reflect SLF4J usage
- [ ] Code review completed
- [ ] No regression in application behavior

## References

- [SLF4J Documentation](https://www.slf4j.org/manual.html)
- [Lombok Logging Annotations](https://projectlombok.org/features/log)
- [Spring Boot Logging Documentation](https://docs.spring.io/spring-boot/reference/features/logging.html)
- Current project instructions: `.github/instructions/spring.instructions.md`

## Related Files

- `.github/instructions/spring.instructions.md` - Contains logging guidelines (currently has typo)
- `backend/pom.xml` - Includes SLF4J via Spring Boot dependencies
- `backend/src/main/resources/logback-spring.xml` - Logback configuration (if exists)
