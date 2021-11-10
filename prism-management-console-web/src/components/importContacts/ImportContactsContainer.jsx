import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { omit } from 'lodash';
import { observer } from 'mobx-react-lite';
import ImportDataContainer from '../importContactData/ImportDataContainer';
import { withRedirector } from '../providers/withRedirector';
import { COMMON_CONTACT_HEADERS, IMPORT_CONTACTS } from '../../helpers/constants';
import Logger from '../../helpers/Logger';
import { validateContactsBulk } from '../../helpers/contactValidations';
import { DynamicFormProvider } from '../providers/DynamicFormProvider';
import { useAllContacts } from '../../hooks/useContactStore';
import { useApi } from '../../hooks/useApi';

import './_style.scss';

const ImportContactsContainer = observer(({ redirector: { redirectToContacts } }) => {
  const { t } = useTranslation();
  const { groupsManager, contactsManager } = useApi();

  const { allContacts } = useAllContacts();

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

      const contactsCreated = await contactsManager.createContacts(groupsToAssign, contactsToSend);

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
    const preExistingGroups = await groupsManager.getAllGroups();
    const newGroups = groups.filter(group => !preExistingGroups.map(g => g.name).includes(group));
    const groupCreationPromises = newGroups.map(group => groupsManager.createGroup(group));

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
});

ImportContactsContainer.propTypes = {
  redirector: PropTypes.shape({ redirectToContacts: PropTypes.func }).isRequired
};

export default withRedirector(ImportContactsContainer);
