import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import Credentials from './Credentials';
import SendCredentialsConfirmationModal from './Molecules/Modals/SendCredentialConfirmationModal';
import { withApi } from '../providers/withApi';
import { credentialMapper, credentialReceivedMapper } from '../../APIs/helpers/credentialHelpers';
import {
  CREDENTIAL_STATUSES,
  CREDENTIAL_STATUSES_TRANSLATOR,
  MAX_CREDENTIALS,
  CREDENTIALS_ISSUED,
  CREDENTIALS_RECEIVED,
  CONNECTION_STATUSES
} from '../../helpers/constants';
import { useCredentialsIssuedListWithFilters } from '../../hooks/useCredentials';

const SEND_CREDENTIALS = 'SEND_CREDENTIALS';
const SIGN_CREDENTIALS = 'SIGN_CREDENTIALS';
const SEND_SINGLE_CREDENTIAL = 'SEND_SINGLE_CREDENTIAL';
const SIGN_SINGLE_CREDENTIAL = 'SIGN_SINGLE_CREDENTIAL';

const CredentialContainer = ({ api }) => {
  const { t } = useTranslation();
  // This field is used to know if there are no credentials on
  // the database, independently of the filters
  const [noReceivedCredentials, setNoReceivedCredentials] = useState(true);

  const [loading, setLoading] = useState({ issued: true, received: true });
  const [searching, setSearching] = useState({ issued: false, received: false });

  const {
    credentialsIssued,
    fetchCredentialsIssued,
    setCredentialsIssued,
    filteredCredentialsIssued,
    filtersIssued,
    hasMoreIssued,
    noIssuedCredentials
  } = useCredentialsIssuedListWithFilters(api.credentialsManager, setLoading, setSearching);

  const [credentialsReceived, setCredentialsReceived] = useState([]);

  const [selectedCredentials, setSelectedCredentials] = useState([]);

  const [selectAll, setSelectAll] = useState(false);
  const [indeterminateSelectAll, setIndeterminateSelectAll] = useState(false);
  const [loadingSelection, setLoadingSelection] = useState(false);
  const [activeTab, setActiveTab] = useState(CREDENTIALS_ISSUED);
  const [showConfirmationModal, setShowConfirmationModal] = useState(false);

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
    } catch (error) {
      Logger.error(
        '[CredentialContainer.refreshCredentialsIssued] Error while getting Credentials',
        error
      );
      message.error(t('errors.errorGetting', { model: 'Credentials' }));
    } finally {
      setLoadingByKey('issued', false);
    }
  };

  const fetchCredentialsReceived = async () => {
    try {
      setLoadingByKey('received', true);
      const newlyFetchedCredentials = await api.credentialsReceivedManager.getReceivedCredentials();
      const credentialWithIssuanceProofPromises = newlyFetchedCredentials.map(credential =>
        api.credentialsManager
          .getBlockchainData(credential.encodedsignedcredential)
          .then(issuanceproof => Object.assign({ issuanceproof }, credential))
      );
      const credentialsWithIssuanceProof = await Promise.all(credentialWithIssuanceProofPromises);

      const credentialTypes = api.credentialsManager.getCredentialTypes();
      const mappedCredentials = credentialsWithIssuanceProof.map(cred =>
        credentialReceivedMapper(cred, credentialTypes)
      );
      const updatedCredentialsReceived = credentialsReceived.concat(mappedCredentials);
      setCredentialsReceived(updatedCredentialsReceived);
      setNoReceivedCredentials(!updatedCredentialsReceived.length);
    } catch (error) {
      Logger.error(
        '[CredentialContainer.getCredentialsRecieved] Error while getting Credentials',
        error
      );
      message.error(t('errors.errorGetting', { model: 'Credentials' }));
    } finally {
      setLoadingByKey('received', false);
    }
  };

  useEffect(() => {
    fetchCredentialsIssued();
    fetchCredentialsReceived();
  }, []);

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

  const signCredentials = credentials => api.wallet.signCredentials(credentials);

  const sendCredentials = credentials => {
    const handleSendCredential = async ({ contactData, ...cred }) => {
      const { connectionid } = await api.contactsManager.getContact(contactData.contactid);
      const credentialBinary = api.credentialsManager.getCredentialBinary(cred);
      await api.connector.sendCredential(credentialBinary, connectionid);
      return api.credentialsManager.markAsSent(cred.credentialid);
    };

    const sendCredentialsRequests = credentials.map(handleSendCredential);

    return Promise.all(sendCredentialsRequests).then(() => setShowConfirmationModal(false));
  };

  const showSignSuccess = () => {
    Logger.info('Successfully sent the credential(s) to the wallet');
    message.success(t('credentials.success.successSign'));
  };

  const showSendSuccess = () => {
    Logger.info('Successfully sent the credential(s) to the wallet');
    message.success(t('credentials.success.successSend'));
  };

  const showSignError = error => {
    Logger.error(error);
    message.error(t('credentials.errors.errorSigning'));
  };

  const showSendError = error => {
    Logger.error(error);
    message.error(t('credentials.errors.errorSending'));
  };

  const signCredentialsRequiredStatus = {
    credential: CREDENTIAL_STATUSES.credentialDraft
  };

  const sendCredentialsRequiredStatus = {
    credential: CREDENTIAL_STATUSES.credentialSigned,
    contact: CONNECTION_STATUSES.connectionAccepted
  };

  const actions = {
    [SIGN_CREDENTIALS]: {
      apiCall: signCredentials,
      requiredStatus: signCredentialsRequiredStatus,
      onSuccess: showSignSuccess,
      onError: showSignError
    },
    [SEND_CREDENTIALS]: {
      apiCall: sendCredentials,
      requiredStatus: sendCredentialsRequiredStatus,
      onSuccess: showSendSuccess,
      onError: showSendError
    },
    [SIGN_SINGLE_CREDENTIAL]: {
      apiCall: signCredentials,
      onSuccess: showSignSuccess,
      onError: showSignError
    },
    [SEND_SINGLE_CREDENTIAL]: {
      apiCall: sendCredentials,
      onSuccess: showSendSuccess,
      onError: showSendError
    }
  };

  const getTargetById = targetId =>
    credentialsIssued.find(creds => creds.credentialid === targetId);

  const wrapSingleCredential = targetId => ({
    targetCredentials: [getTargetById(targetId)]
  });

  const getTargetCredentials = requiredStatus => {
    const selected = getSelectedCredentials();
    return {
      selected,
      targetCredentials: selected.filter(({ status, contactData }) => {
        const validCredentialStatus =
          !requiredStatus.credential || status === requiredStatus.credential;
        const validContactStatus =
          !requiredStatus.contact || contactData.status === requiredStatus.contact;
        return validCredentialStatus && validContactStatus;
      })
    };
  };

  const getSelectedCredentials = () =>
    credentialsIssued.filter(c => selectedCredentials.includes(c.credentialid));

  const performBackendAction = async (actionType, targetId) => {
    const { requiredStatus, apiCall, onSuccess, onError } = actions[actionType];
    try {
      const { targetCredentials } = requiredStatus
        ? getTargetCredentials(requiredStatus)
        : wrapSingleCredential(targetId);
      if (!targetCredentials.length) {
        const statusName = CREDENTIAL_STATUSES_TRANSLATOR[requiredStatus];
        throw new Error(
          `Invalid credential status. Select at least one credential in '${t(
            `credentials.status.${statusName}`
          )}' status`
        );
      }
      if (apiCall) await apiCall(targetCredentials);
      if (onSuccess) onSuccess();
      refreshCredentialsIssued();
    } catch (error) {
      if (onError) onError(error);
    }
  };

  const signSelectedCredentials = () => performBackendAction(SIGN_CREDENTIALS);
  const signSingleCredential = credentialid =>
    performBackendAction(SIGN_SINGLE_CREDENTIAL, credentialid);
  const sendSingleCredential = credentialid =>
    performBackendAction(SEND_SINGLE_CREDENTIAL, credentialid);

  const sendSelectedCredentials = () => {
    setShowConfirmationModal(true);
  };

  const handleConfirmSend = () => performBackendAction(SEND_CREDENTIALS);

  const handleCancel = () => {
    setShowConfirmationModal(false);
  };

  const tabProps = {
    [CREDENTIALS_ISSUED]: {
      tableProps: {
        credentials: filteredCredentialsIssued,
        hasMore: hasMoreIssued,
        searching: searching.issued,
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
        credentials: credentialsReceived
      },
      bulkActionsProps: {},
      filterProps: {},
      showEmpty: noReceivedCredentials
    }
  };

  return (
    <>
      <SendCredentialsConfirmationModal
        visible={showConfirmationModal}
        onOk={handleConfirmSend}
        onCancel={handleCancel}
        {...getTargetCredentials(sendCredentialsRequiredStatus)}
      />
      <Credentials tabProps={tabProps} setActiveTab={setActiveTab} loading={loading} />
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
      signCredentials: PropTypes.func.isRequired
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
