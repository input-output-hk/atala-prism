import React, { useContext } from 'react';
import { observer } from 'mobx-react-lite';
import { IMPORT_CONTACTS } from '../../../../helpers/constants';
import {
  getContactFormSkeleton,
  getContactFormColumns,
  CONTACT_INITIAL_VALUE
} from '../../../../helpers/formDefinitions/contacts';
import { DynamicFormContext } from '../../../providers/DynamicFormProvider';
import DynamicForm from '../../../dynamicForm/DynamicForm';
import { useAllContacts } from '../../../../hooks/useContactStore';

const ContactCreationTable = observer(() => {
  const { form } = useContext(DynamicFormContext);

  const { allContacts } = useAllContacts();

  const contactFormColumns = getContactFormColumns();
  const contactFormSkeleton = getContactFormSkeleton(allContacts, form);

  return (
    <DynamicForm
      columns={contactFormColumns}
      skeleton={contactFormSkeleton}
      initialValues={CONTACT_INITIAL_VALUE}
      useCase={IMPORT_CONTACTS}
    />
  );
});

export default ContactCreationTable;
