import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { withApi } from '../providers/withApi';
import { IMPORT_CONTACTS, IMPORT_CREDENTIALS_DATA } from '../../helpers/constants';
import Logger from '../../helpers/Logger';
import ManualImport from './ManualImport';
import UnderContsructionMessage from '../common/Atoms/UnderContsructionMessage/UnderContsructionMessage';

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
  useCaseProps
}) => {
  const { t } = useTranslation();
  const { useCase, showGroupSelection } = useCaseProps;

  const [contacts, setContacts] = useState([blankContact]);
  const [selectedGroups, setSelectedGroups] = useState([]);
  const [groups, setGroups] = useState([]);

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
    contacts,
    updateDataSource: setContacts,
    deleteContact: handleDeleteContact,
    addNewContact: handleAddNewContact
  };

  const groupsProps = { groups, selectedGroups, setSelectedGroups };

  return useCase === IMPORT_CONTACTS ? (
    <ManualImport
      tableProps={tableProps}
      groupsProps={groupsProps}
      onSave={() => onSave(contacts, selectedGroups)}
      cancelImport={cancelImport}
      loading={loading}
      {...useCaseProps}
    />
  ) : (
    <UnderContsructionMessage goBack={cancelImport} />
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
