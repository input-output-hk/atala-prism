import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import EditableTable from '../../../common/Organisms/Tables/EditableTable';
import { contactCreationShape, credentialTypeShape } from '../../../../helpers/propShapes';

const CredentialCreationTable = ({ tableProps, setDisableSave, credentialType }) => {
  const { t } = useTranslation();

  const commonColumns = [
    {
      title: t('contacts.table.columns.contactName'),
      dataIndex: 'contactName',
      editable: false,
      type: 'string',
      validations: ['required']
    },
    {
      title: t('contacts.table.columns.externalid'),
      dataIndex: 'externalid',
      editable: false,
      type: 'string',
      validations: ['required']
    }
  ];

  const specificColumns = credentialType?.fields.map(f => ({
    title: t(`contacts.table.columns.${f.key}`),
    dataIndex: f.key,
    editable: true,
    type: f.type,
    validations: f.validations
  }));

  const columns = _.uniqBy(commonColumns.concat(specificColumns), e => e.dataIndex);

  return <EditableTable {...tableProps} columns={columns} setDisableSave={setDisableSave} />;
};

CredentialCreationTable.propTypes = {
  tableProps: PropTypes.shape({
    credentialsData: PropTypes.arrayOf(contactCreationShape).isRequired,
    updateDataSource: PropTypes.func.isRequired,
    deleteContact: PropTypes.func,
    addNewRow: PropTypes.func,
    hasSelectedRecipients: PropTypes.bool
  }).isRequired,
  setDisableSave: PropTypes.func.isRequired,
  credentialType: PropTypes.shape(credentialTypeShape).isRequired
};

export default CredentialCreationTable;
