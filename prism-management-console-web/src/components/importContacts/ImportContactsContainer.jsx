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
import { useAllContacts } from '../../hooks/useContacts';
import { DynamicFormProvider } from '../../providers/DynamicFormProvider';

import './_style.scss';

const ImportContactsContainer = ({ api, redirector: { redirectToContacts } }) => {
  const { t } = useTranslation();
  const { allContacts } = useAllContacts(api.contactsManager);

  const [loading, setLoading] = useState(false);

  const handleRequests = async ({ contacts, groups }, setResults) => {
    setLoading(true);
    try {
      const groupsToAssign = await createMissingGroups(groups);
      const contactsToSend = contacts.map(({ contactName, externalId, ...jsonData }) => ({
        contactName,
        externalId,
        jsonData: omit(jsonData, ['errorFields', 'key'])
      }));

      const contactsCreated = await api.contactsManager.createContacts(
        groupsToAssign,
        contactsToSend
      );

      message.success(t('importContacts.success'));
      setResults({
        contactCreations: contactsCreated
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
    const preExistingGroups = await api.groupsManager.getAllGroups();
    const newGroups = groups.filter(group => !preExistingGroups.map(g => g.name).includes(group));
    const groupCreationPromises = newGroups.map(group => api.groupsManager.createGroup(group));

    const createdGroups = await Promise.all(groupCreationPromises);
    const updatedGroupList = preExistingGroups.concat(createdGroups);
    return updatedGroupList.filter(g => groups.includes(g.name));
  };

  const headersMapping = COMMON_CONTACT_HEADERS.map(headerKey => ({
    key: headerKey,
    translation: t(`contacts.table.columns.${headerKey}`)
  }));

  return (
    <DynamicFormProvider formName={IMPORT_CONTACTS}>
      <ImportDataContainer
        bulkValidator={args => validateContactsBulk({ ...args, preExistingContacts: allContacts })}
        onFinish={handleRequests}
        onCancel={redirectToContacts}
        headersMapping={headersMapping}
        useCase={IMPORT_CONTACTS}
        loading={loading}
        continueCallback={redirectToContacts}
      />
    </DynamicFormProvider>
  );
};

ImportContactsContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getAllGroups: PropTypes.func.isRequired,
      createGroup: PropTypes.func.isRequired,
      updateGroup: PropTypes.func.isRequired
    }).isRequired,
    contactsManager: PropTypes.shape({ createContacts: PropTypes.func.isRequired }).isRequired
  }).isRequired,
  redirector: PropTypes.shape({ redirectToContacts: PropTypes.func }).isRequired
};

export default withApi(withRedirector(ImportContactsContainer));
