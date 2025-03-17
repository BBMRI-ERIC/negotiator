/// <reference types="cypress" />

describe("Test create negotiation", () => {
    beforeEach(() => {
        cy.visit("http://localhost:8080")
        cy.login("Admin", "admin")
    })

    context("click on button \"New Request\" and create negotiation", () => {
        it("test create negotiation", () => {
            cy.get(":nth-child(2) > :nth-child(1) > .btn-sm").should("be.visible")
            cy.wait(500)
            cy.get(":nth-child(2) > :nth-child(1) > .btn-sm").click()
            cy.wait(500)
            cy.get(".modal-content").should("be.visible")
            cy.get(".modal-body > p").should("be.visible")

            cy.get(".modal-footer > .btn").should("be.visible")

            cy.window().then(win => {
                cy.stub(win, "open").callsFake((url) => {
                    // call the original `win.open` method
                    // but pass the `_self` argument
                    return win.open.wrappedMethod.call(win, url, "_self")
                }).as("open")
            })
            cy.get(".modal-footer > .btn").click()

            // page 1
            cy.get(".wizard-footer-right > .btn").contains("Save Draft").should("be.visible")
            cy.get(".wizard-footer-right > .btn").contains("Next").click()

            //  form input
            // page 2
            cy.get("#Project1 > :nth-child(2) > div > .form-control").type("Test e2e negotiation")
            cy.get("#Project1 > :nth-child(3) > div > .form-control").type("C92.1")
            cy.get("#Project1 > :nth-child(4) > div > .form-control").type("Innovative method to detect BCR::ABL1")
            cy.get("#Project1 > :nth-child(5) > div > .form-control").type("Masaryk memorial cancer institute")

            cy.get(":nth-child(6) > :nth-child(3) > :nth-child(2) > #inlineRadio2").check()

            cy.get(":nth-child(7) > div > .form-control").type("OPJAK")

            cy.get(":nth-child(1) > .form-check > #inlineCheckbox1").check()
            cy.get(":nth-child(1) > .form-check > #inlineRadio1").check()
            cy.get(":nth-child(10) > :nth-child(3) > :nth-child(1) > #inlineRadio1").check()
            //  next
            cy.get(".wizard-footer-right > .btn").contains("Save Draft").should("be.visible")
            cy.get(".wizard-footer-right > .btn").contains("Next").click()
            // page 3
            cy.get("#Request2 > :nth-child(2) > div > .form-control").type("In this project, we need CML and ALL (BCR::ABL1 positive) samples for checking the sensitivity and selectivity of our method.")
            cy.get("#Request2 > :nth-child(3) > div > .form-control").type("TEST")
            cy.get(".col-5 > .form-control").type(50)
            cy.get("#Request2 > :nth-child(5) > div > .form-control").type("CML (BCR::ABL1 P210) and ALL (BCR::ABL1 P190)")
            cy.get(":nth-child(6) > div > .form-control").type("TEST")
            //  next
            cy.get(".wizard-footer-right > .btn").contains("Save Draft").should("be.visible")
            cy.get(".wizard-footer-right > .btn").contains("Next").click()
            // page 4
            cy.get("#Ethicsvote3 > :nth-child(2) > div > .form-control").type("Approved")
            cy.get("#Ethicsvote3 > :nth-child(3) > div > .form-control").should("be.visible")
            //  next
            cy.get(".wizard-footer-right > .btn").contains("Save Draft").should("be.visible")
            cy.get(".wizard-footer-right > .btn").contains("Next").click()
            // page 5
            //  Overview*
            cy.get(":nth-child(1) > .mb-3").should("be.visible")
            cy.get("#Overview4 > :nth-child(1)  > :nth-child(2)").should("be.visible")

            //    PROJECT
            cy.get("#Overview4 > :nth-child(2) > .mb-3").should("be.visible")

            cy.get("#Overview4 > :nth-child(2) > :nth-child(2)").should("be.visible")
            cy.get("#Overview4 > :nth-child(2) > :nth-child(3)").should("be.visible")
            cy.get("#Overview4 > :nth-child(2) > :nth-child(4)").should("be.visible")
            cy.get("#Overview4 > :nth-child(2) > :nth-child(5)").should("be.visible")
            cy.get("#Overview4 > :nth-child(2) > :nth-child(6)").should("be.visible")
            cy.get("#Overview4 > :nth-child(2) > :nth-child(7)").should("be.visible")
            cy.get("#Overview4 > :nth-child(2) > :nth-child(8)").should("be.visible")
            cy.get("#Overview4 > :nth-child(2) > :nth-child(9)").should("be.visible")
            cy.get("#Overview4 > :nth-child(2) > :nth-child(10)").should("be.visible")

            //    REQUEST
            cy.get(":nth-child(3) > .mb-3").should("be.visible")

            cy.get("#Overview4 > :nth-child(3) > :nth-child(2)").should("be.visible")
            cy.get("#Overview4 > :nth-child(3) > :nth-child(3)").should("be.visible")
            cy.get("#Overview4 > :nth-child(3) > :nth-child(4)").should("be.visible")
            cy.get("#Overview4 > :nth-child(3) > :nth-child(5)").should("be.visible")
            cy.get("#Overview4 > :nth-child(3) > :nth-child(6)").should("be.visible")

            //    ETHICS VOTE
            cy.get(":nth-child(4) > .mb-3").should("be.visible")

            cy.get("#Overview4 > :nth-child(4) > :nth-child(2)").should("be.visible")
            cy.get("#Overview4 > :nth-child(4) > :nth-child(3)").should("be.visible")
            //  Submit request
            cy.get(".wizard-footer-right > .btn").contains("Save Draft").should("be.visible")
            cy.get(".wizard-footer-right > .btn").contains("Submit request").click()

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
            cy.get(".btn-dark").click()
            cy.get(".btn-danger").should("not.be.visible")

            // Submit request
            cy.wait(200)
            cy.get(".wizard-footer-right > .btn").contains("Submit request").click()
            cy.wait(200)
            cy.get(".btn-danger").click()

            cy.url().should("contain", "/negotiations")
            cy.url().should("contain", "/ROLE_RESEARCHER")
        }),
        it("test saving negotiation as draft", () => {
          cy.get(":nth-child(2) > :nth-child(1) > .btn-sm").should("be.visible")
          cy.wait(500)
          cy.get(":nth-child(2) > :nth-child(1) > .btn-sm").click()
          cy.wait(500)
          cy.get(".modal-content").should("be.visible")
          cy.get(".modal-body > p").should("be.visible")

          cy.get(".modal-footer > .btn").should("be.visible")

          cy.window().then(win => {
              cy.stub(win, "open").callsFake((url) => {
                  // call the original `win.open` method
                  // but pass the `_self` argument
                  return win.open.wrappedMethod.call(win, url, "_self")
              }).as("open")
          })
          cy.get(".modal-footer > .btn").click()

          // page 1
          cy.get(".wizard-footer-right > .btn").contains("Save Draft").click()

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
          cy.get(".btn-dark").click()
          cy.get(".btn-danger").should("not.be.visible")

          // Submit request
          cy.wait(200)
          cy.get(".wizard-footer-right > .btn").contains("Save Draft").click()
          cy.wait(200)
          cy.get(".btn-danger").click()

          cy.url().should("contain", "/negotiations")
          cy.url().should("contain", "/ROLE_RESEARCHER")
        })
    })
})
