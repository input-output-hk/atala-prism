import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import EditableTable from '../../../common/Organisms/Tables/EditableTable';
import { useAllContacts } from '../../../../hooks/useContacts';
import { withApi } from '../../../providers/withApi';
import { IMPORT_CONTACTS } from '../../../../helpers/constants';
import {
  CONTACT_FORM,
  CONTACT_FORM_COLUMNS,
  CONTACT_INITIAL_VALUE
} from '../../../../helpers/formDefinitions/contacts';
import { DynamicFormContext } from '../../../../providers/DynamicFormProvider';

const ContactCreationTable = ({ api, tableProps, setDisableSave }) => {
  const { allContacts } = useAllContacts(api.contactsManager);
  const { form } = useContext(DynamicFormContext);

  return (
    <EditableTable
      {...tableProps}
      columns={CONTACT_FORM_COLUMNS()}
      skeleton={CONTACT_FORM(allContacts, form)}
      initialValues={CONTACT_INITIAL_VALUE}
      setDisableSave={setDisableSave}
      preExistingEntries={allContacts}
      useCase={IMPORT_CONTACTS}
    />
  );
};

ContactCreationTable.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ getContact: PropTypes.func })
  }).isRequired,
  tableProps: PropTypes.shape({}).isRequired,
  setDisableSave: PropTypes.func.isRequired
};

export default withApi(ContactCreationTable);
