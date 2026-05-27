/// <reference types="cypress" />

describe("Test access form renaming functionality (Issue #1170)", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
    })

    context("Access form name update - Bug Fix Verification", () => {
        it("should navigate to access forms admin section", () => {
            // Navigate to access forms in admin settings
            cy.visit("http://localhost:8080/settings/5")
            
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
            cy.visit("http://localhost:8080/settings/5")
            
            // Check that at least one row exists in the table
            cy.get("tbody tr").should("have.length.greaterThan", 0)
        })

        it("should successfully update an access form name and persist it", () => {
            cy.visit("http://localhost:8080/settings/5")
            
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
                cy.url().should("contain", `/settings/editAccessForm/${formId}`)
                
                // Check that the form name input is visible
                cy.contains("label", "Form Name").should("be.visible")
                
                // Find the form name input field and update it
                // The input field should be of type TEXT
                cy.get("input[type='TEXT'][placeholder='Give a form name']")
                    .should("be.visible")
                    .clear()
                    .type(newName)
                
                // Scroll down to ensure the button is visible
                cy.get("h1").scrollIntoView()
                
                // Find the form wizard footer and click the final step button
                // This button triggers the modal with text "Edit Access Form"
                cy.get(".wizard-footer-right button").last().click()
                
                // Wait for the confirmation modal to appear
                cy.get(".modal-title").contains("Confirm Editing").should("be.visible")
                
                // Click the confirm button
                cy.get(".modal-footer button").contains("Confirm").click()
                
                // Should be redirected back to access forms page
                cy.url().should("contain", "/settings/access-forms")
                
                // Verify the name was updated in the table (this tests the bug fix)
                cy.get("tbody tr").should("contain", newName)
                
                // Double-check by visiting the access forms page again to ensure persistence
                cy.visit("http://localhost:8080/settings/5")
                cy.get("tbody tr").should("contain", newName)
            })
        })

        it("should preserve form name when editing without changing the name", () => {
            cy.visit("http://localhost:8080/settings/5")
            
            // Get the first access form
            cy.get("tbody tr").first().then(($row) => {
                const formId = $row.find("td").eq(0).text().trim()
                
                // Click to edit
                cy.wrap($row).click()
                cy.url().should("contain", `/settings/editAccessForm/${formId}`)
                
                // Check that the form name field has content
                cy.get("input[type='TEXT'][placeholder='Give a form name']").should("have.value")
                
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
