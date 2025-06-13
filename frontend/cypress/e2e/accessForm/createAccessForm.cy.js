/// <reference types="cypress" />

describe("Test access negotiations", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")

        cy.wait(200)
        // Go to admin page.
        cy.get(':nth-child(1) > .nav-link').click()
        
    })

   
    context("check if add access form buttonis visible", () => {
        it("test if button is visible visible", () => {

            cy.get('div.float-end > :nth-child(1) > .btn-sm').should("be.visible")
        })
    })

    context("check if modal is displayed", () => {
        it("test if modal is visible", () => {
            // Open modal
            cy.get('div.float-end > :nth-child(1) > .btn-sm').click()

            cy.get('#newRedirectModal > .modal-dialog > .modal-content').should("be.visible")
        })
        it("test if modal ok button is working", () => {
            // Open modal
            cy.get('div.float-end > :nth-child(1) > .btn-sm').click()

            cy.get('#newRedirectModal > .modal-dialog > .modal-content').should("be.visible")
            cy.get('#newRedirectModal > .modal-dialog > .modal-content > .modal-footer > .btn').contains("OK").should("be.visible")

        })
    })

    context("check if customize Form page is displayed", () => {
        it("test if eerything is shown", () => {
            // Open modal
            cy.get('div.float-end > :nth-child(1) > .btn-sm').click()
            cy.get('#newRedirectModal > .modal-dialog > .modal-content > .modal-footer > .btn').click()
            
            cy.get('.new-section-button > label').should("be.visible")
            cy.get('.new-section-button > .btn').should("be.visible")
            cy.get('.form-label').should("be.visible")
            cy.get('.template-name > .form-control').should("be.visible")
        })
    })
})
