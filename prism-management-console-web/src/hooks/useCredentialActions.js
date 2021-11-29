import { message } from 'antd';
import i18n from 'i18next';
import { useState } from 'react';
import {
  CREDENTIAL_STATUSES_TRANSLATOR,
  DEFAULT_CREDENTIAL_VERIFICATION_RESULT,
  DRAFT_CREDENTIAL_VERIFICATION_RESULT,
  PENDING_CREDENTIAL_VERIFICATION_RESULT,
  REVOKE_CREDENTIALS,
  REVOKE_SINGLE_CREDENTIAL,
  SEND_CREDENTIALS,
  SEND_SINGLE_CREDENTIAL,
  SIGN_CREDENTIALS,
  SIGN_SINGLE_CREDENTIAL
} from '../helpers/constants';
import { credentialRequiredStatus, getTargetCredentials } from '../helpers/credentialActions';
import Logger from '../helpers/Logger';
import { useApi } from './useApi';

export const useCredentialActions = (
  selectedCredentials,
  credentialsIssued,
  refreshCredentialsIssued
) => {
  const { wallet, connector, contactsManager, credentialsManager } = useApi();
  const [confirmationModal, setConfirmationModal] = useState(false);

  const verifyCredential = ({ encodedSignedCredential, batchInclusionProof }) =>
    batchInclusionProof
      ? wallet.verifyCredential(encodedSignedCredential, batchInclusionProof).catch(error => {
          Logger.error('There has been an error verifiying the credential', error);
          const pendingPublication = error.message.includes('Missing publication date');
          if (pendingPublication) return PENDING_CREDENTIAL_VERIFICATION_RESULT;
          message.error(i18n.t('credentials.errors.errorVerifying'));
          return DEFAULT_CREDENTIAL_VERIFICATION_RESULT;
        })
      : DRAFT_CREDENTIAL_VERIFICATION_RESULT;

  const revokeCredentials = credentials => wallet.revokeCredentials(credentials);

  const signCredentials = credentials => wallet.signCredentials(credentials);

  const sendCredential = async ([{ contactData, ...cred }]) => {
    const { connectionId } = await contactsManager.getContact(contactData.contactId);
    const credentialBinary = credentialsManager.getCredentialBinary(cred);
    await connector.sendCredential(credentialBinary, connectionId);
    await credentialsManager.markAsSent(cred.credentialId);
    setConfirmationModal(false);
  };

  const gatherPayloadFromCredential = async c => ({
    atalaMessage: await credentialsManager.generateAtalaMessage(c),
    connectionToken: (await contactsManager.getContact(c.contactId)).connectionToken
  });

  const markCredentialsAsSent = credentials =>
    Promise.all(credentials.map(c => credentialsManager.markAsSent(c.credentialId)));

  const sendCredentialsBulk = credentials =>
    Promise.all(credentials.map(gatherPayloadFromCredential))
      .then(payload => connector.sendCredentialsBulk(payload))
      .then(() => markCredentialsAsSent(credentials))
      .then(() => setConfirmationModal(false));

  const showRevokeSuccess = () => {
    Logger.info('Successfully sent credential(s) to the wallet');
    message.success(i18n.t('credentials.success.successRevoke'));
  };

  const showSignSuccess = () => {
    Logger.info('Successfully sent the credential(s) to the wallet');
    message.success(i18n.t('credentials.success.successSign'));
  };

  const showSendSuccess = () => {
    Logger.info('Successfully sent the selected credentials');
    message.success(i18n.t('credentials.success.successSend'));
  };

  const showRevokeError = error => {
    Logger.error(error);
    message.error(i18n.t('credentials.errors.errorRevoking'));
  };

  const showSignError = error => {
    Logger.error(error);
    message.error(i18n.t('credentials.errors.errorSigning'));
  };

  const showSendError = error => {
    Logger.error(error);
    message.error(i18n.t('credentials.errors.errorSending'));
  };

  const actions = {
    [REVOKE_CREDENTIALS]: {
      apiCall: revokeCredentials,
      requiredStatus: credentialRequiredStatus[REVOKE_CREDENTIALS],
      onSuccess: showRevokeSuccess,
      onError: showRevokeError
    },
    [SIGN_CREDENTIALS]: {
      apiCall: signCredentials,
      requiredStatus: credentialRequiredStatus[SIGN_CREDENTIALS],
      onSuccess: showSignSuccess,
      onError: showSignError
    },
    [SEND_CREDENTIALS]: {
      apiCall: sendCredentialsBulk,
      requiredStatus: credentialRequiredStatus[SEND_CREDENTIALS],
      onSuccess: showSendSuccess,
      onError: showSendError
    },
    [REVOKE_SINGLE_CREDENTIAL]: {
      apiCall: revokeCredentials,
      onSuccess: showRevokeSuccess,
      onError: showRevokeError
    },
    [SIGN_SINGLE_CREDENTIAL]: {
      apiCall: signCredentials,
      onSuccess: showSignSuccess,
      onError: showSignError
    },
    [SEND_SINGLE_CREDENTIAL]: {
      apiCall: sendCredential,
      onSuccess: showSendSuccess,
      onError: showSendError
    }
  };

  const performBackendAction = async (actionType, targetId) => {
    const { requiredStatus, apiCall, onSuccess, onError } = actions[actionType];
    try {
      const { targetCredentials } = requiredStatus
        ? getTargetCredentials(credentialsIssued, selectedCredentials, requiredStatus)
        : wrapSingleCredential(targetId);
      if (!targetCredentials.length) {
        const statusName = CREDENTIAL_STATUSES_TRANSLATOR[requiredStatus.credentials];
        const statusLabel = i18n.t(`credentials.status.${statusName}`);
        throw new Error(
          `Invalid credential status. Select at least one credential in '${statusLabel}' status`
        );
      }
      if (apiCall) await apiCall(targetCredentials);
      if (onSuccess) onSuccess();
      setConfirmationModal(null);
      refreshCredentialsIssued();
    } catch (error) {
      if (onError) onError(error);
    }
  };

  const wrapSingleCredential = targetId => ({
    targetCredentials: [getTargetById(targetId)]
  });

  const getTargetById = targetId =>
    credentialsIssued.find(creds => creds.credentialId === targetId);

  const revokeSelectedCredentials = () => setConfirmationModal(REVOKE_CREDENTIALS);
  const signSelectedCredentials = () => setConfirmationModal(SIGN_CREDENTIALS);
  const sendSelectedCredentials = () => setConfirmationModal(SEND_CREDENTIALS);

  const revokeSingleCredential = credentialId =>
    performBackendAction(REVOKE_SINGLE_CREDENTIAL, credentialId);
  const signSingleCredential = credentialId =>
    performBackendAction(SIGN_SINGLE_CREDENTIAL, credentialId);
  const sendSingleCredential = credentialId =>
    performBackendAction(SEND_SINGLE_CREDENTIAL, credentialId);

  const handleConfirm = () => performBackendAction(confirmationModal);

  const handleCancel = () => setConfirmationModal(false);

  return {
    selectedCredentials,
    confirmationModalProps: {
      type: confirmationModal,
      onOk: handleConfirm,
      onCancel: handleCancel
    },
    verifyCredential,
    revokeSelectedCredentials,
    revokeSingleCredential,
    signSelectedCredentials,
    signSingleCredential,
    sendSelectedCredentials,
    sendSingleCredential,
    actions
  };
};
