/// <reference types="cypress" />

// The ethics-vote section (step 3) of the dev access form has one FILE element
// (ethics-vote-attachment) and one TEXT_LARGE element (ethics-vote). While the
// negotiation is a DRAFT, every focus-out on the form auto-saves it.
const BASE_URL = 'http://localhost:8080'

describe('Test file uploads in the access form', () => {
  let draftNegotiation
  let file1
  let file2

  function makeFile(name, contents) {
    return { contents: Cypress.Buffer.from(contents), fileName: name, mimeType: 'text/plain' }
  }

  function pick(file) {
    cy.get('input[type=file i]').selectFile(file)
  }

  function typeAndBlur(text) {
    cy.get('textarea').type(text)
    cy.get('textarea').blur()
  }

  function focusAndBlur() {
    cy.get('textarea').focus()
    cy.get('textarea').blur()
  }

  // Creating a request can merge it into today's most recent draft, which may
  // already hold attachments from other runs, so only this run's names count.
  function attachmentNamesOfThisRun() {
    return cy
      .request({
        url: `${BASE_URL}/api/v3/negotiations/${draftNegotiation.id}/attachments`,
        headers: { Authorization: draftNegotiation.authorization },
      })
      .then((response) =>
        response.body
          .map((attachment) => attachment.name)
          .filter((name) => name === file1.fileName || name === file2.fileName),
      )
  }

  // Replaced attachments are deleted after the save, so poll for a while.
  function expectAttachmentsOfThisRun(expected, attempts = 10) {
    attachmentNamesOfThisRun().then((names) => {
      if (attempts > 1 && JSON.stringify(names) !== JSON.stringify(expected)) {
        cy.wait(500)
        expectAttachmentsOfThisRun(expected, attempts - 1)
      } else {
        expect(names).to.deep.equal(expected)
      }
    })
  }

  beforeEach(() => {
    draftNegotiation = null
    const run = Date.now()
    file1 = makeFile(`file1-${run}.txt`, `first file of run ${run}`)
    file2 = makeFile(`file2-${run}.txt`, `second file of run ${run}, which is longer`)

    cy.visit(BASE_URL)
    cy.login('Admin', 'admin')

    cy.get('[data-test=new-request-open-modal-button]').should('be.visible')
    cy.wait(500)
    cy.get('[data-test=new-request-open-modal-button]').click()
    cy.wait(500)
    cy.get('[data-test=new-request-modal-start-button]').should('be.visible')

    cy.window().then((win) => {
      cy.stub(win, 'open')
        .callsFake((url) => {
          return win.open.wrappedMethod.call(win, url, '_self')
        })
        .as('open')
    })
    // The last access form loaded is the draft the page settles on, also after
    // a merge into an existing draft.
    cy.intercept('GET', '**/api/v3/negotiations/*/access-form', (req) => {
      draftNegotiation = {
        id: req.url.match(/negotiations\/([^/]+)\/access-form/)[1],
        authorization: req.headers.authorization,
      }
    }).as('accessForm')
    cy.get('[data-test=new-request-modal-start-button]').click()
    cy.wait('@accessForm')
    cy.get('[data-test=form-navigation-next-button]').should('be.visible')
    cy.wait(1000)

    cy.then(() => {
      cy.visit(`${BASE_URL}/edit/requests/${draftNegotiation.id}?step=3`)
    })
    cy.get('input[type=file i]').should('be.visible')
    cy.get('textarea').should('be.visible')

    cy.intercept('PATCH', '**/api/v3/negotiations/*').as('patch')
  })

  afterEach(() => {
    if (draftNegotiation) {
      cy.request({
        method: 'DELETE',
        url: `${BASE_URL}/api/v3/negotiations/${draftNegotiation.id}`,
        headers: { Authorization: draftNegotiation.authorization },
      })
    }
  })

  it('replaces the attachment after the same file was picked again', () => {
    cy.intercept('POST', '**/api/v3/negotiations/*/attachments').as('upload')

    pick(file1)
    typeAndBlur('a')
    cy.wait('@upload')
    cy.wait('@patch')

    pick(file1)
    typeAndBlur('b')

    pick(file2)
    typeAndBlur('c')
    cy.wait('@upload')
    cy.wait('@patch')

    cy.contains(`Uploaded file: ${file2.fileName}`).should('be.visible')
    expectAttachmentsOfThisRun([file2.fileName])
  })

  it('uploads a picked file once although saves overlap', () => {
    cy.intercept('POST', '**/api/v3/negotiations/*/attachments', (req) => {
      req.on('response', (res) => {
        res.setDelay(3000)
      })
    }).as('upload')

    pick(file1)
    typeAndBlur('a')
    typeAndBlur('b')
    cy.wait('@upload')
    cy.wait('@patch')
    cy.wait('@patch')
    cy.wait(1000)

    cy.get('@upload.all').should('have.length', 1)
    expectAttachmentsOfThisRun([file1.fileName])
    cy.contains(`Uploaded file: ${file1.fileName}`).should('be.visible')
  })

  it('goes back to the previous file after a rejected upload', () => {
    cy.intercept('POST', '**/api/v3/negotiations/*/attachments').as('upload')

    pick(file1)
    typeAndBlur('a')
    cy.wait('@upload')
    cy.wait('@patch')
    cy.wait(1000)

    cy.intercept('POST', '**/api/v3/negotiations/*/attachments', {
      statusCode: 400,
      body: { detail: 'rejected' },
    }).as('rejectedUpload')
    cy.get('@patch.all').then((patches) => {
      const patchesBefore = patches.length
      pick(file2)
      focusAndBlur()
      cy.wait('@rejectedUpload')

      cy.contains(`Uploaded file: ${file1.fileName}`).should('be.visible')
      cy.wait(1000)
      cy.get('@patch.all').should('have.length', patchesBefore)
    })

    focusAndBlur()
    cy.wait(1000)

    cy.get('@rejectedUpload.all').should('have.length', 1)
    expectAttachmentsOfThisRun([file1.fileName])
  })
})
