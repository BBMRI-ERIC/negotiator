/// <reference types="cypress" />

describe("Test negotiation pdf download", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
        // Go to negotiation.
        cy.get("tbody > tr > td ").contains("UNDER REVIEW").parent().parent().find(":nth-child(6)").click()
    })

    context("check if you can download negotiation pdf", () => {
        it("test if download pdf button is working", () => {

            cy.get(".pdf-button").should("be.visible")

            cy.get(".pdf-button").click()

            cy.get('.col-12 > .alert').should("be.visible").contains("File successfully saved")
        })
    })
})
