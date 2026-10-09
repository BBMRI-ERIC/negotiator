/// <reference types="cypress" />

describe("Test negotiation message", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
        // Go to negotiation.
        cy.get(".primary-table-row").contains("UNDER REVIEW").parent().click()
    })

    context("check if message part in negotiation is visible", () => {
        it("test if all message part are visible", () => {
            // Comment section
            cy.get("[resources=\"[object Object]\"] > :nth-child(1)").should("be.visible")
            // Send message section
            cy.get(".mb-4 > textarea").should("be.visible")
            cy.get("#recipient").should("be.visible")
            cy.get(".btn-attachment").should("be.visible")
            cy.get(".mb-4 > .d-flex > span").should("be.visible")
        })
    })

    context("check if you can send message in negotiation", () => {
        it("test send a message", () => {
            // Comment section
            cy.get("[resources=\"[object Object]\"] > :nth-child(1)").should("be.visible")
            // Send message section
            cy.get(".mb-4 > textarea").type("Hi i want to test message functionality, have a great day.")
            cy.get("#recipient").select("Public channel")
            cy.get('#send').click()
        })

        it("test send a message with Ctrl+Enter", () => {
            cy.get("#recipient").select("Public channel")
            cy.get(".mb-4 > textarea").type("First line{enter}second line")
            cy.get(".mb-4 > textarea").should("have.value", "First line\nsecond line")
            cy.get(".mb-4 > textarea").type("{ctrl}{enter}")
            cy.get(".mb-4 > textarea").should("have.value", "")
        })

        it("test send a message with Cmd+Enter", () => {
            cy.get("#recipient").select("Public channel")
            cy.get(".mb-4 > textarea").type("Sent with Cmd+Enter")
            cy.get(".mb-4 > textarea").type("{cmd}{enter}")
            cy.get(".mb-4 > textarea").should("have.value", "")
        })
    })
    context("check if message is visible in negotiation", () => {
        it("test message visibility", () => {
            // Comment section
            cy.get("[resources=\"[object Object]\"] > :nth-child(1)").should("be.visible")
            // Display sender and reciver of message
            cy.get(".badge").should("be.visible")
        })
    })
})
