import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import Logger from '../../helpers/Logger';
import Credentials from './Credentials';
import CredentialActionConfirmationModal from './Molecules/Modals/CredentialActionConfirmationModal';
import { withApi } from '../providers/withApi';
import {
  CREDENTIALS_ISSUED,
  CREDENTIALS_RECEIVED,
  DEFAULT_CREDENTIAL_VERIFICATION_RESULT,
  CREDENTIAL_ID_KEY,
  DRAFT_CREDENTIAL_VERIFICATION_RESULT,
  PENDING_CREDENTIAL_VERIFICATION_RESULT
} from '../../helpers/constants';
import { useCredentialsReceivedListWithFilters } from '../../hooks/useCredentials';
import { getTargetCredentials } from '../../helpers/credentialActions';
import { useCredentialActions } from '../../hooks/useCredentialActions';
import { useTemplateStore } from '../../hooks/useTemplateStore';
import {
  useCredentialIssuedStore,
  useCredentialIssuedUiState
} from '../../hooks/useCredentialIssuedStore';
import { useSelectAll } from '../../hooks/useSelectAll';

const CredentialContainer = observer(({ api }) => {
  const { t } = useTranslation();
  useCredentialIssuedUiState({ reset: true });
  const {
    credentials: credentialsIssued,
    getCredentialsToSelect: getCredentialsIssuedToSelect,
    refreshCredentialsIssued,
    isFetching: isFetchingCredentialsIssued,
    isLoadingFirstPage: isLoadingIssued
  } = useCredentialIssuedStore({ reset: true });

  const [activeTab, setActiveTab] = useState(CREDENTIALS_ISSUED);

  const { credentialTemplates: credentialTypes } = useTemplateStore({ fetch: true });

  const {
    fetchCredentialsReceived,
    filteredCredentialsReceived,
    filtersReceived,
    noReceivedCredentials,
    isLoading: isLoadingReceived,
    hasMoreReceived
  } = useCredentialsReceivedListWithFilters(api);

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
    if (activeTab === CREDENTIALS_RECEIVED && hasMoreReceived) fetchCredentialsReceived();
  }, [activeTab, fetchCredentialsReceived, hasMoreReceived]);

  const selectAllCredentialsIssuedProps = useSelectAll({
    displayedEntities: credentialsIssued,
    entitiesFetcher: getCredentialsIssuedToSelect,
    entityKey: CREDENTIAL_ID_KEY,
    selectedEntities: selectedCredentials,
    setSelectedEntities: setSelectedCredentials,
    isFetchingCredentialsIssued
  });

  const verifyCredential = ({ encodedSignedCredential, batchInclusionProof }) =>
    batchInclusionProof
      ? api.wallet.verifyCredential(encodedSignedCredential, batchInclusionProof).catch(error => {
          Logger.error('There has been an error verifiying the credential', error);
          const pendingPublication = error.message.includes('Missing publication date');
          if (pendingPublication) return PENDING_CREDENTIAL_VERIFICATION_RESULT;
          message.error(t('credentials.errors.errorVerifying'));
          return DEFAULT_CREDENTIAL_VERIFICATION_RESULT;
        })
      : DRAFT_CREDENTIAL_VERIFICATION_RESULT;

  const tabProps = {
    [CREDENTIALS_ISSUED]: {
      tableProps: {
        revokeSingleCredential,
        signSingleCredential,
        sendSingleCredential,
        selectionType: {
          selectedRowKeys: selectedCredentials,
          type: 'checkbox',
          onChange: setSelectedCredentials
        }
      },
      bulkActionsProps: {
        refreshCredentials: refreshCredentialsIssued,
        revokeSelectedCredentials,
        signSelectedCredentials,
        sendSelectedCredentials,
        selectedCredentials,
        selectAllProps: selectAllCredentialsIssuedProps
      },
      credentialTypes
    },
    [CREDENTIALS_RECEIVED]: {
      tableProps: {
        credentials: filteredCredentialsReceived
      },
      fetchCredentials: fetchCredentialsReceived,
      bulkActionsProps: {},
      filterProps: filtersReceived,
      showEmpty: noReceivedCredentials,
      credentialTypes
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
        loading={{ issued: isLoadingIssued, received: isLoadingReceived }}
        verifyCredential={verifyCredential}
      />
    </>
  );
});

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
    credentialTypesManager: PropTypes.shape({
      getCredentialTypes: PropTypes.func.isRequired,
      getCredentialTypeDetails: PropTypes.func.isRequired
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
