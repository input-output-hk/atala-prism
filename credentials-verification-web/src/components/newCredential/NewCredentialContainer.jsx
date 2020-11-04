import React, { useState, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import _ from 'lodash';
import NewCredential from './NewCredential';
import { withApi } from '../providers/withApi';
import TypeSelection from './Organism/TypeSelection/TypeSelection';
import RecipientsSelection from './Organism/RecipientsSelection/RecipientsSelection';
import CredentialsPreview from './Organism/CredentialsPreview/CredentialsPreview';
import { withRedirector } from '../providers/withRedirector';
import {
  HOLDER_PAGE_SIZE,
  MAX_CONTACTS,
  SELECT_CREDENTIAL_TYPE_STEP,
  SELECT_RECIPIENTS_STEP,
  IMPORT_CREDENTIAL_DATA_STEP,
  PREVIEW_AND_SIGN_CREDENTIAL_STEP,
  CONTACT_NAME_KEY,
  GROUP_NAME_KEY,
  SUCCESS,
  FAILED,
  CONNECTION_STATUSES
} from '../../helpers/constants';
import Logger from '../../helpers/Logger';
import { contactMapper } from '../../APIs/helpers';
import ImportCredentialsData from '../importCredentialsData/ImportCredentialsData';

const NewCredentialContainer = ({ api, redirector: { redirectToCredentials } }) => {
  const { t } = useTranslation();

  const [currentStep, setCurrentStep] = useState(SELECT_CREDENTIAL_TYPE_STEP);
  const [isLoading, setIsLoading] = useState(false);

  const [credentialType, setCredentialType] = useState();

  const [groups, setGroups] = useState([]);
  const [selectedGroups, setSelectedGroups] = useState([]);
  const [groupsFilter, setGroupsFilter] = useState('');
  const [filteredGroups, setFilteredGroups] = useState([]);

  const [subjects, setSubjects] = useState([]);
  const [selectedSubjects, setSelectedSubjects] = useState([]);
  const [subjectsFilter, setSubjectsFilter] = useState('');
  const [filteredSubjects, setFilteredSubjects] = useState([]);

  const [importedData, setImportedData] = useState([]);

  const [credentialViewTemplates, setCredentialViewTemplates] = useState([]);

  const getCredentialViewTemplates = () =>
    api.credentialsViewManager
      .getCredentialViewTemplates()
      .then(setCredentialViewTemplates)
      .catch(error => {
        Logger.error('[NewCredentailContainer.getCredentialViewTemplates] Error: ', error);
        message.error(t('errors.errorGettingCredentialViewTemplates'));
      });

  const credentialTypes = api.credentialsManager.getCredentialTypes();

  const getGroups = () =>
    api.groupsManager
      .getGroups()
      .then(allGroups => setGroups(allGroups))
      .catch(error => {
        Logger.error('[NewCredentailContainer.getGroups] Error: ', error);
        message.error(t('errors.errorGettingHolders'));
      });

  const getSubjects = async () => {
    try {
      const allContacts = await api.contactsManager.getContacts(null, MAX_CONTACTS);
      const allContactsMapped = allContacts.map(contactMapper);

      setSubjects(allContactsMapped);
    } catch (error) {
      Logger.error('[NewCredentailContainer.getSubjects] Error while getting connections', error);
      message.error(t('errors.errorGetting', { model: 'Holders' }));
    }
  };

  useEffect(() => {
    if (!groups.length) getGroups();
    if (!subjects.length) getSubjects();
    if (!credentialViewTemplates.length) getCredentialViewTemplates();
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

  const handleImportedData = (dataObjects, _groups, setResults) => {
    setImportedData(dataObjects);
    setResults({
      credentialDataImported: dataObjects.length,
      continueCallback: () => goToCredentialsPreview()
    });
  };

  const goToCredentialsPreview = () => setCurrentStep(PREVIEW_AND_SIGN_CREDENTIAL_STEP);

  const getContactsFromGroups = () => {
    const groupContactsromises = selectedGroups.map(group =>
      api.contactsManager.getContacts(0, MAX_CONTACTS, group)
    );

    return Promise.all(groupContactsromises);
  };

  const signCredentials = async () => {
    try {
      setIsLoading(true);
      const credentialsData = importedData.map(data => {
        const { contactid } = subjects.find(({ externalid }) => externalid === data.externalId);
        return Object.assign(_.omit(data, 'originalArray'), { credentialType, contactid });
      });
      const createCredentialsResponse = await api.credentialsManager.createBatchOfCredentials(
        credentialsData
      );
      Logger.debug('Created credentials:', createCredentialsResponse);

      const failedCredentials = createCredentialsResponse.filter(({ status }) => status === FAILED);
      if (failedCredentials.length)
        message.error(
          t('newCredential.messages.creationError', { amount: failedCredentials.length })
        );

      const credentials = createCredentialsResponse
        .filter(({ status }) => status === SUCCESS)
        .map(({ response }) => response.getGenericcredential().toObject());

      await api.wallet.signCredentials(credentials);

      Logger.info('Successfully created the credential(s)');
      message.success(
        t('newCredential.messages.creationSuccess', { amount: credentialsData.length })
      );
      redirectToCredentials();
    } catch (error) {
      setIsLoading(false);
      Logger.error(error);
      message.error(t('errors.errorSaving', { model: t('credentials.title') }));
    }
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
      default:
        return (
          <CredentialsPreview
            credentialsData={importedData}
            groups={groups.filter(({ name }) => selectedGroups.includes(name))}
            subjects={subjects.filter(({ contactid }) => selectedSubjects.includes(contactid))}
            credentialViewTemplate={credentialViewTemplates.find(
              template => template.id === credentialTypes[credentialType].id
            )}
            credentialPlaceholders={credentialTypes[credentialType].placeholders}
          />
        );
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
      onSuccess={signCredentials}
      isLoading={isLoading}
    />
  );
};

NewCredentialContainer.propTypes = {
  api: PropTypes.shape({
    groupsManager: PropTypes.shape({ getGroups: PropTypes.func }),
    credentialsManager: PropTypes.shape({
      getCredentialTypes: PropTypes.func,
      createBatchOfCredentials: PropTypes.func
    }).isRequired,
    credentialsViewManager: PropTypes.shape({ getCredentialViewTemplates: PropTypes.func })
      .isRequired,
    contactsManager: PropTypes.shape({
      getContacts: PropTypes.func
    }),
    wallet: PropTypes.shape({ isIssuer: PropTypes.func, signCredentials: PropTypes.func })
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withApi(withRedirector(NewCredentialContainer));
