import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { useAllContacts } from '../../../../hooks/useContacts';
import { withApi } from '../../../providers/withApi';
import { IMPORT_CONTACTS } from '../../../../helpers/constants';
import {
  getContactFormSkeleton,
  getContactFormColumns,
  CONTACT_INITIAL_VALUE
} from '../../../../helpers/formDefinitions/contacts';
import { DynamicFormContext } from '../../../providers/DynamicFormProvider';
import DynamicForm from '../../../dynamicForm/DynamicForm';

const ContactCreationTable = ({ api }) => {
  const { allContacts } = useAllContacts(api.contactsManager);
  const { form } = useContext(DynamicFormContext);

  const contactFormColumns = getContactFormColumns();
  const contactFormSkeleton = getContactFormSkeleton(allContacts, form);

  return (
    <DynamicForm
      columns={contactFormColumns}
      skeleton={contactFormSkeleton}
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
