import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import { contactShape, credentialTypeShape } from '../../../../helpers/propShapes';
import {
  getCredentialFormSkeleton,
  getCredentialFormColumns
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
      validations: ['required'],
      fixed: 'left'
    },
    {
      title: t('contacts.table.columns.externalId'),
      dataIndex: 'externalId',
      editable: false,
      type: 'string',
      validations: ['required'],
      width: 350,
      fixed: 'left'
    }
  ];

  const specificColumns = credentialType.fields.map(f => ({
    title: t(`contacts.table.columns.${f.key}`),
    dataIndex: f.key,
    editable: true,
    type: f.type,
    validations: f.validations
  }));

  const columns = _.uniqBy(commonColumns.concat(specificColumns), e => e.dataIndex);

  const credentialFormColumns = getCredentialFormColumns(columns);
  const credentialFormSkeleton = getCredentialFormSkeleton(columns, form);

  return (
    <DynamicForm
      columns={credentialFormColumns}
      skeleton={credentialFormSkeleton}
      initialValues={initialValues}
      useCase={IMPORT_CREDENTIALS_DATA}
    />
  );
};

CredentialCreationTable.propTypes = {
  initialValues: PropTypes.arrayOf(contactShape).isRequired,
  credentialType: PropTypes.shape(credentialTypeShape).isRequired
};

export default CredentialCreationTable;
