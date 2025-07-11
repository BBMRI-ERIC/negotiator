describe('Download Merged PDF', () => {
  beforeEach(() => {
    cy.visit('/login')
    cy.get('[data-cy=username]').type('researcher@example.com')
    cy.get('[data-cy=password]').type('password123')
    cy.get('[data-cy=login-btn]').click()
    cy.url().should('include', '/dashboard')
  })

  it('should download merged PDF with attachments', () => {
    // Navigate to a negotiation with attachments
    cy.get('[data-cy=negotiation-card]').first().click()
    cy.url().should('include', '/negotiation/')
    
    // Click the merged PDF button
    cy.get('.merged-pdf-button').click()
    
    // Check for success notification
    cy.get('[data-cy=notification]').should('contain', 'Merged PDF successfully saved')
    
    // Verify the button exists and has the correct text
    cy.get('.merged-pdf-button').should('contain', 'Download Merged PDF')
    cy.get('.merged-pdf-button i').should('have.class', 'bi-file-earmark-pdf')
  })

  it('should show error notification when merged PDF fails', () => {
    // Navigate to a negotiation
    cy.get('[data-cy=negotiation-card]').first().click()
    cy.url().should('include', '/negotiation/')
    
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
    cy.get('[data-cy=notification]').should('contain', 'Error saving merged PDF')
  })
})