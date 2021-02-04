import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import EditableTable from '../../../common/Organisms/Tables/EditableTable';
import { useAllContacts } from '../../../../hooks/useContacts';
import { withApi } from '../../../providers/withApi';

const ContactCreationTable = ({ api, tableProps, setDisableSave }) => {
  const { t } = useTranslation();
  const { contacts } = useAllContacts(api.contactsManager);

  const columns = [
    {
      title: t('contacts.table.columns.contactName'),
      dataIndex: 'contactName',
      editable: true,
      type: 'string',
      validations: ['required']
    },
    {
      title: t('contacts.table.columns.externalid'),
      dataIndex: 'externalid',
      editable: true,
      type: 'string',
      validations: ['required', 'unique', 'checkPreexisting']
    }
  ];

  return (
    <EditableTable
      {...tableProps}
      columns={columns}
      setDisableSave={setDisableSave}
      preExistingEntries={contacts}
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
