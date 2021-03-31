import { useState, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import {
  CREDENTIAL_PAGE_SIZE,
  MAX_CREDENTIALS,
  UNKNOWN_DID_SUFFIX_ERROR_CODE
} from '../helpers/constants';
import Logger from '../helpers/Logger';
import { getLastArrayElementOrEmpty } from '../helpers/genericHelpers';
import {
  filterByInclusion,
  filterByExactMatch,
  filterByUnixDate,
  filterContactByStatus
} from '../helpers/filterHelpers';
import { credentialMapper, credentialReceivedMapper } from '../APIs/helpers/credentialHelpers';
import { useSession } from '../components/providers/SessionContext';

const useCredentialsFilters = () => {
  const [name, setName] = useState();
  const [credentialType, setCredentialType] = useState();
  const [credentialStatus, setCredentialStatus] = useState();
  const [contactStatus, setContactStatus] = useState();
  const [date, setDate] = useState();

  const values = {
    name,
    credentialType,
    credentialStatus,
    contactStatus,
    date
  };

  const setters = {
    setName,
    setCredentialType,
    setCredentialStatus,
    setContactStatus,
    setDate
  };

  return {
    values,
    setters
  };
};

export const useCredentialsIssuedListWithFilters = (
  credentialsManager,
  setLoading,
  setSearching
) => {
  const { t } = useTranslation();
  const [credentials, setCredentials] = useState([]);
  const [filteredCredentials, setFilteredCredentials] = useState([]);
  const [noCredentials, setNoCredentials] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const filters = useCredentialsFilters();
  const { name, credentialStatus, credentialType, contactStatus, date } = filters.values;

  const credentialTypes = credentialsManager.getCredentialTypes();
  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  useEffect(() => {
    if (!credentials.length && hasMore) getCredentials();
  }, [credentials, hasMore]);

  useEffect(() => {
    const newFilteredCredentials = applyFilters(credentials);
    setFilteredCredentials(newFilteredCredentials);
    setNoCredentials(!credentials.length);
  }, [credentials, ...Object.values(filters.values)]);

  useEffect(() => {
    /* if the amount of filtered credentials is less than the page size,
    there might be unfetched credentials that match the filters to show */
    const isSomeFilterSet = Object.values(filters.values).some(val => val);
    if (isSomeFilterSet && filteredCredentials.length < CREDENTIAL_PAGE_SIZE && hasMore) {
      setSearchingByKey('issued', true);
      getCredentials();
    }
  }, [filteredCredentials, ...Object.values(filters.values)]);

  const setIssuedLoading = value =>
    setLoading(previousLoading => ({ ...previousLoading, issued: value }));

  const setSearchingByKey = (key, value) =>
    setSearching(previousSearching => ({ ...previousSearching, [key]: value }));

  const applyFilters = aCredentialsList =>
    aCredentialsList.filter(item => {
      const matchName = filterByInclusion(name, item.contactData.contactName);
      const matchExternalId = filterByInclusion(name, item.contactData.externalid);
      const matchContactStatus = filterContactByStatus(contactStatus, item.contactData.status);
      const matchStatus = filterByExactMatch(credentialStatus, item.status);
      const matchType = filterByExactMatch(credentialType, item.credentialType.id);
      const matchDate = filterByUnixDate(date, item.publicationstoredat);

      return (
        (matchName || matchExternalId) &&
        matchStatus &&
        matchType &&
        matchContactStatus &&
        matchDate
      );
    });

  const getCredentials = async ({ isFetchAll } = {}) => {
    try {
      const { credentialid } = getLastArrayElementOrEmpty(credentials);

      const newlyFetchedCredentials = await credentialsManager.getCredentials(
        isFetchAll ? MAX_CREDENTIALS : CREDENTIAL_PAGE_SIZE,
        credentialid
      );

      if (newlyFetchedCredentials.length < CREDENTIAL_PAGE_SIZE) {
        setHasMore(false);
      }

      const mappedCredentials = newlyFetchedCredentials.map(cred =>
        credentialMapper(cred, credentialTypes)
      );
      const updatedCredentials = credentials.concat(mappedCredentials);
      const newFilteredCredentials = applyFilters(updatedCredentials);

      setCredentials(updatedCredentials);
      setFilteredCredentials(newFilteredCredentials);
      removeUnconfirmedAccountError();
      return {
        credentials: updatedCredentials,
        filteredCredentials: newFilteredCredentials
      };
    } catch (error) {
      Logger.error(
        '[CredentialContainer.getCredentialsRecieved] Error while getting Credentials',
        error
      );
      if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
        showUnconfirmedAccountError();
      } else {
        removeUnconfirmedAccountError();
        message.error(t('errors.errorGetting', { model: 'Credentials' }));
      }
    } finally {
      setIssuedLoading(false);
    }
  };

  // leave as async function for backward compatibility,
  // so promise callbacks can be used when this function is called
  const handleGetCredentials = async () => hasMore && getCredentials();

  const fetchAll = () => getCredentials({ isFetchAll: true });

  return {
    fetchCredentialsIssued: handleGetCredentials,
    credentialsIssued: credentials,
    setCredentialsIssued: setCredentials,
    filtersIssued: {
      ...filters.values,
      ...filters.setters
    },
    filteredCredentialsIssued: filteredCredentials,
    noIssuedCredentials: noCredentials,
    hasMoreIssued: hasMore,
    fetchAll
  };
};

export const useCredentialsReceivedListWithFilters = (api, setLoading) => {
  const { t } = useTranslation();
  const [credentials, setCredentials] = useState([]);
  const [filteredCredentials, setFilteredCredentials] = useState([]);
  const [noCredentials, setNoCredentials] = useState(true);
  const filters = useCredentialsFilters();
  const { name, credentialType, date } = filters.values;

  const credentialTypes = api.credentialsManager.getCredentialTypes();

  useEffect(() => {
    const newFilteredCredentials = applyFilters(credentials);
    setFilteredCredentials(newFilteredCredentials);
    setNoCredentials(!credentials.length);
  }, [credentials, ...Object.values(filters.values)]);

  const setReceivedLoading = value =>
    setLoading(previousLoading => ({ ...previousLoading, received: value }));

  const applyFilters = aCredentialsList =>
    aCredentialsList.filter(item => {
      const matchName = filterByInclusion(name, item.contactData.contactName);
      const matchExternalId = filterByInclusion(name, item.contactData.externalid);
      const matchType = filterByExactMatch(credentialType, item.credentialType?.id);
      const matchDate = filterByUnixDate(date, item.storedat);

      return (matchName || matchExternalId) && matchType && matchDate;
    });

  const getCredentials = async () => {
    try {
      setReceivedLoading(true);
      const newlyFetchedCredentials = await api.credentialsReceivedManager.getReceivedCredentials();
      const credentialWithIssuanceProofPromises = newlyFetchedCredentials.map(credential =>
        api.credentialsManager
          .getBlockchainData(credential.encodedsignedcredential)
          .then(issuanceproof => Object.assign({ issuanceproof }, credential))
      );
      const credentialsWithIssuanceProof = await Promise.all(credentialWithIssuanceProofPromises);

      const mappedCredentials = credentialsWithIssuanceProof.map(cred =>
        credentialReceivedMapper(cred, credentialTypes)
      );

      const updatedCredentials = credentials.concat(mappedCredentials);
      const newFilteredCredentials = applyFilters(updatedCredentials);

      setCredentials(updatedCredentials);
      setFilteredCredentials(newFilteredCredentials);
      setNoCredentials(!updatedCredentials.length);
    } catch (error) {
      Logger.error(
        '[CredentialContainer.getCredentialsRecieved] Error while getting Credentials',
        error
      );
      message.error(t('errors.errorGetting', { model: 'Credentials' }));
    } finally {
      setReceivedLoading(false);
    }
  };

  return {
    fetchCredentialsReceived: getCredentials,
    credentialsReceived: credentials,
    setCredentialsReceived: setCredentials,
    filtersReceived: {
      ...filters.values,
      ...filters.setters
    },
    filteredCredentialsReceived: filteredCredentials,
    noReceivedCredentials: noCredentials
  };
};
