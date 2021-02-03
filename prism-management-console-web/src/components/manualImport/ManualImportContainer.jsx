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

const blankContact = {
  externalid: '',
  contactName: '',
  key: 0
};

const ManualImportContainer = ({
  api: { groupsManager },
  onSave,
  cancelImport,
  loading,
  useCaseProps,
  credentialType,
  recipients
}) => {
  const { t } = useTranslation();
  const { useCase, showGroupSelection } = useCaseProps;
  const [contacts, setContacts] = useState([blankContact]);
  const [selectedGroups, setSelectedGroups] = useState([]);
  const [groups, setGroups] = useState([]);
  const [credentialsData, setCredentialsData] = useState(recipients);

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

  const createBlankContact = key => ({
    ...blankContact,
    key
  });

  const handleAddNewContact = () => {
    const { key = 0 } = _.last(contacts) || {};

    const newContact = createBlankContact(key + 1);
    const newContactList = contacts.concat(newContact);

    setContacts(newContactList);
  };

  const handleDeleteContact = key => {
    const filteredContacts = contacts.filter(({ key: contactKey }) => key !== contactKey);

    setContacts(filteredContacts);
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
      updateDataSource: setCredentialsData
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
  }).isRequired
};

export default withApi(ManualImportContainer);
