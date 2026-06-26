/// <reference types="cypress" />

describe("Test access form renaming functionality (Issue #1170)", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
        // Wait for login to fully complete - user should be redirected to /researcher
        cy.url().should("eq", "http://localhost:8080/researcher")
        // Use the Vue Router programmatically to navigate without a full page reload
        // This preserves the OIDC session state stored in the Pinia store.
        // Vue 3 attaches the app instance to the mount container element (#app), not to window.
        cy.window().then((win) => {
            win.document
                .querySelector("#app")
                .__vue_app__.config.globalProperties.$router.push("/settings/access-forms")
        })
        cy.url().should("contain", "/settings/access-forms")
        // Wait for the Access Forms section to load
        cy.contains("h2", "Access Forms", { timeout: 10000 }).should("be.visible")
    })

    // The edit form is a multi-step wizard. The footer-right button reads "Next" on every step
    // except the last, where it completes the wizard and opens the confirmation modal.
    // Click through every step regardless of how many sections the form has.
    function completeWizard() {
        cy.get(".wizard-footer-right button").then(($btn) => {
            const isLastStep = $btn.text().trim() !== "Next"
            cy.wrap($btn).click()
            if (!isLastStep) completeWizard()
        })
    }

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
            cy.get("tbody tr").its("length").should("be.gt", 0)     
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
                
                // Commented out because of cache error in git CI
                // Wait for navigation to the edit page (flat routing)
                // cy.url().should("contain", `/settings/editAccessForm/${formId}`, { timeout: 1000 })

                // Check that the form name input is visible
                cy.contains("label", "Form Name").should("be.visible")

                // Find the form name input field and update it
                cy.get("input[type='TEXT'][placeholder='Give a form name']").should("be.visible").clear()
                cy.get("input[type='TEXT'][placeholder='Give a form name']").type(newName)

                // Step through the wizard to the final step (auto-scrolls before each click)
                completeWizard()

                // Wait for the (edit) confirmation modal to appear
                cy.get("#feedbackEditModal", { timeout: 10000 }).should("be.visible")
                cy.get("#feedbackEditModal .modal-title").should("contain", "Confirm Editing")

                // Click the confirm button
                cy.get("#feedbackEditModal .modal-footer").contains("button", "Confirm").click()

                // Should be redirected back to access forms page
                cy.url().should("contain", "/settings/access-forms")

                // Verify the name was updated in the table (this tests the bug fix)
                cy.get("tbody tr").should("contain", newName)

                // Navigate away and back to verify persistence (using in-app navigation)
                cy.reload()
                cy.contains(".nav-link", "User Management").click()
                cy.url().should("contain", "/settings/user-management")
                cy.contains("button.nav-link", "Access Forms").click()
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
                
                // Click to edit (flat routing)
                cy.wrap($row).click()
                // Commented out because of cache error in git CI
                // cy.url().should("contain", `/settings/editAccessForm/${formId}`, { timeout: 1000 })

                // Wait for the form to load and check that the form name field has content
                cy.get("input[type='TEXT'][placeholder='Give a form name']")
                    .should("be.visible")
                    .should("not.have.value", "")

                // Get current name value
                cy.get("input[type='TEXT'][placeholder='Give a form name']").invoke("val").then((currentName) => {
                    // Submit without changing the name (step through the whole wizard)
                    completeWizard()

                    // Confirm the modal
                    cy.get("#feedbackEditModal", { timeout: 10000 }).should("be.visible")
                    cy.get("#feedbackEditModal .modal-title").should("contain", "Confirm Editing")
                    cy.get("#feedbackEditModal .modal-footer").contains("button", "Confirm").click()

                    // Verify the name is still the same
                    cy.url().should("contain", "/settings/access-forms")
                    cy.get("tbody tr").should("contain", currentName)
                })
            })
        })
    })
})
