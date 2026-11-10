/// <reference types="cypress" />

const apiUrl = "http://localhost:8080/api/v3"
const currentYear = new Date().getFullYear()

describe("Test configurable copyright text", () => {
    let authHeader
    let originalFooterConfig

    function updateFooterConfig(footerConfig) {
        cy.request({
            method: "PUT",
            url: `${apiUrl}/ui-config`,
            headers: { Authorization: authHeader },
            body: { footer: footerConfig },
        })
    }

    before(() => {
        cy.request(`${apiUrl}/ui-config`).then(({ body }) => {
            originalFooterConfig = {
                footerCopyRightText: body.footer.footerCopyRightText,
                isFooterCopyRightVisible: body.footer.isFooterCopyRightVisible,
            }
        })
    })

    beforeEach(() => {
        // Log in only to get an admin token for the API, then start every test logged out
        cy.intercept("GET", "**/api/v3/userinfo").as("userInfo")
        cy.login("Admin", "admin")
        cy.wait("@userInfo").then(({ request }) => {
            authHeader = request.headers.authorization
        })
        cy.clearAllCookies()
        cy.clearAllLocalStorage()
        cy.clearAllSessionStorage()
    })

    after(() => {
        // The UI configuration is shared, so restore it for other specs and reruns
        updateFooterConfig(originalFooterConfig)
    })

    it("shows the configured text with the current year in the footer", () => {
        updateFooterConfig({
            footerCopyRightText: "© {year} Test Org e2e",
            isFooterCopyRightVisible: true,
        })
        cy.login("Admin", "admin")
        cy.get("[data-test=footer] [data-test=copyright-text]").should(
            "have.text",
            `© ${currentYear} Test Org e2e`,
        )
    })

    it("shows the configured text on the home page", () => {
        updateFooterConfig({
            footerCopyRightText: "Copyright {year} Test Org e2e",
            isFooterCopyRightVisible: true,
        })
        cy.visit("http://localhost:8080")
        cy.get("[data-test=copyright-text]").should(
            "have.text",
            `Copyright ${currentYear} Test Org e2e`,
        )
    })

    it("hides the text when copyright visibility is turned off", () => {
        updateFooterConfig({
            footerCopyRightText: "© {year} Test Org e2e",
            isFooterCopyRightVisible: false,
        })
        cy.visit("http://localhost:8080")
        cy.get("[data-test=login-button]").should("be.visible")
        cy.get("[data-test=copyright-text]").should("not.exist")
    })
})
