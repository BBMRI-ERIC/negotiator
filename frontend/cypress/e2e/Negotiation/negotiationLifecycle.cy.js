/// <reference types="cypress" />

const apiUrl = "http://localhost:8080/api/v3"
// "Test collection 2" from the dev seed data
const resource = {
    id: "bbmri-eric:ID:CZ_MMCI:collection:LTS",
    name: "Test collection 2",
    description: "This is the second test collection",
}
const resourceId = 2
const payload = {
    project: {
        title: "Test e2e negotiation lifecycle",
        "disease-code": "C34.9",
        objective: "Test negotiation lifecycle actions",
        organization: "Masaryk Memorial Cancer Institute",
        profit: "No",
        acknowledgment: "TEST",
        "Multichoice test": ["second_choice"],
        "Single test": "second_choice",
        "Bool test": "Yes",
    },
    request: {
        description: "TEST",
        collection: "",
        donors: "100",
        samples: "tissue",
        specifics: "TEST",
    },
    "ethics-vote": { "ethics-vote": "Approved", "ethics-vote-attachment": null },
}

describe("Test negotiation lifecycle actions", () => {
    let authHeader
    let userId
    let negotiationId

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
                url: `${apiUrl}/users/${userId}/resources`,
                headers: { Authorization: authHeader },
                body: { id: resourceId },
            })
            // Every attempt gets its own submitted negotiation, as the test moves it to a final state
            cy.request("POST", `${apiUrl}/requests`, {
                url: "https://bbmritestnn.gcc.rug.nl",
                humanReadable: "Test e2e negotiation lifecycle",
                resources: [resource],
            }).then(({ body: req }) => {
                cy.request({
                    method: "POST",
                    url: `${apiUrl}/negotiations`,
                    headers: { Authorization: authHeader },
                    body: { request: req.id, payload },
                }).then(({ body: negotiation }) => {
                    negotiationId = negotiation.id
                })
            })
        })
    })

    afterEach(() => {
        if (userId === undefined) return
        cy.request({
            method: "DELETE",
            url: `${apiUrl}/users/${userId}/resources/${resourceId}`,
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

            // Approving contacts the representative. Stepping away brings the only resource to a final
            // state, which auto-concludes the negotiation
            cy.contains(".collapse-organization", "Masaryk Memorial Cancer Institute").click()
            cy.contains(".lifecycle-text", "Step Away").click()

            cy.contains(".bg-status-badge", "RESOURCE UNAVAILABLE").should("exist")
            cy.contains("CONCLUDED").should("be.visible")
            cy.contains("button", "Conclude").should("not.exist")
            cy.contains("button", "Pause").should("not.exist")
            cy.contains("button", "Abandon").should("not.exist")
        })
    })
})
