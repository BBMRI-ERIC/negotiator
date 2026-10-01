/// <reference types="cypress" />

// Element 103 (SINGLE_CHOICE_DROPDOWN) is linked to value set 101, so a value set
// fetched by element id instead of linked value set id would fail.
const expectedValues = ["first_choice", "second_choice", "third_choice"]
const valueSetError = "Error getting value-sets request data from server"

function assertDropdownOptions(selector) {
    cy.get(selector)
        .filter(":visible")
        .first()
        .find("option")
        .should(($options) => {
            const values = [...$options].map((option) => option.value).filter(Boolean)
            expect(values).to.deep.equal(expectedValues)
        })
}

describe("Test value sets of access form elements", () => {
    let valueSetStatuses

    beforeEach(() => {
        valueSetStatuses = []
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
        cy.intercept("GET", "**/api/v3/value-sets/*", (req) => {
            req.continue((res) => {
                valueSetStatuses.push(res.statusCode)
            })
        }).as("valueSet")
    })

    function assertAllValueSetsLoaded() {
        cy.wrap(null).should(() => {
            expect(valueSetStatuses).to.not.be.empty
            expect(valueSetStatuses.every((status) => status === 200)).to.equal(true)
        })
        cy.contains(valueSetError).should("not.exist")
    }

    context("fill out the access form of a new request", () => {
        it("loads the value set linked to the dropdown element", () => {
            cy.get("[data-test=new-request-open-modal-button]").should("be.visible")
            cy.wait(500)
            cy.get("[data-test=new-request-open-modal-button]").click()
            cy.wait(500)
            cy.get("[data-test=new-request-modal-start-button]").should("be.visible")

            cy.window().then(win => {
                cy.stub(win, "open").callsFake((url) => {
                    return win.open.wrappedMethod.call(win, url, "_self")
                }).as("open")
            })
            cy.intercept("GET", "**/api/v3/negotiations/*/access-form").as("accessForm")
            cy.get("[data-test=new-request-modal-start-button]").click()
            cy.wait("@accessForm")

            // page 1
            cy.wait(500)
            cy.get("[data-test=form-navigation-next-button]").click()

            // page 2: project section
            cy.get("#inlineCheckbox-100-1").should("exist")
            cy.get("#inlineRadio-101-1").should("exist")
            assertDropdownOptions("#dropdown-103")
            assertAllValueSetsLoaded()
        })
    })

    context("edit an access form as admin", () => {
        it("loads the value set linked to the dropdown element", () => {
            cy.visit("http://localhost:8080/settings/access-forms/edit/3")

            assertDropdownOptions("#dropdown-103")
            assertAllValueSetsLoaded()
        })
    })
})
