import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import Credentials from './Credentials';
import CredentialActionConfirmationModal from './Molecules/Modals/CredentialActionConfirmationModal';
import { withApi } from '../providers/withApi';
import { credentialMapper } from '../../APIs/helpers/credentialHelpers';
import {
  MAX_CREDENTIALS,
  CREDENTIALS_ISSUED,
  CREDENTIALS_RECEIVED,
  UNKNOWN_DID_SUFFIX_ERROR_CODE,
  DEFAULT_CREDENTIAL_VERIFICATION_RESULT
} from '../../helpers/constants';
import {
  useCredentialsIssuedListWithFilters,
  useCredentialsReceivedListWithFilters
} from '../../hooks/useCredentials';
import { useSession } from '../providers/SessionContext';
import { getTargetCredentials } from '../../helpers/credentialActions';
import { useCredentialActions } from '../../hooks/useCredentialActions';

const CredentialContainer = ({ api }) => {
  const { t } = useTranslation();
  // This field is used to know if there are no credentials on
  // the database, independently of the filters

  const [loading, setLoading] = useState({ issued: true, received: true });
  const [searching, setSearching] = useState({ issued: false, received: false });

  const [selectAll, setSelectAll] = useState(null);
  const [indeterminateSelectAll, setIndeterminateSelectAll] = useState(false);
  const [loadingSelection, setLoadingSelection] = useState(false);
  const [activeTab, setActiveTab] = useState(CREDENTIALS_ISSUED);

  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const {
    credentialsIssued,
    fetchCredentialsIssued,
    setCredentialsIssued,
    filteredCredentialsIssued,
    filtersIssued,
    hasMoreIssued,
    noIssuedCredentials
  } = useCredentialsIssuedListWithFilters(api.credentialsManager, setLoading, setSearching);

  const {
    fetchCredentialsReceived,
    filteredCredentialsReceived,
    filtersReceived,
    noReceivedCredentials
  } = useCredentialsReceivedListWithFilters(api, setLoading);

  const setLoadingByKey = (key, value) =>
    setLoading(previousLoading => ({ ...previousLoading, [key]: value }));

  const refreshCredentialsIssued = async () => {
    try {
      setLoadingByKey('issued', true);
      const refreshedCredentials = await api.credentialsManager.getCredentials(
        credentialsIssued.length,
        null
      );

      const credentialTypes = api.credentialsManager.getCredentialTypes();
      const mappedCredentials = refreshedCredentials.map(cred =>
        credentialMapper(cred, credentialTypes)
      );

      setCredentialsIssued(mappedCredentials);
      removeUnconfirmedAccountError();
    } catch (error) {
      Logger.error(
        '[CredentialContainer.refreshCredentialsIssued] Error while getting Credentials',
        error
      );
      if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
        showUnconfirmedAccountError();
      } else {
        removeUnconfirmedAccountError();
        message.error(t('errors.errorGetting', { model: 'Credentials' }));
      }
    } finally {
      setLoadingByKey('issued', false);
    }
  };

  const {
    revokeSingleCredential,
    signSingleCredential,
    sendSingleCredential,
    revokeSelectedCredentials,
    signSelectedCredentials,
    sendSelectedCredentials,
    selectedCredentials,
    setSelectedCredentials,
    actions,
    confirmationModalProps
  } = useCredentialActions(api, credentialsIssued, refreshCredentialsIssued);

  useEffect(() => {
    if (activeTab === CREDENTIALS_RECEIVED) fetchCredentialsReceived();
  }, [activeTab]);

  const getAllCredentialsIssued = async () => {
    const allCredentials = await api.credentialsManager.getCredentials(MAX_CREDENTIALS, null);
    const credentialTypes = api.credentialsManager.getCredentialTypes();
    const mappedCredentials = allCredentials.map(cred => credentialMapper(cred, credentialTypes));
    setCredentialsIssued(mappedCredentials);
    return mappedCredentials;
  };

  const toggleSelectAll = async ev => {
    setLoadingSelection(true);
    const { checked } = ev.target;
    setIndeterminateSelectAll(false);
    setSelectAll(checked);
    setSelectedCredentials(checked ? credentialsIssued.map(c => c.credentialid) : []);
    if (checked && hasMoreIssued) {
      const allCredentials = await getAllCredentialsIssued();
      setSelectedCredentials(allCredentials.map(c => c.credentialid));
    }
    setLoadingSelection(false);
  };

  const handleSelectionChange = selectedRowKeys => {
    setSelectedCredentials(selectedRowKeys);
    setIndeterminateSelectAll(
      !!selectedRowKeys.length && selectedRowKeys.length < credentialsIssued.length
    );
    setSelectAll(!hasMoreIssued && selectedRowKeys.length === credentialsIssued.length);
  };

  const verifyCredential = ({ encodedsignedcredential, batchinclusionproof }) =>
    api.wallet.verifyCredential(encodedsignedcredential, batchinclusionproof).catch(error => {
      Logger.error('There has been an error verifiying the credential', error);
      const pendingPublication = error.message.includes('Missing publication date');
      message.error(
        t(`credentials.errors.${pendingPublication ? 'pendingPublication' : 'errorVerifying'}`)
      );
      return DEFAULT_CREDENTIAL_VERIFICATION_RESULT;
    });

  const tabProps = {
    [CREDENTIALS_ISSUED]: {
      tableProps: {
        credentials: filteredCredentialsIssued,
        hasMore: hasMoreIssued,
        searching: searching.issued,
        revokeSingleCredential,
        signSingleCredential,
        sendSingleCredential,
        selectionType: {
          selectedRowKeys: selectedCredentials,
          type: 'checkbox',
          onChange: handleSelectionChange
        }
      },
      fetchCredentials: fetchCredentialsIssued,
      bulkActionsProps: {
        refreshCredentials: refreshCredentialsIssued,
        revokeSelectedCredentials,
        signSelectedCredentials,
        sendSelectedCredentials,
        selectAll,
        indeterminateSelectAll,
        toggleSelectAll
      },
      filterProps: filtersIssued,
      showEmpty: noIssuedCredentials,
      credentialsTypes: api.credentialsManager.getCredentialTypes(),
      loadingSelection
    },
    [CREDENTIALS_RECEIVED]: {
      tableProps: {
        credentials: filteredCredentialsReceived
      },
      fetchCredentials: fetchCredentialsReceived,
      bulkActionsProps: {},
      filterProps: filtersReceived,
      showEmpty: noReceivedCredentials,
      credentialsTypes: api.credentialsManager.getCredentialTypes()
    }
  };

  const renderModal = () => {
    const credentialsRequiredStatus = actions[confirmationModalProps.type]?.requiredStatus;
    const targetCredentialsProps = getTargetCredentials(
      credentialsIssued,
      selectedCredentials,
      credentialsRequiredStatus
    );
    return (
      <CredentialActionConfirmationModal {...confirmationModalProps} {...targetCredentialsProps} />
    );
  };

  return (
    <>
      {confirmationModalProps.type && renderModal()}
      <Credentials
        tabProps={tabProps}
        setActiveTab={setActiveTab}
        loading={loading}
        verifyCredential={verifyCredential}
      />
    </>
  );
};

CredentialContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ getContact: PropTypes.func.isRequired }).isRequired,
    credentialsManager: PropTypes.shape({
      getCredentialBinary: PropTypes.func.isRequired,
      getCredentials: PropTypes.func.isRequired,
      getCredentialTypes: PropTypes.func.isRequired,
      markAsSent: PropTypes.func.isRequired,
      getBlockchainData: PropTypes.func.isRequired
    }).isRequired,
    credentialsReceivedManager: PropTypes.shape({
      getReceivedCredentials: PropTypes.func.isRequired
    }),
    wallet: PropTypes.shape({
      signCredentials: PropTypes.func.isRequired,
      verifyCredential: PropTypes.func.isRequired,
      revokeCredentials: PropTypes.func.isRequired
    }).isRequired,
    connector: PropTypes.shape({
      sendCredential: PropTypes.func.isRequired
    }).isRequired,
    getCredentialTypes: PropTypes.func.isRequired,
    getCategoryTypes: PropTypes.func.isRequired,
    getCredentialsGroups: PropTypes.func.isRequired,
    getTotalCredentials: PropTypes.func.isRequired
  }).isRequired
};

export default withApi(CredentialContainer);
