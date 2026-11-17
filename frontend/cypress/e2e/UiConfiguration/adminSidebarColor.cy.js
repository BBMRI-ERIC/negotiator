/// <reference types="cypress" />

const apiUrl = "http://localhost:8080/api/v3"

describe("Test admin console sidebar colors", () => {
    let authHeader
    let originalButtonColor

    function updateButtonColor(buttonColor) {
        cy.request({
            method: "PUT",
            url: `${apiUrl}/ui-config`,
            headers: { Authorization: authHeader },
            body: { theme: { buttonColor } },
        })
    }

    before(() => {
        cy.request(`${apiUrl}/ui-config`).then(({ body }) => {
            originalButtonColor = body.theme.buttonColor
        })
    })

    beforeEach(() => {
        cy.intercept("GET", "**/api/v3/userinfo").as("userInfo")
        cy.login("Admin", "admin")
        cy.wait("@userInfo").then(({ request }) => {
            authHeader = request.headers.authorization
        })
    })

    after(() => {
        // The UI configuration is shared, so restore it for other specs and reruns
        updateButtonColor(originalButtonColor)
    })

    it("uses the configured button color for the active sidebar item", () => {
        updateButtonColor("#123456")
        cy.visit("http://localhost:8080/settings/webhooks")
        cy.get("[data-test=admin-settings-nav-item].active")
            .should("have.length", 1)
            .and("contain.text", "Webhooks")
            .and("have.css", "background-color", "rgb(18, 52, 86)")
        cy.get("[data-test=admin-settings-nav-item]:not(.active)")
            .first()
            .should("not.have.css", "background-color", "rgb(18, 52, 86)")
    })
})
