/// <reference types="cypress" />

describe("Test create negotiation", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
    })

    context("click on button \"New Request\" and create negotiation", () => {
        it("test create negotiation", () => {
            cy.get(".new-request > .btn-sm").should("be.visible")
            cy.wait(500)
            cy.get(".new-request > .btn-sm").click()
            cy.wait(500)
            cy.get(".modal-content").should("be.visible")
            cy.get(".modal-body > p").should("be.visible")

            cy.get("#newRequestModal > .modal-dialog > .modal-content > .modal-footer > .btn").should("be.visible")

            cy.window().then(win => {
                cy.stub(win, "open").callsFake((url) => {
                    // call the original `win.open` method
                    // but pass the `_self` argument
                    return win.open.wrappedMethod.call(win, url, "_self")
                }).as("open")
            })
            cy.get("#newRequestModal > .modal-dialog > .modal-content > .modal-footer > .btn").click()

            // page 1
            cy.get(".middle-buttons > :nth-child(2)").contains("Next").click()

            //  form input
            // page 2
            cy.get(":nth-child(2) > .access-form-section-elements > div > .form-control").type("Test e2e negotiation", {force: true})
            cy.get(":nth-child(3) > .access-form-section-elements > div > .form-control").type("C92.1", {force: true})
            cy.get(":nth-child(4) > .access-form-section-elements > div > .form-control").type("Innovative method to detect BCR::ABL1")
            cy.get(":nth-child(5) > .access-form-section-elements > div > .form-control").type("Masaryk memorial cancer institute")

            cy.get("#boolean-7-yes").check()

            cy.get(":nth-child(7) > .access-form-section-elements > div > .form-control").type("OPJAK")

            cy.get("#inlineCheckbox-100-1").check()
            cy.get("#inlineRadio-101-1").check()
            cy.get("#boolean-102-no").check()
            //  next
            cy.get(".middle-buttons > :nth-child(2)").contains("Next").click()
            // page 3
            cy.get(":nth-child(2) > .access-form-section-elements > div > .form-control").type("In this project, we need CML and ALL (BCR::ABL1 positive) samples for checking the sensitivity and selectivity of our method.")
            cy.get(":nth-child(3) > .access-form-section-elements > div > .form-control").type("TEST")
            cy.get(".col-5 > .form-control").type(50)
            cy.get(":nth-child(5) > .access-form-section-elements > div > .form-control").type("CML (BCR::ABL1 P210) and ALL (BCR::ABL1 P190)")
            cy.get(":nth-child(6) > .access-form-section-elements > div > .form-control").type("TEST")
            //  next
            cy.get(".middle-buttons > :nth-child(2)").contains("Next").click()
            // page 4
            cy.get(":nth-child(2) > .access-form-section-elements > div > .form-control").type("Approved")
            cy.get(":nth-child(3) > .access-form-section-elements > div > .form-control").should("be.visible")
            //  next
            cy.get(".middle-buttons > :nth-child(2)").contains("Next").click()
            // page 5
            //  Overview*
            cy.get(".overview > .mb-3").should("be.visible")
            cy.get(".overview > .mb-0").should("be.visible")

            //  Submit request
            cy.get(".justify-content-end > .btn").contains("Submit").click()
            // Confirmation modal
            cy.get(".modal-content").should("be.visible")
            cy.get(".modal-title").should("be.visible")
            cy.get(".modal-body > p").should("be.visible")
            cy.get(".btn-dark").should("be.visible")
            cy.get(".btn-danger").should("be.visible")

            // Cancel request
            cy.wait(200)
            cy.get(".btn-danger").should("be.visible")
            cy.wait(500)
            cy.get(".btn-dark").eq(0).click()
            cy.get(".btn-danger").should("not.be.visible")

            // Submit request
            cy.wait(200)
            cy.get(".justify-content-end > .btn").contains("Submit").click()
            cy.wait(200)
            cy.get(".btn-danger").eq(0).click()

            cy.url().should("contain", "/negotiations")
            cy.url().should("contain", "/ROLE_RESEARCHER")
        }),
        
        it("test saving negotiation as draft by clicking on Next", () => {
          cy.get(".new-request > .btn-sm").should("be.visible")
          cy.wait(500)
          cy.get(".new-request > .btn-sm").click()
          cy.wait(500)
          cy.get(".modal-content").should("be.visible")
          cy.get(".modal-body > p").should("be.visible")

          cy.get("#newRequestModal > .modal-dialog > .modal-content > .modal-footer > .btn").should("be.visible")

          cy.window().then(win => {
              cy.stub(win, "open").callsFake((url) => {
                  // call the original `win.open` method
                  // but pass the `_self` argument
                  return win.open.wrappedMethod.call(win, url, "_self")
              }).as("open")
          })
          cy.get("#newRequestModal > .modal-dialog > .modal-content > .modal-footer > .btn").click()

          // page 1
          cy.get(".middle-buttons > :nth-child(2)").contains("Next").click()

          //  form input
          // page 2
          cy.get(":nth-child(2) > .access-form-section-elements > div > .form-control").type("Test e2e negotiation", {force: true})
          cy.get(":nth-child(3) > .access-form-section-elements > div > .form-control").type("C92.1", {force: true})
          cy.get(":nth-child(4) > .access-form-section-elements > div > .form-control").type("Innovative method to detect BCR::ABL1")
          cy.get(":nth-child(5) > .access-form-section-elements > div > .form-control").type("Masaryk memorial cancer institute")

          cy.get("#boolean-7-yes").check()

          cy.get(":nth-child(7) > .access-form-section-elements > div > .form-control").type("OPJAK")

          cy.get("#inlineCheckbox-100-1").check()
          cy.get("#inlineRadio-101-1").check()
          cy.get("#boolean-102-no").check()
          //  next
          cy.get(".middle-buttons > :nth-child(2)").contains("Next").click()

          // Submit request
          cy.wait(200)
          cy.get(".alert").should("be.visible")
          cy.get(".alert").contains("Negotiation saved correctly as draft")
          cy.url().should("contain", "/edit/requests")
        })
       
    })
})
