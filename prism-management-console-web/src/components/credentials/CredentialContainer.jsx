import React from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import { observer } from 'mobx-react-lite';
import Logger from '../../helpers/Logger';
import CredentialTabs from './CredentialTabs';
import CredentialActionConfirmationModal from './Molecules/Modals/CredentialActionConfirmationModal';
import {
  DEFAULT_CREDENTIAL_VERIFICATION_RESULT,
  CREDENTIAL_ID_KEY,
  DRAFT_CREDENTIAL_VERIFICATION_RESULT,
  PENDING_CREDENTIAL_VERIFICATION_RESULT
} from '../../helpers/constants';
import { getTargetCredentials } from '../../helpers/credentialActions';
import { useCredentialActions } from '../../hooks/useCredentialActions';
import { useTemplateStore } from '../../hooks/useTemplateStore';
import { useCredentialsIssuedPageStore } from '../../hooks/useCredentialsIssuedPageStore';
import { useSelectAll } from '../../hooks/useSelectAll';
import { useApi } from '../../hooks/useApi';

const CredentialContainer = observer(() => {
  const { t } = useTranslation();
  const { wallet } = useApi();
  const {
    credentials: credentialsIssued,
    getCredentialsToSelect: getCredentialsIssuedToSelect,
    refreshCredentials: refreshCredentialsIssued,
    isFetching: isFetchingCredentialsIssued
  } = useCredentialsIssuedPageStore();
  useTemplateStore({ fetch: true });

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
  } = useCredentialActions(credentialsIssued, refreshCredentialsIssued);

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
      ? wallet.verifyCredential(encodedSignedCredential, batchInclusionProof).catch(error => {
          Logger.error('There has been an error verifiying the credential', error);
          const pendingPublication = error.message.includes('Missing publication date');
          if (pendingPublication) return PENDING_CREDENTIAL_VERIFICATION_RESULT;
          message.error(t('credentials.errors.errorVerifying'));
          return DEFAULT_CREDENTIAL_VERIFICATION_RESULT;
        })
      : DRAFT_CREDENTIAL_VERIFICATION_RESULT;

  const credentialsIssuedProps = {
    revokeSingleCredential,
    signSingleCredential,
    sendSingleCredential,
    selectionType: {
      selectedRowKeys: selectedCredentials,
      type: 'checkbox',
      onChange: setSelectedCredentials
    },
    bulkActionsProps: {
      refreshCredentials: refreshCredentialsIssued,
      revokeSelectedCredentials,
      signSelectedCredentials,
      sendSelectedCredentials,
      selectedCredentials,
      selectAllProps: selectAllCredentialsIssuedProps
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
      <CredentialTabs
        credentialsIssuedProps={credentialsIssuedProps}
        verifyCredential={verifyCredential}
      />
    </>
  );
});

export default CredentialContainer;
