/// <reference types="cypress" />

describe("Test negotiation pdf download", () => {
  beforeEach(() => {
    cy.visit("http://localhost:8080")
    cy.login("Admin", "admin")
    // Go to negotiation.
    cy.get("tbody > tr > td ").contains("UNDER REVIEW").parent().parent().find(":nth-child(6)").click()
  })

  // Download of merged pdf
  context("check if negotiation pdf download is working", () => {
    it("test if download pdf button is working", () => {

      cy.get("#pdf-button").should("be.visible")
      cy.get("#pdf-button > .pdf-button").click()
      cy.get(".modal-content", { timeout: 10000 }).should("be.visible")
      cy.get(".modal-dialog > .modal-content").should("be.visible").contains("Downloading")
      cy.get('.col-12 > .alert', { timeout: 10000 }).should("be.visible").contains("File successfully saved")
    })
  })

  // Download of negotiation pdf
  context("check if merged negotiation pdf download is working", () => {
    it("test if download merged pdf button is working", () => {
      cy.get("#merged-pdf-button").should("be.visible")
      cy.get("#merged-pdf-button > .pdf-button").click()
      cy.get(".modal-content", { timeout: 10000 }).should("be.visible")
      cy.get(".modal-dialog > .modal-content").should("be.visible").contains("Downloading")
      cy.get('.col-12 > .alert', { timeout: 10000 }).should("be.visible").contains("File successfully saved")
    })
  })
})
