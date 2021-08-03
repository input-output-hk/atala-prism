import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import { credentialTypeShape } from '../../../../helpers/propShapes';
import {
  CREDENTIAL_FORM,
  CREDENTIAL_FORM_COLUMNS
} from '../../../../helpers/formDefinitions/credentials';
import { DynamicFormContext } from '../../../../providers/DynamicFormProvider';
import { IMPORT_CREDENTIALS_DATA } from '../../../../helpers/constants';
import DynamicForm from '../../../dynamicForm/DynamicForm';

const CredentialCreationTable = ({ initialValues, credentialType }) => {
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
    <DynamicForm
      columns={CREDENTIAL_FORM_COLUMNS(columns)}
      skeleton={CREDENTIAL_FORM(columns, form)}
      initialValues={initialValues}
      useCase={IMPORT_CREDENTIALS_DATA}
    />
  );
};

CredentialCreationTable.propTypes = {
  initialValues: PropTypes.shape({
    // FIXME: datasource shape
  }).isRequired,
  credentialType: PropTypes.shape(credentialTypeShape).isRequired
};

export default CredentialCreationTable;
