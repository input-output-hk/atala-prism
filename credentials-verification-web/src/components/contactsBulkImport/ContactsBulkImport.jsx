import React from 'react';
import PropTypes from 'prop-types';
import moment from 'moment';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import { withApi } from '../providers/withApi';
import { withRedirector } from '../providers/withRedirector';
import BulkImport from '../bulkImport/BulkImport';
import Logger from '../../helpers/Logger';
import { validateContactsBulk } from '../../helpers/contactValidations';
import { aoaToObjects } from '../../helpers/fileHelpers';
import './_style.scss';
import { fromMomentToProtoDateFormatter } from '../../helpers/formatters';

const ContactsBulkImport = ({ api, redirector: { redirectToImportContacts } }) => {
  const { t } = useTranslation();

  const handleUpload = (fileData, selectedGroups, setResults) => {
    const { dataObjects, containsErrors, validationErrors } = parseFile(fileData);
    if (containsErrors) setResults({ fileData, validationErrors });
    else handleRequests(dataObjects, selectedGroups, setResults);
  };

  const parseFile = fileData => {
    const inputHeaders = fileData.data[0];
    const dataObjects = aoaToObjects(fileData.data);

    const { containsErrors, validationErrors } = validateContactsBulk(dataObjects, inputHeaders);

    return {
      dataObjects,
      containsErrors,
      validationErrors
    };
  };

  // TODO: replace with bulk request
  const handleRequests = async (contactsData, groups, setResults) => {
    try {
      const groupCreations = await createNewGroups(groups);
      const contactCreations = await createNewContacts(contactsData, groups);

      message.success(t('studentCreation.success'));
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
    const newGroups = groups.filter(group => !preExistingGroups.includes(group));
    const groupCreationPromises = newGroups.map(group => api.groupsManager.createGroup(group));

    return Promise.all(groupCreationPromises);
  };

  const createNewContacts = async (contactsData, groups) => {
    const contactCreationPromises = contactsData
      // .map(contact => api.subjectsManager.createSubject(groups, contact)))
      .map(contact =>
        groups.map(group =>
          api.subjectsManager.createSubject(group, creationDateDecorator(contact))
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
    <div className="BulkImportContainer">
      <BulkImport
        onUpload={handleUpload}
        cancelImport={redirectToImportContacts}
        showGroupSelection
      />
    </div>
  );
};

ContactsBulkImport.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired,
      createGroup: PropTypes.func.isRequired
    }).isRequired,
    subjectsManager: PropTypes.shape({ createSubject: PropTypes.func.isRequired }).isRequired
  }).isRequired,
  redirector: PropTypes.shape({ redirectToImportContacts: PropTypes.func }).isRequired
};

export default withApi(withRedirector(ContactsBulkImport));
