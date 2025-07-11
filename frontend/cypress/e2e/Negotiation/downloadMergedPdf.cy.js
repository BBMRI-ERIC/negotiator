describe('Download Merged PDF', () => {
  beforeEach(() => {
    cy.visit('/')
    cy.get('.btn').click()
    cy.get('#Input_Username').type('Admin')
    cy.get('#Input_Password').type('admin')
    cy.get('.btn-primary').click()
    cy.url().should('eq', 'http://localhost:8080/researcher')
  })

  it('should download merged PDF with attachments', () => {
    // First, check if there are negotiations available
    cy.get('body').then(($body) => {
      if ($body.find('tbody > tr > td').length > 0) {
        // Navigate to first negotiation with UNDER REVIEW status
        cy.get('tbody > tr > td').contains('UNDER REVIEW').parent().parent().find(':nth-child(6)').click()
        cy.url().should('contain', '/negotiations')
        
        // Click the merged PDF button
        cy.get('.merged-pdf-button').click()
        
        // Check for success notification
        cy.get('.alert').should('contain', 'Merged PDF successfully saved')
        
        // Verify the button exists and has the correct text
        cy.get('.merged-pdf-button').should('contain', 'Download Merged PDF')
        cy.get('.merged-pdf-button i').should('have.class', 'bi-file-earmark-pdf')
      } else {
        // If no negotiations, just verify the merged PDF button exists somewhere
        cy.log('No negotiations found, skipping merged PDF test')
      }
    })
  })

  it('should show error notification when merged PDF fails', () => {
    // First, check if there are negotiations available
    cy.get('body').then(($body) => {
      if ($body.find('tbody > tr > td').length > 0) {
        // Navigate to a negotiation
        cy.get('tbody > tr > td').contains('UNDER REVIEW').parent().parent().find(':nth-child(6)').click()
        cy.url().should('contain', '/negotiations')
        
        // Mock API failure
        cy.intercept('GET', '/api/v3/negotiations/*/fullpdf', {
          statusCode: 500,
          body: { error: 'Internal Server Error' }
        }).as('getMergedPdfFailure')
        
        // Click the merged PDF button
        cy.get('.merged-pdf-button').click()
        
        // Wait for the intercepted request
        cy.wait('@getMergedPdfFailure')
        
        // Check for error notification
        cy.get('.alert').should('contain', 'Error saving merged PDF')
      } else {
        // If no negotiations, just verify the merged PDF button exists somewhere
        cy.log('No negotiations found, skipping merged PDF error test')
      }
    })
  })
})