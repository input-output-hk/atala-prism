import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Logger from '../../helpers/Logger';
import Credentials from './Credentials';
import { withApi } from '../providers/withApi';
import { credentialMapper, credentialRecievedMapper } from '../../APIs/helpers/credentialHelpers';
import { getLastArrayElementOrEmpty } from '../../helpers/genericHelpers';
import {
  CREDENTIAL_PAGE_SIZE,
  CREDENTIAL_STATUSES,
  CREDENTIAL_STATUSES_TRANSLATOR,
  MAX_CREDENTIALS,
  CREDENTIALS_ISSUED,
  CREDENTIALS_RECIEVED
} from '../../helpers/constants';

const SEND_CREDENTIALS = 'SEND_CREDENTIALS';
const SIGN_CREDENTIALS = 'SIGN_CREDENTIALS';
const SEND_SINGLE_CREDENTIAL = 'SEND_SINGLE_CREDENTIAL';
const SIGN_SINGLE_CREDENTIAL = 'SIGN_SINGLE_CREDENTIAL';

const CredentialContainer = ({ api }) => {
  const { t } = useTranslation();
  // This field is used to know if there are no credentials on
  // the database, independently of the filters
  const [noIssuedCredentials, setNoIssuedCredentials] = useState(true);
  const [noRecievedCredentials, setNoRecievedCredentials] = useState(true);

  // These are the arrays from options
  const [categories, setCategories] = useState([]);
  const [groups, setGroups] = useState([]);

  // These are the credentials returned from the "backend"
  const [credentialsIssued, setCredentialsIssued] = useState([]);
  const [hasMoreIssued, setHasMoreIssued] = useState(true);
  const [credentialsRecieved, setCredentialsRecieved] = useState([]);
  const [hasMoreRecieved, setHasMoreRecieved] = useState(true);
  const [selectedCredentials, setSelectedCredentials] = useState([]);

  const [selectAll, setSelectAll] = useState(false);
  const [indeterminateSelectAll, setIndeterminateSelectAll] = useState(false);
  const [loadingSelection, setLoadingSelection] = useState(false);

  const [activeTab, setActiveTab] = useState(CREDENTIALS_ISSUED);

  const fetchCredentialsIssued = async () => {
    try {
      const { credentialid } = getLastArrayElementOrEmpty(credentialsIssued);

      const newlyFetchedCredentials = await api.credentialsManager.getCredentials(
        CREDENTIAL_PAGE_SIZE,
        credentialid
      );

      if (newlyFetchedCredentials.length < CREDENTIAL_PAGE_SIZE) {
        setHasMoreIssued(false);
      }

      const credentialTypes = api.credentialsManager.getCredentialTypes();
      const mappedCredentials = newlyFetchedCredentials.map(cred =>
        credentialMapper(cred, credentialTypes)
      );
      const updatedCredentialsIssued = credentialsIssued.concat(mappedCredentials);
      setCredentialsIssued(updatedCredentialsIssued);
      setNoIssuedCredentials(!updatedCredentialsIssued.length);
    } catch (error) {
      Logger.error('[CredentialContainer.getCredentials] Error while getting Credentials', error);
      message.error(t('errors.errorGetting', { model: 'Credentials' }));
    }
  };

  const fetchCredentialsRecieved = async () => {
    try {
      const { messageid } = getLastArrayElementOrEmpty(credentialsRecieved);

      const newlyFetchedCredentials = await api.connector.getCredentialsRecieved(
        CREDENTIAL_PAGE_SIZE,
        messageid
      );

      if (newlyFetchedCredentials.length < CREDENTIAL_PAGE_SIZE) {
        setHasMoreRecieved(false);
      }

      const mappedCredentials = newlyFetchedCredentials.map(credentialRecievedMapper);
      const updatedCredentialsRecieved = credentialsRecieved.concat(mappedCredentials);
      setCredentialsRecieved(updatedCredentialsRecieved);
      setNoRecievedCredentials(!updatedCredentialsRecieved.length);
    } catch (error) {
      Logger.error('[CredentialContainer.getCredentials] Error while getting Credentials', error);
      message.error(t('errors.errorGetting', { model: 'Credentials' }));
    }
  };

  useEffect(() => {
    fetchCredentialsIssued();
  }, []);

  useEffect(() => {
    if (!credentialsIssued.length && hasMoreIssued) fetchCredentialsIssued();
  }, [credentialsIssued, hasMoreIssued]);

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

  const signCredentials = creds => api.wallet.signCredentials(creds);

  // TODO: implement sending credenitals
  const sendCredentials = () => {};

  const showSuccess = () => {
    Logger.info('Successfully sent the credential(s) to the wallet');
    message.success(t('credentials.success.successSingleSign'));
  };

  const showNotImplementedWarning = () => {
    message.warn(t('credentials.messages.notImplementedYet'));
  };

  const actions = {
    [SIGN_CREDENTIALS]: {
      apiCall: signCredentials,
      requiredStatus: CREDENTIAL_STATUSES.credentialDraft,
      isBulkAction: true,
      onSuccess: showSuccess
    },
    [SEND_CREDENTIALS]: {
      apiCall: sendCredentials,
      requiredStatus: CREDENTIAL_STATUSES.credentialSigned,
      isBulkAction: true,
      onSuccess: showNotImplementedWarning
    },
    [SIGN_SINGLE_CREDENTIAL]: {
      apiCall: signCredentials,
      requiredStatus: CREDENTIAL_STATUSES.credentialDraft,
      onSuccess: showSuccess
    },
    [SEND_SINGLE_CREDENTIAL]: {
      apiCall: sendCredentials,
      requiredStatus: CREDENTIAL_STATUSES.credentialSigned,
      onSuccess: showNotImplementedWarning
    }
  };

  const getTargetById = targetId =>
    credentialsIssued.find(creds => creds.credentialid === targetId);

  const getTargetCredentials = actionType => {
    const targetCredentials = getSelectedCredentials();
    return targetCredentials.filter(({ status }) => status === actions[actionType].requiredStatus);
  };

  const getSelectedCredentials = () =>
    credentialsIssued.filter(c => selectedCredentials.includes(c.credentialid));

  const performBackendAction = async (actionType, targetId) => {
    try {
      const targetCredentials = actions[actionType].isBulkAction
        ? getTargetCredentials(actionType)
        : [getTargetById(targetId)];
      if (!targetCredentials.length) {
        const statusName = CREDENTIAL_STATUSES_TRANSLATOR[actions[actionType].requiredStatus];
        throw new Error(
          `Invalid credential status. Select at least one credential in '${t(
            `credentials.status.${statusName}`
          )}' status`
        );
      }

      await actions[actionType].apiCall(targetCredentials);
      actions[actionType].onSuccess();
    } catch (error) {
      Logger.error(error);
      message.error(t('credentials.errors.errorSigning'));
    }
  };

  const signSelectedCredentials = () => performBackendAction(SIGN_CREDENTIALS);
  const sendSelectedCredentials = () => performBackendAction(SEND_CREDENTIALS);
  const signSingleCredential = credentialid =>
    performBackendAction(SIGN_SINGLE_CREDENTIAL, credentialid);
  const sendSingleCredential = credentialid =>
    performBackendAction(SEND_SINGLE_CREDENTIAL, credentialid);

  const tabProps = {
    [CREDENTIALS_ISSUED]: {
      tableProps: {
        credentials: credentialsIssued,
        hasMore: hasMoreIssued,
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
        signSelectedCredentials,
        sendSelectedCredentials,
        selectAll,
        indeterminateSelectAll,
        toggleSelectAll
      },
      filterProps: {},
      showEmpty: noIssuedCredentials,
      loadingSelection
    },
    [CREDENTIALS_RECIEVED]: {
      tableProps: {
        credentials: credentialsRecieved,
        hasMore: hasMoreRecieved
      },
      fetchCredentials: fetchCredentialsRecieved,
      bulkActionsProps: {},
      filterProps: {},
      showEmpty: noRecievedCredentials
    }
  };

  return <Credentials tabProps={tabProps} setActiveTab={setActiveTab} />;
};

CredentialContainer.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({ getContact: PropTypes.func.isRequired }).isRequired,
    credentialsManager: PropTypes.shape({
      getCredentialBinary: PropTypes.func.isRequired,
      getCredentials: PropTypes.func.isRequired,
      getCredentialTypes: PropTypes.func.isRequired
    }).isRequired,
    wallet: PropTypes.shape({
      signCredentials: PropTypes.func.isRequired
    }).isRequired,
    connector: PropTypes.shape({ issueCredential: PropTypes.func.isRequired }).isRequired,
    getCredentialTypes: PropTypes.func.isRequired,
    getCategoryTypes: PropTypes.func.isRequired,
    getCredentialsGroups: PropTypes.func.isRequired,
    getTotalCredentials: PropTypes.func.isRequired
  }).isRequired
};

export default withApi(CredentialContainer);
