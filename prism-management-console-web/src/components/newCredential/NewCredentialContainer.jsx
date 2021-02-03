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
  MAX_CONTACTS,
  SELECT_CREDENTIAL_TYPE_STEP,
  SELECT_RECIPIENTS_STEP,
  IMPORT_CREDENTIAL_DATA_STEP,
  PREVIEW_AND_SIGN_CREDENTIAL_STEP,
  CONTACT_NAME_KEY,
  EXTERNAL_ID_KEY,
  GROUP_NAME_KEY,
  SUCCESS,
  FAILED
} from '../../helpers/constants';
import Logger from '../../helpers/Logger';
import { contactMapper } from '../../APIs/helpers/contactHelpers';
import ImportCredentialsData from '../importCredentialsData/ImportCredentialsData';
import { filterByManyFields } from '../../helpers/filterHelpers';
import { useSession } from '../providers/SessionContext';
import { fillHTMLCredential } from '../../helpers/credentialView';

const NewCredentialContainer = ({ api, redirector: { redirectToCredentials } }) => {
  const { t } = useTranslation();
  const { session } = useSession();

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

  const [shouldSelectRecipients, setShouldSelectRecipients] = useState(true);

  const [recipients, setRecipients] = useState([]);

  const [importedData, setImportedData] = useState([]);

  const [credentialViewTemplates, setCredentialViewTemplates] = useState([]);
  const [credentialViews, setCredentialViews] = useState([]);

  const handleGetGroups = allGroups => {
    const parsedGroups = allGroups.map(group => ({ ...group, groupName: group.name }));
    setGroups(parsedGroups);
  };

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
      .then(handleGetGroups)
      .catch(error => {
        Logger.error('[NewCredentailContainer.getGroups] Error: ', error);
        message.error(message.error(t('errors.errorGetting', { model: 'groups' })));
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
    setFilteredSubjects(filterByManyFields(subjects, filter, [CONTACT_NAME_KEY, EXTERNAL_ID_KEY]));

  const filterBy = (toFilter, filter, key) =>
    toFilter.filter(({ [key]: name }) => name.toLowerCase().includes(filter.toLowerCase()));

  useEffect(() => {
    filterGroups(groupsFilter);
  }, [groupsFilter, groups]);

  useEffect(() => {
    filterSubjects(subjectsFilter);
  }, [subjectsFilter, subjects]);

  useEffect(() => {
    if (!shouldSelectRecipients) {
      setSelectedGroups([]);
      setSelectedSubjects([]);
    }
  }, [shouldSelectRecipients]);

  const getRecipients = async () => {
    const groupContactsPromises = selectedGroups.map(group =>
      api.contactsManager.getContacts(null, MAX_CONTACTS, group)
    );
    const targetsFromGroups = (await Promise.all(groupContactsPromises)).flat();
    const targetsFromGroupsWithKeys = targetsFromGroups.map(contactMapper);

    const cherryPickedSubjects = subjects.filter(({ contactid }) =>
      selectedSubjects.includes(contactid)
    );

    const targetSubjects = [...targetsFromGroupsWithKeys, ...cherryPickedSubjects];
    const noRepeatedTargets = _.uniqBy(targetSubjects, 'externalid');

    setRecipients(noRepeatedTargets);
  };

  const parseMultiRowCredentials = (dataObjects, multiRowKey, fields) => {
    const groupedData = _.groupBy(dataObjects, 'externalid');
    const externalIds = Object.keys(groupedData);
    const rowsPerExternalId = externalIds.map(externalId => groupedData[externalId]);
    const rowFields = fields.filter(({ isRowField }) => isRowField).map(({ key }) => key);
    return rowsPerExternalId.map(rows => {
      const firstRow = _.head(rows);
      const rowsData = rows.map(row => _.pick(row, ...rowFields));
      return {
        ..._.omit(firstRow, ...rowFields),
        [multiRowKey]: rowsData
      };
    });
  };

  const handleImportedData = ({ credentials }, setResults) => {
    const { isMultiRow, multiRowKey, fields } = credentialTypes[credentialType];
    const credentialsData = isMultiRow
      ? parseMultiRowCredentials(credentials, multiRowKey, fields)
      : credentials;

    setImportedData(credentialsData);

    setResults({
      credentialDataImported: credentialsData.length,
      continueCallback: () => goToCredentialsPreview(credentialsData)
    });
  };

  const goToCredentialsPreview = credentialsData => {
    const { htmltemplate } = credentialViewTemplates.find(
      template => template.id === credentialTypes[credentialType].id
    );
    const htmlCredentials = credentialsData.map(credentialData =>
      fillHTMLCredential(
        htmltemplate,
        credentialTypes[credentialType],
        credentialData,
        session.organisationName
      )
    );
    setCredentialViews(htmlCredentials);
    setCurrentStep(PREVIEW_AND_SIGN_CREDENTIAL_STEP);
  };

  const getContactsFromGroups = () => {
    const groupContactsromises = selectedGroups.map(group =>
      api.contactsManager.getContacts(0, MAX_CONTACTS, group)
    );

    return Promise.all(groupContactsromises);
  };

  const signCredentials = async () => {
    try {
      setIsLoading(true);
      const credentialsData = importedData.map((data, index) => {
        const { contactid } = subjects.find(({ externalid }) => externalid === data.externalid);
        const html = _.escape(credentialViews[index]);
        return Object.assign(_.omit(data, 'originalArray'), { credentialType, contactid, html });
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
      Logger.error(error);
      message.error(t('errors.errorSaving', { model: t('credentials.title') }));
    } finally {
      setIsLoading(false);
    }
  };

  const changeStep = nextStep => {
    if (nextStep !== IMPORT_CREDENTIAL_DATA_STEP) return setCurrentStep(nextStep);

    setIsLoading(true);
    return getRecipients()
      .then(() => setCurrentStep(nextStep))
      .finally(() => setIsLoading(false));
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
            groups={filteredGroups}
            selectedGroups={selectedGroups}
            setSelectedGroups={setSelectedGroups}
            setGroupsFilter={setGroupsFilter}
            subjects={filteredSubjects}
            setSelectedSubjects={setSelectedSubjects}
            selectedSubjects={selectedSubjects}
            setSubjectsFilter={setSubjectsFilter}
            getSubjects={getSubjects}
            toggleShouldSelectRecipients={({ target: { checked } }) =>
              setShouldSelectRecipients(!checked)
            }
            shouldSelectRecipients={shouldSelectRecipients}
          />
        );
      case IMPORT_CREDENTIAL_DATA_STEP: {
        return (
          <ImportCredentialsData
            recipients={recipients}
            contacts={subjects}
            credentialType={credentialTypes[credentialType]}
            onCancel={() => setCurrentStep(currentStep - 1)}
            onFinish={handleImportedData}
            getContactsFromGroups={getContactsFromGroups}
            hasSelectedRecipients={shouldSelectRecipients}
          />
        );
      }
      case PREVIEW_AND_SIGN_CREDENTIAL_STEP:
      default:
        return (
          <CredentialsPreview
            groups={groups.filter(({ name }) => selectedGroups.includes(name))}
            subjects={subjects.filter(({ contactid }) => selectedSubjects.includes(contactid))}
            credentialViews={credentialViews}
          />
        );
    }
  };

  const hasSelectedRecipients =
    !shouldSelectRecipients || selectedGroups.length || selectedSubjects.length;

  return (
    <NewCredential
      currentStep={currentStep}
      changeStep={changeStep}
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
    wallet: PropTypes.shape({ signCredentials: PropTypes.func })
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withApi(withRedirector(NewCredentialContainer));
