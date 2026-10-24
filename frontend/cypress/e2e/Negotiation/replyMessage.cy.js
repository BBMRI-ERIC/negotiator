/// <reference types="cypress" />

// Messages use unique text because the dev database keeps messages from earlier runs.
// Cards are found by their body: a reply card also shows the original's text, but only
// in its .reply-reference.
function cardWithText(text) {
    return cy.contains(".card-body", text).closest(".card")
}

function sendMessage(text) {
    cy.get("#message-form textarea").type(text)
    cy.get("#send").click()
}

function postMessage(channelId, text) {
    cy.get("#recipient").select(channelId)
    sendMessage(text)
    cardWithText(text).should("be.visible")
}

function firstPrivateChannel() {
    return cy.get("#recipient optgroup option").first().invoke("val")
}

describe("Test replying to a negotiation message", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
    })

    // Submitting a negotiation enables public messages; 1:1 messages stay off until approval
    context("reply to a public message", () => {
        beforeEach(() => {
            cy.get(".primary-table-row").contains("UNDER REVIEW").parent().click()
        })

        it("test reply prefills the public channel and links back to the original", () => {
            const original = `Original public message ${Date.now()}`
            const reply = `Reply to a public message ${Date.now()}`
            postMessage("public", original)

            cardWithText(original).find(".reply-button").click()
            cy.get("#recipient").should("have.value", "public")
            cy.get(".reply-preview").should("contain", original)

            sendMessage(reply)
            cardWithText(reply).find(".reply-reference").should("contain", original)
            cardWithText(reply).find(".reply-reference").click()
            cardWithText(original).should("have.class", "reply-highlight")
        })
    })

    // Approving a negotiation enables 1:1 messages
    context("reply to a private message", () => {
        beforeEach(() => {
            cy.get('.v-step-10 > .nav-link').click()
            cy.get(".primary-table-row").contains("IN PROGRESS").parent().click()
        })

        it("test reply locks the channel, keeps it after a failed send and unlocks it on cancel", () => {
            const original = `Original private message ${Date.now()}`
            firstPrivateChannel().then((organizationId) => {
                postMessage(organizationId, original)

                cardWithText(original).find(".reply-button").click()
                cy.get("#recipient").should("be.disabled").and("have.value", organizationId)

                cy.intercept("POST", "**/posts", { statusCode: 500 }).as("failedReply")
                sendMessage("This reply fails to send")
                cy.wait("@failedReply")
                cy.get(".reply-preview").should("contain", original)
                cy.get("#recipient").should("be.disabled").and("have.value", organizationId)

                cy.get(".reply-preview .btn-close").click()
                cy.get(".reply-preview").should("not.exist")
                // have.value reads jQuery's val(), which is null while the disabled placeholder is selected
                cy.get("#recipient").should("be.enabled").and("have.prop", "value", "")
            })
        })

        it("test reply to a public message keeps the channel the user picked", () => {
            const original = `Original public message ${Date.now()}`
            postMessage("public", original)

            firstPrivateChannel().then((organizationId) => {
                cy.get("#recipient").select(organizationId)
                cardWithText(original).find(".reply-button").click()
                cy.get(".reply-preview").should("contain", original)
                cy.get("#recipient").should("be.enabled").and("have.value", organizationId)
            })
        })
    })
})
