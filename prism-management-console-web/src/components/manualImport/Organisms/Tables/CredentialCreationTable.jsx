import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import EditableTable from '../../../common/Organisms/Tables/EditableTable';
import { credentialShape, credentialTypeShape } from '../../../../helpers/propShapes';
import {
  CREDENTIAL_FORM,
  CREDENTIAL_FORM_COLUMNS
} from '../../../../helpers/formDefinitions/credentials';
import { DynamicFormContext } from '../../../../providers/DynamicFormProvider';
import { IMPORT_CREDENTIALS_DATA } from '../../../../helpers/constants';

const CredentialCreationTable = ({ tableProps, setDisableSave, credentialType }) => {
  const { t } = useTranslation();
  const { form } = useContext(DynamicFormContext);

  const commonColumns = [
    {
      title: t('contacts.table.columns.contactName'),
      dataIndex: 'contactName',
      editable: false,
      type: 'string',
      validations: ['required']
    },
    {
      title: t('contacts.table.columns.externalId'),
      dataIndex: 'externalId',
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

  return (
    <EditableTable
      {...tableProps}
      columns={CREDENTIAL_FORM_COLUMNS(columns)}
      skeleton={CREDENTIAL_FORM(columns, form)}
      initialValues={tableProps.dataSource}
      useCase={IMPORT_CREDENTIALS_DATA}
    />
  );
};

CredentialCreationTable.propTypes = {
  tableProps: PropTypes.shape({
    // FIXME: datasource shape
    dataSource: PropTypes.arrayOf(credentialShape).isRequired,
    updateDataSource: PropTypes.func.isRequired,
    deleteContact: PropTypes.func,
    addNewRow: PropTypes.func,
    hasSelectedRecipients: PropTypes.bool
  }).isRequired,
  setDisableSave: PropTypes.func.isRequired,
  credentialType: PropTypes.shape(credentialTypeShape).isRequired
};

export default CredentialCreationTable;
