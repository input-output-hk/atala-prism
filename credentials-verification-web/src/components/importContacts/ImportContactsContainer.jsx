import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { omit } from 'lodash';
import ImportDataContainer from '../importContactData/ImportDataContainer';
import { withRedirector } from '../providers/withRedirector';
import { COMMON_CONTACT_HEADERS, IMPORT_CONTACTS } from '../../helpers/constants';
import { withApi } from '../providers/withApi';
import Logger from '../../helpers/Logger';
import {
  translateBackSpreadsheetNamesToContactKeys,
  validateContactsBulk
} from '../../helpers/contactValidations';
import { fromMomentToProtoDateFormatter } from '../../helpers/formatters';

import './_style.scss';

const ImportContactsContainer = ({ api, redirector: { redirectToContacts } }) => {
  const { t } = useTranslation();

  const headersMapping = COMMON_CONTACT_HEADERS.map(headerKey => ({
    key: headerKey,
    translation: t(`contacts.table.columns.${headerKey}`)
  }));

  // TODO: replace with bulk request
  const handleRequests = async (contactsData, groups, setResults) => {
    try {
      const translatedContacts = translateBackSpreadsheetNamesToContactKeys(
        contactsData,
        headersMapping
      );

      const groupCreations = await createNewGroups(groups);
      const contactCreations = await createNewContacts(translatedContacts, groups);

      message.success(t('importContacts.success'));
      setResults({
        groupCreations: groupCreations.length,
        contactCreations: contactCreations.length
      });
    } catch (error) {
      Logger.error('Error while creating contact', error);
      message.error('Error while saving the contact');
    }
  };

  const createNewGroups = async groups => {
    const preExistingGroups = await api.groupsManager.getGroups();
    const newGroups = groups.filter(group => !preExistingGroups.map(g => g.name).includes(group));
    const groupCreationPromises = newGroups.map(group => api.groupsManager.createGroup(group));

    return Promise.all(groupCreationPromises);
  };

  const createNewContacts = async (contactsData, groups) => {
    const contactCreationPromises = contactsData
      .map(contact =>
        groups.map(group =>
          api.contactsManager.createContact(
            group,
            creationDateDecorator(omit(contact, ['originalArray'])),
            contact.externalid
          )
        )
      )
      .flat();

    return Promise.all(contactCreationPromises);
  };

  const creationDateDecorator = data => ({
    ...data,
    creationDate: fromMomentToProtoDateFormatter(moment())
  });

  return (
    <ImportDataContainer
      bulkValidator={validateContactsBulk}
      onFinish={handleRequests}
      onCancel={redirectToContacts}
      useCase={IMPORT_CONTACTS}
      headersMapping={headersMapping}
    />
  );
};

ImportContactsContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired,
      createGroup: PropTypes.func.isRequired
    }).isRequired,
    contactsManager: PropTypes.shape({ createContact: PropTypes.func.isRequired }).isRequired
  }).isRequired,
  redirector: PropTypes.shape({ redirectToContacts: PropTypes.func }).isRequired
};

export default withApi(withRedirector(ImportContactsContainer));
