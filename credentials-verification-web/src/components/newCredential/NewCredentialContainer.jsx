import React, { useState, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import NewCredential from './NewCredential';
import { withApi } from '../providers/withApi';
import TypeSelection from './Organism/TypeSelection/TypeSelection';
import RecipientsSelection from './Organism/RecipientsSelection/RecipientsSelection';
import { withRedirector } from '../providers/withRedirector';
import {
  HOLDER_PAGE_SIZE,
  MAX_CONTACTS,
  SELECT_CREDENTIAL_TYPE_STEP,
  SELECT_RECIPIENTS_STEP,
  IMPORT_CREDENTIAL_DATA_STEP,
  PREVIEW_AND_SIGN_CREDENTIAL_STEP,
  CONTACT_NAME_KEY,
  GROUP_NAME_KEY
} from '../../helpers/constants';
import { getLastArrayElementOrEmpty } from '../../helpers/genericHelpers';
import Logger from '../../helpers/Logger';
import { contactMapper } from '../../APIs/helpers';
import ImportCredentialsData from '../importCredentialsData/ImportCredentialsData';
import UnderContsructionMessage from '../common/Atoms/UnderContsructionMessage/UnderContsructionMessage';

const NewCredentialContainer = ({ api, redirector: { redirectToCredentials } }) => {
  const { t } = useTranslation();

  const [currentStep, setCurrentStep] = useState(0);

  const [credentialType, setCredentialType] = useState();

  const [groups, setGroups] = useState([]);
  const [selectedGroups, setSelectedGroups] = useState([]);
  const [groupsFilter, setGroupsFilter] = useState('');
  const [filteredGroups, setFilteredGroups] = useState([]);

  const [subjects, setSubjects] = useState([]);
  const [selectedSubjects, setSelectedSubjects] = useState([]);
  const [subjectsFilter, setSubjectsFilter] = useState('');
  const [filteredSubjects, setFilteredSubjects] = useState([]);
  const [hasMoreSubjects, setHasMoreSubjects] = useState(true);

  const [importedData, setImportedData] = useState([]);

  const credentialTypes = api.credentialsManager.getCredentialTypes();

  const getGroups = () =>
    api.groupsManager
      .getGroups()
      .then(allGroups => setGroups(allGroups))
      .catch(error => {
        Logger.error('[NewCredentailContainer.getGroups] Error: ', error);
        message.error(t('errors.errorGettingHolders'));
      });

  const getSubjects = () => {
    const { contactid: lastId } = getLastArrayElementOrEmpty(subjects);

    return api.contactsManager
      .getContacts(lastId, HOLDER_PAGE_SIZE)
      .then(connections => {
        if (connections.length < HOLDER_PAGE_SIZE) setHasMoreSubjects(false);

        const subjectsWithKey = connections.map(contactMapper);

        const updatedSubjects = subjects.concat(subjectsWithKey);
        setSubjects(updatedSubjects);
      })
      .catch(error => {
        Logger.error('[NewCredentailContainer.getSubjects] Error while getting connections', error);
        message.error(t('errors.errorGetting', { model: 'Holders' }));
      });
  };

  useEffect(() => {
    if (!groups.length) getGroups();
    if (!subjects.length) getSubjects();
  }, []);

  const filterGroups = filter => setFilteredGroups(filterBy(groups, filter, GROUP_NAME_KEY));

  const filterSubjects = filter =>
    setFilteredSubjects(filterBy(subjects, filter, CONTACT_NAME_KEY));

  const filterBy = (toFilter, filter, key) =>
    toFilter.filter(({ [key]: name }) => name.toLowerCase().includes(filter.toLowerCase()));

  useEffect(() => {
    filterGroups(groupsFilter);
  }, [groupsFilter, groups]);

  useEffect(() => {
    filterSubjects(subjectsFilter);
  }, [subjectsFilter, subjects]);

  useEffect(() => {
    if (hasMoreSubjects && filteredSubjects.length < HOLDER_PAGE_SIZE) getSubjects();
  }, [filteredSubjects]);

  const handleImportedData = (dataObjects, _groups, setResults) => {
    setImportedData(dataObjects);
    setResults({
      credentialDataImported: dataObjects.length,
      continueCallback: () => goToCredentialsPreview(dataObjects)
    });
  };

  const goToCredentialsPreview = importedCredentials => {
    message.warn(t('newCredential.messages.noFurtherSteps'));
    Logger.debug(importedCredentials);
    setCurrentStep(PREVIEW_AND_SIGN_CREDENTIAL_STEP);
  };

  const getContactsFromGroups = () => {
    const groupContactsromises = selectedGroups.map(group =>
      api.contactsManager.getContacts(0, MAX_CONTACTS, group)
    );

    return Promise.all(groupContactsromises);
  };

  const renderStep = () => {
    switch (currentStep) {
      case SELECT_CREDENTIAL_TYPE_STEP:
        return (
          <TypeSelection
            credentialTypes={credentialTypes}
            onTypeSelection={setCredentialType}
            selectedType={credentialType}
          />
        );
      case SELECT_RECIPIENTS_STEP:
        return (
          <RecipientsSelection
            isIssuer={() => api.wallet.isIssuer()}
            groups={filteredGroups}
            selectedGroups={selectedGroups}
            setSelectedGroups={setSelectedGroups}
            setGroupsFilter={setGroupsFilter}
            subjects={filteredSubjects}
            setSelectedSubjects={setSelectedSubjects}
            selectedSubjects={selectedSubjects}
            setSubjectsFilter={setSubjectsFilter}
            getSubjects={getSubjects}
            hasMoreSubjects={hasMoreSubjects}
          />
        );
      case IMPORT_CREDENTIAL_DATA_STEP: {
        return (
          <ImportCredentialsData
            subjects={subjects}
            credentialType={credentialTypes[credentialType]}
            selectedGroups={selectedGroups}
            selectedSubjects={selectedSubjects}
            onCancel={() => setCurrentStep(currentStep - 1)}
            onFinish={handleImportedData}
            getContactsFromGroups={getContactsFromGroups}
            goToCredentialsPreview={goToCredentialsPreview}
          />
        );
      }
      case PREVIEW_AND_SIGN_CREDENTIAL_STEP:
        // TODO: Implement credential visualisation + signing
        return <UnderContsructionMessage goBack={redirectToCredentials} />;
      default:
        // TODO: Implement credential visualisation + signing
        return <UnderContsructionMessage goBack={redirectToCredentials} />;
    }
  };

  const hasSelectedRecipients = selectedGroups.length || selectedSubjects.length;

  return (
    <NewCredential
      currentStep={currentStep}
      changeStep={setCurrentStep}
      renderStep={renderStep}
      credentialType={credentialType}
      hasSelectedRecipients={hasSelectedRecipients}
    />
  );
};

NewCredentialContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({ getGroups: PropTypes.func }),
    credentialsManager: PropTypes.shape({
      getCredentialTypes: PropTypes.func,
      createCredential: PropTypes.func
    }).isRequired,
    contactsManager: PropTypes.shape({
      getContacts: PropTypes.func
    }),
    wallet: PropTypes.shape({ isIssuer: PropTypes.func })
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withApi(withRedirector(NewCredentialContainer));
