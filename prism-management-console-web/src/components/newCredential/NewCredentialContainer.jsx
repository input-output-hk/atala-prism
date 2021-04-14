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
  GROUP_NAME_KEY,
  SUCCESS,
  FAILED
} from '../../helpers/constants';
import Logger from '../../helpers/Logger';
import { contactMapper } from '../../APIs/helpers/contactHelpers';
import ImportCredentialsData from '../importCredentialsData/ImportCredentialsData';
import { useSession } from '../providers/SessionContext';
import { fillHTMLCredential } from '../../helpers/credentialView';
import { useContactsWithFilteredList } from '../../hooks/useContacts';

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

  const [selectedContacts, setSelectedContacts] = useState([]);
  const {
    contacts,
    filteredContacts,
    filterProps: subjectFilterProps,
    handleContactsRequest,
    hasMore,
    fetchAll,
    isLoading: loadingContacts,
    isSearching: searching
  } = useContactsWithFilteredList(api.contactsManager);

  const [shouldSelectRecipients, setShouldSelectRecipients] = useState(true);
  const [recipients, setRecipients] = useState([]);
  const [importedData, setImportedData] = useState([]);
  const [credentialViewTemplates, setCredentialViewTemplates] = useState([]);
  const [credentialViews, setCredentialViews] = useState([]);

  const credentialTypes = api.credentialsManager.getCredentialTypes();

  useEffect(() => {
    if (!groups.length)
      api.groupsManager
        .getGroups()
        .then(allGroups => {
          const parsedGroups = allGroups.map(group => ({ ...group, groupName: group.name }));
          setGroups(parsedGroups);
        })
        .catch(error => {
          Logger.error('[NewCredentailContainer.getGroups] Error: ', error);
          message.error(message.error(t('errors.errorGetting', { model: 'groups' })));
        });

    if (!credentialViewTemplates.length)
      api.credentialsViewManager
        .getCredentialViewTemplates()
        .then(setCredentialViewTemplates)
        .catch(error => {
          Logger.error('[NewCredentailContainer.getCredentialViewTemplates] Error: ', error);
          message.error(t('errors.errorGettingCredentialViewTemplates'));
        });
  }, [
    credentialViewTemplates.length,
    groups.length,
    api.credentialsViewManager,
    api.groupsManager,
    t
  ]);

  const filterBy = (toFilter, filter, key) =>
    toFilter.filter(({ [key]: name }) => name.toLowerCase().includes(filter.toLowerCase()));

  useEffect(() => {
    const filterGroups = filter => setFilteredGroups(filterBy(groups, filter, GROUP_NAME_KEY));

    filterGroups(groupsFilter);
  }, [groupsFilter, groups]);

  useEffect(() => {
    if (!shouldSelectRecipients) {
      setSelectedGroups([]);
      setSelectedContacts([]);
    }
  }, [shouldSelectRecipients]);

  const getRecipients = async () => {
    const groupContactsPromises = selectedGroups.map(group =>
      api.contactsManager.getContacts(null, MAX_CONTACTS, group)
    );
    const targetsFromGroups = (await Promise.all(groupContactsPromises)).flat();
    const targetsFromGroupsWithKeys = targetsFromGroups.map(contactMapper);

    const cherryPickedSubjects = contacts.filter(({ contactid }) =>
      selectedContacts.includes(contactid)
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

  const signCredentials = () => {
    setIsLoading(true);

    const onFinish = async allContacts => {
      try {
        const credentialsData = importedData.map((data, index) => {
          const { contactid } = allContacts.find(
            ({ externalid }) => externalid === data.externalid
          );
          const html = _.escape(credentialViews[index]);
          return Object.assign(_.omit(data, 'originalArray'), {
            credentialType,
            contactid,
            html,
            issuer: session.organisationName
          });
        });
        const createCredentialsResponse = await api.credentialsManager.createBatchOfCredentials(
          credentialsData
        );
        Logger.debug('Created credentials:', createCredentialsResponse);

        const failedCredentials = createCredentialsResponse.filter(
          ({ status }) => status === FAILED
        );
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

    fetchAll(onFinish);
  };

  const changeStep = nextStep => {
    if (nextStep !== IMPORT_CREDENTIAL_DATA_STEP) return setCurrentStep(nextStep);

    setIsLoading(true);
    return getRecipients()
      .then(() => setCurrentStep(nextStep))
      .finally(() => setIsLoading(false));
  };

  const groupsProps = {
    groups: filteredGroups,
    selectedGroups,
    setSelectedGroups,
    setGroupsFilter
  };

  const contactsProps = {
    contacts: filteredContacts,
    setSelectedContacts,
    selectedContacts,
    setContactsFilter: subjectFilterProps.setSearchText,
    handleContactsRequest,
    hasMore,
    fetchAll,
    loadingContacts,
    searching
  };

  const handleToggleShouldSelectRecipients = ev => {
    const { checked } = ev.target;
    setShouldSelectRecipients(!checked);
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
            groupsProps={groupsProps}
            contactsProps={contactsProps}
            toggleShouldSelectRecipients={handleToggleShouldSelectRecipients}
            shouldSelectRecipients={shouldSelectRecipients}
          />
        );
      case IMPORT_CREDENTIAL_DATA_STEP: {
        return (
          <ImportCredentialsData
            recipients={recipients}
            contacts={contacts}
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
            subjects={contacts.filter(({ contactid }) => selectedContacts.includes(contactid))}
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
