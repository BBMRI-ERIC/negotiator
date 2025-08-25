package eu.bbmri_eric.negotiator.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
public class TemplateServiceTest {

  @Autowired
  private TemplateService templateService;

  @Autowired
  private TemplateRepository templateRepository;

  @BeforeEach
  void setUp() {
    templateRepository.deleteAll();
    var simpleTemplate = Template.builder()
        .name("TEST_SIMPLE")
        .content("""
            <!DOCTYPE html>
            <html xmlns:th="http://www.thymeleaf.org">
            <head>
                <title>Simple Test Template</title>
            </head>
            <body>
                <h1>Hello, <span th:text="${userName}">Default User</span>!</h1>
                <p>Welcome to our application.</p>
            </body>
            </html>""")
        .build();

    var complexTemplate = Template.builder()
        .name("TEST_COMPLEX")
        .content("""
            <!DOCTYPE html>
            <html xmlns:th="http://www.thymeleaf.org">
            <head>
                <title>Complex Test Template</title>
            </head>
            <body>
                <h1>User Profile</h1>
                <div>
                    <p>Name: <span th:text="${user.name}">John Doe</span></p>
                    <p>Email: <span th:text="${user.email}">john@example.com</span></p>
                    <p>Age: <span th:text="${user.age}">25</span></p>
                    
                    <div th:if="${user.isActive}">
                        <p>Status: Active User</p>
                    </div>
                    <div th:unless="${user.isActive}">
                        <p>Status: Inactive User</p>
                    </div>
                    
                    <div th:if="${items != null and !items.isEmpty()}">
                        <h3>Items:</h3>
                        <ul>
                            <li th:each="item : ${items}" th:text="${item}">Item</li>
                        </ul>
                    </div>
                    
                    <p>Generated on: <span th:text="${#dates.format(currentDate, 'yyyy-MM-dd HH:mm:ss')}">2024-01-01 12:00:00</span></p>
                </div>
            </body>
            </html>""")
        .build();

    templateRepository.save(simpleTemplate);
    templateRepository.save(complexTemplate);
  }

  @Test
  @DisplayName("Should process template with single variable successfully")
  void shouldProcessTemplateWithSingleVariable() {
    Map<String, Object> variables = Map.of("userName", "Alice");

    var result = templateService.processTemplate(variables, "TEST_SIMPLE");

    assertThat(result).isNotNull();
    assertThat(result).contains("<span>Alice</span>");
    assertThat(result).contains("Welcome to our application.");
    assertThat(result).contains("<title>Simple Test Template</title>");
  }

  @Test
  @DisplayName("Should process template with complex object variables")
  void shouldProcessTemplateWithComplexVariables() {
    Map<String, Object> user = Map.of(
        "name", "John Smith",
        "email", "john.smith@example.com",
        "age", 30,
        "isActive", true
    );

    var items = List.of("Item 1", "Item 2", "Item 3");
    var currentDate = new Date();

    Map<String, Object> variables = Map.of(
        "user", user,
        "items", items,
        "currentDate", currentDate
    );

    var result = templateService.processTemplate(variables, "TEST_COMPLEX");

    assertThat(result).isNotNull();
    assertThat(result).contains("<span>John Smith</span>");
    assertThat(result).contains("<span>john.smith@example.com</span>");
    assertThat(result).contains("<span>30</span>");
    assertThat(result).contains("Status: Active User");
    assertThat(result).doesNotContain("Status: Inactive User");
    assertThat(result).contains("Items:");
    assertThat(result).contains("Item 1");
    assertThat(result).contains("Item 2");
    assertThat(result).contains("Item 3");
    assertThat(result).contains("Generated on:");
  }

  @Test
  @DisplayName("Should process template with inactive user")
  void shouldProcessTemplateWithInactiveUser() {
    Map<String, Object> user = Map.of(
        "name", "Jane Doe",
        "email", "jane@example.com",
        "age", 25,
        "isActive", false
    );

    Map<String, Object> variables = Map.of("user", user);

    var result = templateService.processTemplate(variables, "TEST_COMPLEX");

    assertThat(result).contains("Status: Inactive User");
    assertThat(result).doesNotContain("Status: Active User");
  }

  @Test
  @DisplayName("Should process template with empty variables")
  void shouldProcessTemplateWithEmptyVariables() {
    var variables = Map.<String, Object>of();

    var result = templateService.processTemplate(variables, "TEST_SIMPLE");

    assertThat(result).isNotNull();
    assertThat(result).contains("<span></span>");
    assertThat(result).contains("Welcome to our application.");
  }

  @Test
  @DisplayName("Should process template with null variables")
  void shouldProcessTemplateWithNullVariables() {
    var result = templateService.processTemplate(null, "TEST_SIMPLE");

    assertThat(result).isNotNull();
    assertThat(result).contains("<span></span>");
    assertThat(result).contains("Welcome to our application.");
  }

  @Test
  @DisplayName("Should process template with empty items list")
  void shouldProcessTemplateWithEmptyItems() {
    Map<String, Object> user = Map.of(
        "name", "Test User",
        "email", "test@example.com",
        "age", 20,
        "isActive", true
    );

    Map<String, Object> variables = Map.of(
        "user", user,
        "items", List.<String>of(),
        "currentDate", new Date()
    );

    var result = templateService.processTemplate(variables, "TEST_COMPLEX");

    assertThat(result).doesNotContain("Items:");
    assertThat(result).contains("<span>Test User</span>");
  }

  @Test
  @DisplayName("Should throw exception when template name is null")
  void shouldThrowExceptionWhenTemplateNameIsNull() {
    Map<String, Object> variables = Map.of("userName", "Alice");

    assertThatThrownBy(() -> templateService.processTemplate(variables, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template name cannot be null or blank");
  }

  @Test
  @DisplayName("Should throw exception when template name is blank")
  void shouldThrowExceptionWhenTemplateNameIsBlank() {
    Map<String, Object> variables = Map.of("userName", "Alice");

    assertThatThrownBy(() -> templateService.processTemplate(variables, ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template name cannot be null or blank");

    assertThatThrownBy(() -> templateService.processTemplate(variables, "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template name cannot be null or blank");
  }

  @Test
  @DisplayName("Should throw exception when template does not exist")
  void shouldThrowExceptionWhenTemplateDoesNotExist() {
    Map<String, Object> variables = Map.of("userName", "Alice");

    assertThatThrownBy(() -> templateService.processTemplate(variables, "NON_EXISTENT"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template 'NON_EXISTENT' not found");
  }

  @Test
  @DisplayName("Should handle variables with different data types")
  void shouldHandleVariablesWithDifferentDataTypes() {
    var variables = new HashMap<String, Object>();
    variables.put("userName", "Test User");
    variables.put("count", 42);
    variables.put("isEnabled", true);
    variables.put("price", 19.99);
    variables.put("timestamp", LocalDateTime.now());

    var result = templateService.processTemplate(variables, "TEST_SIMPLE");

    assertThat(result).isNotNull();
    assertThat(result).contains("<span>Test User</span>");
  }

  @Test
  @DisplayName("Should process template with nested object properties")
  void shouldProcessTemplateWithNestedObjectProperties() {
    Map<String, Object> address = Map.of(
        "street", "123 Main St",
        "city", "Springfield",
        "zipCode", "12345"
    );

    Map<String, Object> user = Map.of(
        "name", "John Doe",
        "email", "john@example.com",
        "age", 28,
        "isActive", true,
        "address", address
    );

    Map<String, Object> variables = Map.of("user", user);

    var result = templateService.processTemplate(variables, "TEST_COMPLEX");

    assertThat(result).contains("<span>John Doe</span>");
    assertThat(result).contains("<span>john@example.com</span>");
    assertThat(result).contains("Status: Active User");
  }
}
