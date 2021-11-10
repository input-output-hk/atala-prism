import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import _ from 'lodash';
import NewCredential from './NewCredential';
import TypeSelection from './Organism/TypeSelection/TypeSelection';
import RecipientsSelection from './Organism/RecipientsSelection/RecipientsSelection';
import CredentialsPreview from './Organism/CredentialsPreview/CredentialsPreview';
import { withRedirector } from '../providers/withRedirector';
import {
  SELECT_CREDENTIAL_TYPE_STEP,
  SELECT_RECIPIENTS_STEP,
  IMPORT_CREDENTIAL_DATA_STEP,
  PREVIEW_AND_SIGN_CREDENTIAL_STEP
} from '../../helpers/constants';
import Logger from '../../helpers/Logger';
import { contactMapper } from '../../APIs/helpers/contactHelpers';
import ImportCredentialsData from '../importCredentialsData/ImportCredentialsData';
import { useSession } from '../../hooks/useSession';
import { fillHTMLCredential } from '../../helpers/credentialView';
import { useTemplateStore } from '../../hooks/useTemplateStore';
import { useGroupStore, useGroupUiState } from '../../hooks/useGroupStore';
import { useContactStore, useContactUiState } from '../../hooks/useContactStore';
import { useApi } from '../../hooks/useApi';

const NewCredentialContainer = observer(({ redirector: { redirectToCredentials } }) => {
  const { t } = useTranslation();
  const { groupsManager, contactsManager, credentialsManager, credentialsViewManager } = useApi();
  const { session } = useSession();

  const [currentStep, setCurrentStep] = useState(SELECT_CREDENTIAL_TYPE_STEP);
  const [isLoading, setIsLoading] = useState(false);

  const [selectedCredentialTypeId, setSelectedCredentialTypeId] = useState();
  const [credentialTypeDetails, setCredentialTypeDetails] = useState();
  const resetCredentialTypeDetails = useCallback(() => setCredentialTypeDetails(), []);

  const [selectedGroups, setSelectedGroups] = useState([]);

  const { groups } = useGroupStore({ fetch: true, reset: true });
  useGroupUiState({ reset: true });

  const { contacts } = useContactStore({ fetch: true, reset: true });
  useContactUiState({ reset: true });

  const [selectedContacts, setSelectedContacts] = useState([]);

  const {
    getCredentialTemplateDetails: getCredentialTypeDetails,
    templateCategories
  } = useTemplateStore({ fetch: true });

  const [shouldSelectRecipients, setShouldSelectRecipients] = useState(true);
  const [recipients, setRecipients] = useState([]);
  const [importedData, setImportedData] = useState([]);
  const [credentialViewTemplates, setCredentialViewTemplates] = useState([]);
  const [credentialViews, setCredentialViews] = useState([]);

  useEffect(() => {
    if (!credentialViewTemplates.length)
      credentialsViewManager
        .getCredentialViewTemplates()
        .then(setCredentialViewTemplates)
        .catch(error => {
          Logger.error('[NewCredentailContainer.getCredentialViewTemplates] Error: ', error);
          message.error(t('errors.errorGettingCredentialViewTemplates'));
        });
  }, [credentialViewTemplates.length, groups.length, credentialsViewManager, groupsManager, t]);

  useEffect(() => {
    if (!credentialTypeDetails && currentStep === SELECT_RECIPIENTS_STEP) {
      getCredentialTypeDetails(selectedCredentialTypeId)
        .then(setCredentialTypeDetails)
        .catch(error => {
          Logger.error('[NewCredentailContainer.getCredentialTypeDetails] Error: ', error);
          message.error(t('errors.errorGettingCredentialTypeDetails'));
        });
    }
  }, [currentStep, credentialTypeDetails, selectedCredentialTypeId, getCredentialTypeDetails, t]);

  useEffect(() => {
    if (!shouldSelectRecipients) {
      setSelectedGroups([]);
      setSelectedContacts([]);
    }
  }, [shouldSelectRecipients]);

  const getRecipients = async () => {
    const groupContactsPromises = selectedGroups.map(group =>
      contactsManager.getAllContacts(group)
    );

    const allContacts = await contactsManager.getAllContacts();
    const promisesList = await Promise.all(groupContactsPromises);

    const targetsFromGroups = promisesList.flat();
    const targetsFromGroupsWithKeys = targetsFromGroups.map(contactMapper);
    const cherryPickedSubjects = allContacts
      .filter(({ contactId }) => selectedContacts.includes(contactId))
      .map(contactMapper);

    const targetSubjects = [...targetsFromGroupsWithKeys, ...cherryPickedSubjects];
    const noRepeatedTargets = _.uniqBy(targetSubjects, 'externalId');

    setRecipients(noRepeatedTargets);
  };

  const parseMultiRowCredentials = (dataObjects, multiRowKey, fields) => {
    const groupedData = _.groupBy(dataObjects, 'externalId');
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
    const { isMultiRow, multiRowKey, fields } = credentialTypeDetails;
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
    const { template } = credentialTypeDetails;
    const htmlCredentials = credentialsData.map(credentialData =>
      fillHTMLCredential(template, credentialTypeDetails, credentialData, session.organisationName)
    );
    setCredentialViews(htmlCredentials);
    setCurrentStep(PREVIEW_AND_SIGN_CREDENTIAL_STEP);
  };

  const getContactsFromGroups = () => {
    const groupContactsPromises = selectedGroups.map(group =>
      contactsManager.getAllContacts(group)
    );

    return Promise.all(groupContactsPromises);
  };

  const signCredentials = async () => {
    setIsLoading(true);

    const onFinish = async allContacts => {
      try {
        const credentialsData = importedData.map((data, index) => {
          const { contactId } = allContacts.find(
            ({ externalId }) => externalId === data.externalId
          );
          const html = credentialViews[index];
          return Object.assign(_.omit(data, 'originalArray'), {
            credentialTypeDetails,
            contactId,
            html,
            issuer: session.organisationName
          });
        });
        const createCredentialsResponse = await credentialsManager.createBatchOfCredentials(
          credentialsData,
          credentialTypeDetails,
          selectedGroups.map(sg => groups.find(g => g.name === sg))
        );
        Logger.debug('Created credentials:', createCredentialsResponse);
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

    return contactsManager.getAllContacts().then(onFinish);
  };

  const changeStep = nextStep => {
    if (nextStep !== IMPORT_CREDENTIAL_DATA_STEP) return setCurrentStep(nextStep);

    setIsLoading(true);
    return getRecipients()
      .then(() => setCurrentStep(nextStep))
      .finally(() => setIsLoading(false));
  };

  const handleToggleShouldSelectRecipients = ev => {
    const { checked } = ev.target;
    setShouldSelectRecipients(!checked);
  };

  const handleTypeSelection = id => {
    resetCredentialTypeDetails();
    setSelectedCredentialTypeId(id);
  };

  const renderStep = () => {
    switch (currentStep) {
      case SELECT_CREDENTIAL_TYPE_STEP:
        return (
          <TypeSelection
            templateCategories={templateCategories}
            onTypeSelection={handleTypeSelection}
            selectedType={selectedCredentialTypeId}
          />
        );
      case SELECT_RECIPIENTS_STEP:
        return (
          <RecipientsSelection
            selectedGroups={selectedGroups}
            setSelectedGroups={setSelectedGroups}
            selectedContacts={selectedContacts}
            setSelectedContacts={setSelectedContacts}
            toggleShouldSelectRecipients={handleToggleShouldSelectRecipients}
            shouldSelectRecipients={shouldSelectRecipients}
          />
        );
      case IMPORT_CREDENTIAL_DATA_STEP: {
        return (
          <ImportCredentialsData
            recipients={recipients}
            contacts={contacts}
            credentialType={credentialTypeDetails}
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
            groups={groups.filter(({ name: groupName }) => selectedGroups.includes(groupName))}
            subjects={contacts.filter(({ contactId }) => selectedContacts.includes(contactId))}
            credentialViews={credentialViews}
          />
        );
    }
  };

  const hasSelectedRecipients =
    !shouldSelectRecipients || selectedGroups.length || selectedContacts.length;

  return (
    <NewCredential
      currentStep={currentStep}
      changeStep={changeStep}
      renderStep={renderStep}
      selectedCredentialTypeId={selectedCredentialTypeId}
      hasSelectedRecipients={hasSelectedRecipients}
      onSuccess={signCredentials}
      isLoading={isLoading}
    />
  );
});

NewCredentialContainer.propTypes = {
  redirector: PropTypes.shape({
    redirectToCredentials: PropTypes.func
  }).isRequired
};

export default withRedirector(NewCredentialContainer);
