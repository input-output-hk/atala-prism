import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../providers/withApi';
import {
  COMMON_CONTACT_HEADERS,
  IMPORT_CONTACTS,
  IMPORT_CREDENTIALS_DATA
} from '../../helpers/constants';
import { dateFormat } from '../../helpers/formatters';
import Logger from '../../helpers/Logger';
import ManualImport from './ManualImport';
import { contactShape, credentialTypeShape } from '../../helpers/propShapes';

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

  const createBlankContact = key => ({
    ...blankContact,
    key
  });

  const createBlankCredential = key => ({
    ...blankContact,
    ...credentialType?.fields.map(f => ({ [f.key]: '' })),
    key
  });

  const [contacts, setContacts] = useState([createBlankContact(0)]);
  const [selectedGroups, setSelectedGroups] = useState([]);
  const [groups, setGroups] = useState([]);
  const [credentialsData, setCredentialsData] = useState(
    hasSelectedRecipients ? recipients : [createBlankCredential(0)]
  );

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

  const handleAddNewContact = () => {
    const { key = 0 } = _.last(contacts) || {};

    const newContact = createBlankContact(key + 1);
    const newContactList = contacts.concat(newContact);

    setContacts(newContactList);
  };

  const handleDeleteContact = key => {
    const filteredContacts = contacts.filter(({ key: contactKey }) => key !== contactKey);
    const last = _.last(contacts) || {};

    const contactsToSave = filteredContacts.length
      ? filteredContacts
      : [createBlankContact(last.key + 1)];

    setContacts(contactsToSave);
  };

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
    [IMPORT_CONTACTS]: {
      dataSource: contacts,
      updateDataSource: setContacts,
      deleteRow: handleDeleteContact,
      addRow: handleAddNewContact
    },
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

  const handleSave = {
    [IMPORT_CONTACTS]: () => onSave({ contacts, groups: selectedGroups }),
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
