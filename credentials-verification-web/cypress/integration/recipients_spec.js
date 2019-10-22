describe('Recipients list page Test', function() {
  beforeEach(function() {
    cy.visit('recipients');
  });

  it('test pagination', function() {
    cy.get('#recipientsTable tbody tr').should('have.length', 5);
    cy.get('.ant-pagination-next .ant-pagination-item-link').click();
    cy.get('#recipientsTable tbody tr').should('have.length', 5);
    cy.get('.ant-pagination-next .ant-pagination-item-link').click();
    cy.get('#recipientsTable tbody tr').should('have.length', 3);
  });
});
