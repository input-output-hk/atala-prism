import React from 'react';
import { observer } from 'mobx-react-lite';
import CredentialTabs from './CredentialTabs';
import CredentialActionConfirmationModal from './Molecules/Modals/CredentialActionConfirmationModal';
import { getTargetCredentials } from '../../helpers/credentialActions';
import { useCredentialActions } from '../../hooks/useCredentialActions';
import { useCredentialsIssuedPageStore } from '../../hooks/useCredentialsIssuedPageStore';

const CredentialContainer = observer(() => {
  const {
    credentials: credentialsIssued,
    refreshCredentials: refreshCredentialsIssued,
    selectedCredentials
  } = useCredentialsIssuedPageStore();

  const {
    verifyCredential,
    revokeSingleCredential,
    signSingleCredential,
    sendSingleCredential,
    revokeSelectedCredentials,
    signSelectedCredentials,
    sendSelectedCredentials,
    actions,
    confirmationModalProps
  } = useCredentialActions(selectedCredentials, credentialsIssued, refreshCredentialsIssued);

  const credentialActionsProps = {
    verifyCredential,
    revokeSingleCredential,
    signSingleCredential,
    sendSingleCredential,
    bulkActionsProps: {
      refreshCredentials: refreshCredentialsIssued,
      revokeSelectedCredentials,
      signSelectedCredentials,
      sendSelectedCredentials,
      selectedCredentials
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
      <CredentialTabs credentialActionsProps={credentialActionsProps} />
    </>
  );
});

export default CredentialContainer;
