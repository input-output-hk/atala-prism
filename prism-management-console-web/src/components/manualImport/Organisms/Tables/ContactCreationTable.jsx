import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import EditableTable from '../../../common/Organisms/Tables/EditableTable';
import { contactCreationShape } from '../../../../helpers/propShapes';

const ContactCreationTable = ({ contacts, updateDataSource, deleteContact, setDisableSave }) => {
  const { t } = useTranslation();

  const columns = [
    {
      title: t('manualImport.table.contactName'),
      dataIndex: 'contactName',
      editable: true,
      type: 'string',
      validations: ['required']
    },
    {
      title: t('manualImport.table.externalid'),
      dataIndex: 'externalid',
      editable: true,
      type: 'string',
      validations: ['required']
    }
  ];

  return (
    <EditableTable
      columns={columns}
      dataSource={contacts}
      deleteRow={deleteContact}
      updateDataSource={updateDataSource}
      setDisableSave={setDisableSave}
    />
  );
};

ContactCreationTable.propTypes = {
  contacts: PropTypes.arrayOf(contactCreationShape).isRequired,
  updateDataSource: PropTypes.func.isRequired,
  deleteContact: PropTypes.func.isRequired,
  setDisableSave: PropTypes.func.isRequired
};

export default ContactCreationTable;
