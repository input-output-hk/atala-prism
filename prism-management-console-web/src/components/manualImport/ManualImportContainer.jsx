import React, { useState, useEffect, useContext } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../providers/withApi';
import { DynamicFormContext } from '../../providers/DynamicFormProvider';
import ManualImport from './ManualImport';
import Logger from '../../helpers/Logger';
import { contactShape, credentialTypeShape } from '../../helpers/propShapes';
import {
  COMMON_CONTACT_HEADERS,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA
} from '../../helpers/constants';
import { dateFormat } from '../../helpers/formatters';
import { getFirstError } from '../../helpers/formHelpers';

const blankContact = {
  externalid: '',
  contactName: ''
};

const ManualImportContainer = ({
  api: { groupsManager },
  onSave,
  cancelImport,
  loading,
  useCaseProps,
  credentialType,
  recipients,
  hasSelectedRecipients
}) => {
  const { t } = useTranslation();
  const { useCase, showGroupSelection } = useCaseProps;

  const createBlankCredential = key => ({
    ...blankContact,
    ...credentialType?.fields.map(f => ({ [f.key]: '' })),
    key
  });

  const [selectedGroups, setSelectedGroups] = useState([]);
  const [groups, setGroups] = useState([]);
  const [credentialsData, setCredentialsData] = useState(
    hasSelectedRecipients ? recipients : [createBlankCredential(0)]
  );
  const { form } = useContext(DynamicFormContext);

  useEffect(() => {
    if (showGroupSelection) {
      groupsManager
        .getGroups()
        .then(setGroups)
        .catch(error => {
          Logger.error('[GroupsContainer.updateGroups] Error: ', error);
          message.error(t('errors.errorGetting', { model: t('groups.title') }));
        });
    }
  }, []);

  const handleAddNewCredential = () => {
    const { key = 0 } = _.last(credentialsData) || {};

    const newCredential = createBlankCredential(key + 1);
    const newCredentialList = credentialsData.concat(newCredential);

    setCredentialsData(newCredentialList);
  };

  const handleDeleteCredential = key => {
    const filteredCredentials = credentialsData.filter(
      ({ key: credentialKey }) => key !== credentialKey
    );
    const last = _.last(credentialsData) || {};

    const credentialsToSave = filteredCredentials.length
      ? filteredCredentials
      : [createBlankCredential(last.key + 1)];

    setCredentialsData(credentialsToSave);
  };

  const tableProps = {
    // backward compatibility
    [IMPORT_CONTACTS]: {},
    [IMPORT_CREDENTIALS_DATA]: {
      dataSource: credentialsData,
      updateDataSource: setCredentialsData,
      deleteRow: !hasSelectedRecipients && handleDeleteCredential,
      addRow: !hasSelectedRecipients && handleAddNewCredential,
      hasSelectedRecipients
    }
  };

  const processCredentials = credentials => {
    const fieldsToInclude = COMMON_CONTACT_HEADERS.concat(credentialType.fields.map(f => f.key));

    const dateFields = credentialType.fields
      .filter(({ type }) => type === 'date')
      .map(({ key }) => key);

    const trimmedCredentials = credentials.map(r =>
      _.pickBy(r, (_value, key) => fieldsToInclude.includes(key))
    );

    const parsedCredentials = trimmedCredentials.map(c =>
      dateFields.reduce((acc, df) => Object.assign(acc, { [df]: dateFormat(c[df]) }), c)
    );

    return parsedCredentials;
  };

  const handleSaveContacts = async () => {
    try {
      const data = form.getFieldValue(IMPORT_CONTACTS);
      const parsedData = data.map((item, key) => ({ ...item, key }));
      await form.validateFields();
      onSave({ contacts: parsedData, groups: selectedGroups });
    } catch (error) {
      Logger.error('An error occurred while saving contacts', error);
      message.error(getFirstError(error));
    }
  };

  const handleSave = {
    [IMPORT_CONTACTS]: handleSaveContacts,
    [IMPORT_CREDENTIALS_DATA]: () => onSave({ credentials: processCredentials(credentialsData) })
  };

  const groupsProps = { groups, selectedGroups, setSelectedGroups };

  return (
    <ManualImport
      tableProps={tableProps[useCase]}
      groupsProps={groupsProps}
      onSave={handleSave[useCase]}
      cancelImport={cancelImport}
      loading={loading}
      credentialType={credentialType}
      recipients={recipients}
      {...useCaseProps}
    />
  );
};

ManualImportContainer.defaultProps = {
  credentialType: null,
  recipients: []
};

ManualImportContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  onSave: PropTypes.func.isRequired,
  cancelImport: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  useCaseProps: PropTypes.shape({
    useCase: PropTypes.oneOf([IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA]).isRequired,
    showGroupSelection: PropTypes.func.isRequired,
    isEmbedded: PropTypes.bool.isRequired
  }).isRequired,
  credentialType: PropTypes.shape(credentialTypeShape),
  recipients: PropTypes.arrayOf(contactShape),
  hasSelectedRecipients: PropTypes.bool.isRequired
};

export default withApi(ManualImportContainer);
