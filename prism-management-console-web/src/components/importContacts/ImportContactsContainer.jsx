import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { omit } from 'lodash';
import ImportDataContainer from '../importContactData/ImportDataContainer';
import { withRedirector } from '../providers/withRedirector';
import { COMMON_CONTACT_HEADERS, IMPORT_CONTACTS } from '../../helpers/constants';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import { validateContactsBulk } from '../../helpers/contactValidations';

import './_style.scss';

const ImportContactsContainer = ({ api, redirector: { redirectToContacts } }) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);

  // TODO: replace with bulk request
  const handleRequests = async ({ contacts, groups }, setResults) => {
    setLoading(true);
    try {
      const groupsToAssign = await createMissingGroups(groups);

      const contactsCreated = await createContacts(contacts, groupsToAssign);

      message.success(t('importContacts.success'));
      setResults({
        contactCreations: contactsCreated.length
      });
    } catch (error) {
      Logger.error('Error while creating contact', error);
      message.error('Error while saving the contact');
    } finally {
      setLoading(false);
    }
  };

  const createMissingGroups = async groups => {
    if (!groups.length) return [];
    const preExistingGroups = await api.groupsManager.getGroups();
    const newGroups = groups.filter(group => !preExistingGroups.map(g => g.name).includes(group));
    const groupCreationPromises = newGroups.map(group => api.groupsManager.createGroup(group));

    const createdGroups = await Promise.all(groupCreationPromises);
    const updatedGroupList = preExistingGroups.concat(createdGroups);
    return updatedGroupList.filter(g => groups.includes(g.name));
  };

  const createContacts = async (contacts, groups) => {
    const [firstGroup, ...otherGroups] = groups;
    const contactCreationPromises = contacts.map(contact => {
      const contactToSend = omit(contact, ['originalArray', 'errors', 'key', 'externalid']);
      return api.contactsManager.createContact(firstGroup?.name, contactToSend, contact.externalid);
    });
    const contactsCreated = await Promise.all(contactCreationPromises);
    await assignToGroups(contactsCreated, otherGroups);
    return contactsCreated;
  };

  const assignToGroups = async (contacts, groups) => {
    const contactIds = contacts.map(c => c.contactid);

    const updateGroupsPromises = groups.map(group =>
      api.groupsManager.updateGroup(group.id, contactIds)
    );

    await Promise.all(updateGroupsPromises);
  };

  const headersMapping = COMMON_CONTACT_HEADERS.map(headerKey => ({
    key: headerKey,
    translation: t(`contacts.table.columns.${headerKey}`)
  }));

  return (
    <ImportDataContainer
      bulkValidator={validateContactsBulk}
      onFinish={handleRequests}
      onCancel={redirectToContacts}
      headersMapping={headersMapping}
      useCase={IMPORT_CONTACTS}
      loading={loading}
    />
  );
};

ImportContactsContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired,
      createGroup: PropTypes.func.isRequired,
      updateGroup: PropTypes.func.isRequired
    }).isRequired,
    contactsManager: PropTypes.shape({ createContact: PropTypes.func.isRequired }).isRequired
  }).isRequired,
  redirector: PropTypes.shape({ redirectToContacts: PropTypes.func }).isRequired
};

export default withApi(withRedirector(ImportContactsContainer));
