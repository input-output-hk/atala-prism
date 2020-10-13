import React from 'react';
import { useHistory } from 'react-router-dom';

export const withRedirector = Component => props => {
  const history = useHistory();

  const redirectTo = route => history.push(`/${route}`);

  const redirectToHome = () => history.push('/');

  const redirectToNewCredential = () => redirectTo('credentials/create');

  const redirectToRegistration = () => redirectTo('registration');

  const redirectToCredentials = () => redirectTo('credentials');

  const redirectToContacts = () => redirectTo('contacts');

  const redirectToImportContacts = () => redirectTo('contacts/import');

  const redirectToBulkImport = () => redirectTo('contacts/import/bulk');

  const redirectToManualImport = () => redirectTo('contacts/import/manual');

  const redirectToStudentCreation = () => redirectTo('studentCreation');

  const redirectToIndividualCreation = () => redirectTo('individualCreation');

  const redirectToGroupCreation = () => redirectTo('groups/creation');

  const redirectToGroups = () => redirectTo('groups');

  const redirector = {
    redirectToHome,
    redirectToNewCredential,
    redirectToRegistration,
    redirectToCredentials,
    redirectToContacts,
    redirectToImportContacts,
    redirectToBulkImport,
    redirectToManualImport,
    redirectToStudentCreation,
    redirectToIndividualCreation,
    redirectToGroupCreation,
    redirectToGroups
  };

  return <Component {...props} redirector={redirector} />;
};
