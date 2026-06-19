// Check that the silent renew callback page is served

describe("OIDC silent renew callback page", () => {
    it("serves the silent renew callback html file", () => {
        cy.visit("http://localhost:8080/silent-renew-oidc.html")
        cy.title().should("eq", "OIDC Silent Renew")
    })
})
