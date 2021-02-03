import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import EditableTable from '../../../common/Organisms/Tables/EditableTable';
import { contactCreationShape } from '../../../../helpers/propShapes';

const ContactCreationTable = ({ tableProps, setDisableSave }) => {
  const { t } = useTranslation();

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
      validations: ['required']
    }
  ];

  return <EditableTable {...tableProps} columns={columns} setDisableSave={setDisableSave} />;
};

ContactCreationTable.propTypes = {
  contacts: PropTypes.arrayOf(contactCreationShape).isRequired,
  updateDataSource: PropTypes.func.isRequired,
  deleteContact: PropTypes.func.isRequired,
  setDisableSave: PropTypes.func.isRequired
};

export default ContactCreationTable;
