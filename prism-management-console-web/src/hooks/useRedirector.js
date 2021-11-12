import { useHistory } from 'react-router-dom';

export const useRedirector = () => {
  const history = useHistory();

  const redirectTo = route => history.push(`/${route}`);

  const redirectToHome = () => history.push('/');

  const redirectToNewCredential = () => redirectTo('credentials/create');

  const redirectToRegistration = () => redirectTo('registration');

  const redirectToCredentials = () => redirectTo('credentials');

  const redirectToContacts = () => redirectTo('contacts');

  const redirectToContactDetails = (id, isEdit) =>
    redirectTo(`contacts/${id}${isEdit ? '?editing=true' : ''}`);

  const redirectToImportContacts = () => redirectTo('contacts/import');

  const redirectToBulkImport = () => redirectTo('contacts/import/bulk');

  const redirectToManualImport = () => redirectTo('contacts/import/manual');

  const redirectToStudentCreation = () => redirectTo('studentCreation');

  const redirectToIndividualCreation = () => redirectTo('individualCreation');

  const redirectToGroupCreation = () => redirectTo('groups/creation');

  const redirectToGroups = () => redirectTo('groups');

  const redirectToCredentialTemplates = () => redirectTo('templates');

  const redirectToCredentialTemplateCreation = () => redirectTo('templates/create');

  return {
    redirectToHome,
    redirectToNewCredential,
    redirectToRegistration,
    redirectToCredentials,
    redirectToContacts,
    redirectToContactDetails,
    redirectToImportContacts,
    redirectToBulkImport,
    redirectToManualImport,
    redirectToStudentCreation,
    redirectToIndividualCreation,
    redirectToGroupCreation,
    redirectToGroups,
    redirectToCredentialTemplates,
    redirectToCredentialTemplateCreation
  };
};
