import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { useTranslation } from 'react-i18next';
import { contactShape, credentialTypeShape } from '../../../../helpers/propShapes';
import {
  getCredentialFormSkeleton,
  getCredentialFormColumns
} from '../../../../helpers/formDefinitions/credentials';
import {
  CREDENTIAL_TYPE_FIELD_TYPES,
  IMPORT_CREDENTIALS_DATA,
  VALIDATION_KEYS
} from '../../../../helpers/constants';
import DynamicForm from '../../../dynamicForm/DynamicForm';
import { DynamicFormContext } from '../../../providers/DynamicFormProvider';
import { humanizeCamelCaseString } from '../../../../helpers/genericHelpers';

const CredentialCreationTable = ({ recipients, credentialType }) => {
  const { t } = useTranslation();
  const { form } = useContext(DynamicFormContext);

  const commonColumns = [
    {
      title: t('contacts.table.columns.contactName'),
      dataIndex: 'contactName',
      editable: false,
      type: CREDENTIAL_TYPE_FIELD_TYPES.STRING,
      validations: [VALIDATION_KEYS.REQUIRED],
      fixed: 'left'
    },
    {
      title: t('contacts.table.columns.externalId'),
      dataIndex: 'externalId',
      editable: false,
      type: CREDENTIAL_TYPE_FIELD_TYPES.STRING,
      validations: [VALIDATION_KEYS.REQUIRED],
      width: 350,
      fixed: 'left'
    }
  ];

  const specificColumns = credentialType.fields.map(f => ({
    title: t(`contacts.table.columns.${f.key}`, { defaultValue: humanizeCamelCaseString(f.key) }),
    dataIndex: f.key,
    editable: true,
    type: f.type,
    validations: f.validations
  }));

  const columns = _.uniqBy(commonColumns.concat(specificColumns), e => e.dataIndex);

  const credentialFormColumns = getCredentialFormColumns(columns);
  const credentialFormSkeleton = getCredentialFormSkeleton(columns, form);

  const credentialFieldKeys = columns.map(c => c.dataIndex);
  const initialValues = recipients.map(r => _.pick(r, credentialFieldKeys));

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
  recipients: PropTypes.arrayOf(contactShape).isRequired,
  credentialType: credentialTypeShape.isRequired
};

export default CredentialCreationTable;
