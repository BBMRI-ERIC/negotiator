/// <reference types="cypress" />

describe("Test merged pdf download", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
        // Go to negotiation.
        cy.get("tbody > tr > td ").contains("UNDER REVIEW").parent().parent().find(":nth-child(6)").click()
    })

    context("check if you can download merged negotiation pdf", () => {
        it("test if download merged pdf button is working", () => {

            cy.get(".merged-pdf-button").should("be.visible")

            cy.get(".merged-pdf-button").click()

            cy.get('.col-12 > .alert').should("be.visible").contains("Merged PDF successfully saved")
        })
    })
})
