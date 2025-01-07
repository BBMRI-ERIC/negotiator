/// <reference types="cypress" />

// Welcome to Cypress!
//
// This spec file contains a variety of sample tests
// for a todo list app that are designed to demonstrate
// the power of writing tests in Cypress.
//
// To learn more about how Cypress works and
// what makes it such an awesome testing tool,
// please read our getting started guide:
// https://on.cypress.io/introduction-to-cypress

describe("Test login", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
    })

    context("Check visibility of elements", () => {
        it("check visibility of logo", () => {
            cy.get(".img-fluid").should("be.visible")
        })

        it("check visibility of title", () => {
            cy.get(".card-title").should("be.visible")
            cy.get(".card-title").should("have.text", "NEGOTIATOR")
        })

        it("check visibility of text", () => {
            cy.get(".card-subtitle").should("be.visible")
            cy.get(".card-subtitle").should("have.text", " Choose how to log in ")
        })

        it("check visibility of \"Life Science Login\" button", () => {
            cy.get(".btn > .align-self-center").should("be.visible")
            cy.get(".btn > .align-self-center").should("have.text", " Life Science Login")
        })

        it("check visibility of footer text", () => {
            cy.get(".col-sm-10 > :nth-child(2)").should("be.visible")
            cy.get(".col-sm-10 > :nth-child(3)").should("be.visible")
            cy.get(".col-sm-10 > :nth-child(4)").should("be.visible")
            cy.get(".col-sm-10 > :nth-child(5)").should("be.visible")
            cy.get(".col-sm-10 > :nth-child(6)").should("be.visible")
        })

        it("check visibility of version text", () => {
            cy.get(".version-class").should("be.visible")
        })

        it("check visibility of trademark text", () => {
            cy.get("p").should("be.visible")
        })
    })

    context("Test Login funcionality \"Admin\"", () => {
        it("Login as admin", () => {
            cy.get(".btn").click()

            cy.get("#Input_Username").type("Admin")
            cy.get("#Input_Password").type("admin")

            cy.get(".btn-primary").click()

            cy.url().should("eq", "http://localhost:8080/researcher")
        })
    })

    context("Test Login funcionality \"Biobanker\"", () => {
        it("Login as Biobanker", () => {
            cy.get(".btn").click()

            cy.get("#Input_Username").type("Biobanker")
            cy.get("#Input_Password").type("biobanker")

            cy.get(".btn-primary").click()

            cy.url().should("eq", "http://localhost:8080/researcher")
        })
    })

    context("Test Login funcionality \"Repr\"", () => {
        it("Login as Repr", () => {
            cy.get(".btn").click()

            cy.get("#Input_Username").type("Repr")
            cy.get("#Input_Password").type("repr")

            cy.get(".btn-primary").click()

            cy.url().should("eq", "http://localhost:8080/researcher")
        })
    })

    context("Test Login funcionality \"Researcher\"", () => {
        it("Login as Researcher", () => {
            cy.get(".btn").click()

            cy.get("#Input_Username").type("Researcher")
            cy.get("#Input_Password").type("researcher")

            cy.get(".btn-primary").click()

            cy.url().should("eq", "http://localhost:8080/researcher")
        })
    })
})
