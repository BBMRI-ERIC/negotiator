/// <reference types="cypress" />

// SUBMITTED negotiation from the dev seed data whose only resource is "Test collection 2" (id 2)
const negotiationId = "550e8400-e29b-41d4-a716-446655440003"
const resourceId = 2

describe("Test negotiation lifecycle actions", () => {
    let authHeader
    let userId

    beforeEach(() => {
        cy.intercept("GET", "**/api/v3/userinfo").as("userInfo")
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
        cy.wait("@userInfo").then(({ request, response }) => {
            authHeader = request.headers.authorization
            userId = response.body.id
            // Make the admin a representative, so the resource state can be updated from the page
            cy.request({
                method: "PATCH",
                url: `http://localhost:8080/api/v3/users/${userId}/resources`,
                headers: { Authorization: authHeader },
                body: { id: resourceId },
            })
        })
    })

    afterEach(() => {
        if (userId === undefined) return
        cy.request({
            method: "DELETE",
            url: `http://localhost:8080/api/v3/users/${userId}/resources/${resourceId}`,
            headers: { Authorization: authHeader },
        })
    })

    context("negotiation is auto-concluded by a resource state change", () => {
        it("test if the negotiation lifecycle actions are updated without a page refresh", () => {
            cy.visit(`http://localhost:8080/negotiations/${negotiationId}/ROLE_ADMIN`)

            // Approve the negotiation
            cy.contains("button", "Approve").click()
            // Bootstrap ignores the dismiss while the modal is still fading in
            cy.get("#abandonModal .btn-danger").should("be.visible")
            cy.wait(500)
            cy.get("#abandonModal .btn-danger").click()
            cy.get("#abandonModal").should("not.be.visible")
            cy.contains("button", "Conclude").should("be.visible")

            // Bring the only resource to a final state, which auto-concludes the negotiation
            cy.contains(".collapse-organization", "Masaryk Memorial Cancer Institute").click()
            cy.contains(".lifecycle-text", "Contact").click()
            cy.contains(".lifecycle-text", "Step Away").click()

            cy.contains(".bg-status-badge", "RESOURCE UNAVAILABLE").should("exist")
            cy.contains("CONCLUDED").should("be.visible")
            cy.contains("button", "Conclude").should("not.exist")
            cy.contains("button", "Pause").should("not.exist")
            cy.contains("button", "Abandon").should("not.exist")
        })
    })
})
