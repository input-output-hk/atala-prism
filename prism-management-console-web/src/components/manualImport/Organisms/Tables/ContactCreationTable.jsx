import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { useAllContacts } from '../../../../hooks/useContacts';
import { withApi } from '../../../providers/withApi';
import { IMPORT_CONTACTS } from '../../../../helpers/constants';
import {
  CONTACT_FORM,
  CONTACT_FORM_COLUMNS,
  CONTACT_INITIAL_VALUE
} from '../../../../helpers/formDefinitions/contacts';
import { DynamicFormContext } from '../../../../providers/DynamicFormProvider';
import DynamicForm from '../../../dynamicForm/DynamicForm';

const ContactCreationTable = ({ api }) => {
  const { allContacts } = useAllContacts(api.contactsManager);
  const { form } = useContext(DynamicFormContext);

  return (
    <DynamicForm
      columns={CONTACT_FORM_COLUMNS()}
      skeleton={CONTACT_FORM(allContacts, form)}
      initialValues={CONTACT_INITIAL_VALUE}
      preExistingEntries={allContacts}
      useCase={IMPORT_CONTACTS}
    />
  );
};

ContactCreationTable.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ getContact: PropTypes.func })
  }).isRequired,
  tableProps: PropTypes.shape({}).isRequired
};

export default withApi(ContactCreationTable);
