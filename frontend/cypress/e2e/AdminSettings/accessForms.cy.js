/// <reference types="cypress" />

describe("Test access form renaming functionality (Issue #1170)", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
        // Wait for login to fully complete - user should be redirected to /researcher
        cy.url().should("eq", "http://localhost:8080/researcher")
        // Navigate to settings via the navbar to preserve OIDC session
        // (cy.visit() causes a full page reload which loses the in-memory auth state)
        cy.get("a[href='/settings']").first().click()
        cy.url().should("contain", "/settings")
        // Click the Access Forms nav link
        cy.contains("a.nav-link", "Access Forms").click()
        cy.url().should("contain", "/settings/access-forms")
    })

    context("Access form name update - Bug Fix Verification", () => {
        it("should navigate to access forms admin section", () => {
            // Check that the access forms section heading is visible
            cy.contains("h2", "Access Forms").should("be.visible")
            
            // Check that the add button is visible
            cy.contains("button", "Add Access Form").should("be.visible")
            
            // Check that the table exists
            cy.get("table.table").should("be.visible")
            
            // Check that table headers are present
            cy.contains("thead th", "ID").should("be.visible")
            cy.contains("thead th", "Name").should("be.visible")
        })

        it("should display access forms in a table", () => {
            // Check that at least one row exists in the table
            cy.get("tbody tr").should("have.length.greaterThan", 0)
        })

        it("should successfully update an access form name and persist it", () => {
            // Wait for the table to load
            cy.get("tbody tr").should("have.length.greaterThan", 0)

            // Get the first access form in the table
            cy.get("tbody tr").first().then(($row) => {
                // Extract the form ID and original name
                const cells = $row.find("td")
                const formId = cells.eq(0).text().trim()
                const originalName = cells.eq(1).text().trim()
                const newName = `${originalName}-CypressTest-${Date.now()}`
                
                // Click on the form to edit it
                cy.wrap($row).click()
                
                // Wait for navigation to the edit page
                cy.url().should("contain", `/settings/access-forms/edit/${formId}`)
                
                // Check that the form name input is visible
                cy.contains("label", "Form Name").should("be.visible")
                
                // Find the form name input field and update it
                cy.get("input[type='TEXT'][placeholder='Give a form name']")
                    .should("be.visible")
                    .clear()
                    .type(newName)
                
                // Scroll down to ensure the button is visible
                cy.get("h1").scrollIntoView()
                
                // Find the form wizard footer and click the final step button
                cy.get(".wizard-footer-right button").last().click()
                
                // Wait for the confirmation modal to appear
                cy.get(".modal-title").contains("Confirm Editing").should("be.visible")
                
                // Click the confirm button
                cy.get(".modal-footer button").contains("Confirm").click()
                
                // Should be redirected back to access forms page
                cy.url().should("contain", "/settings/access-forms")
                
                // Verify the name was updated in the table (this tests the bug fix)
                cy.get("tbody tr").should("contain", newName)
                
                // Navigate away and back to verify persistence
                cy.contains("a.nav-link", "Users").click()
                cy.url().should("contain", "/settings/users")
                cy.contains("a.nav-link", "Access Forms").click()
                cy.url().should("contain", "/settings/access-forms")
                cy.get("tbody tr").should("contain", newName)
            })
        })

        it("should preserve form name when editing without changing the name", () => {
            // Wait for the table to load
            cy.get("tbody tr").should("have.length.greaterThan", 0)

            // Get the first access form
            cy.get("tbody tr").first().then(($row) => {
                const formId = $row.find("td").eq(0).text().trim()
                
                // Click to edit
                cy.wrap($row).click()
                cy.url().should("contain", `/settings/access-forms/edit/${formId}`)
                
                // Wait for the form to load and check that the form name field has content
                cy.get("input[type='TEXT'][placeholder='Give a form name']")
                    .should("be.visible")
                    .should("not.have.value", "")
                
                // Get current name value
                cy.get("input[type='TEXT'][placeholder='Give a form name']").invoke("val").then((currentName) => {
                    // Submit without changing the name
                    cy.get(".wizard-footer-right button").last().click()
                    
                    // Confirm the modal
                    cy.get(".modal-title").contains("Confirm Editing").should("be.visible")
                    cy.get(".modal-footer button").contains("Confirm").click()
                    
                    // Verify the name is still the same
                    cy.url().should("contain", "/settings/access-forms")
                    cy.get("tbody tr").should("contain", currentName)
                })
            })
        })
    })
})
