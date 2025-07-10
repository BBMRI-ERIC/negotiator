/// <reference types="cypress" />

describe("Test access forms settings", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
        cy.visit("http://localhost:8080/settings")
    })

    context("check if access form settings section is visible", () => {
        it("test if tittle is visible", () => {
            cy.get('.access-forms-section > .mb-1').should("be.visible")
        })
        it("test if tittle is subtittle visible", () => {
            cy.get('.access-forms-section > .mb-3').should("be.visible")
        })
    })

    context("check if you can duplicate form", () => {
        it("click duplicate access form button", () => {
            // Open modal
            cy.get(':nth-child(2) > tr > .col-3 > .btn').should("be.visible")

            cy.get(':nth-child(2) > tr > .col-3 > .btn').click()
        })
        it("duplicate form is displayed", () => {
            cy.get(':nth-child(2) > tr > .col-3 > .btn').click()

            cy.get('.new-section-button > label').should("be.visible")
        })
    })
})
