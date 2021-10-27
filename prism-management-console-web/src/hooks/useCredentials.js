import { useState, useEffect, useCallback } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import _ from 'lodash';
import {
  CREDENTIAL_PAGE_SIZE,
  CREDENTIAL_SORTING_KEYS,
  MAX_CREDENTIAL_PAGE_SIZE,
  SORTING_DIRECTIONS,
  UNKNOWN_DID_SUFFIX_ERROR_CODE
} from '../helpers/constants';
import Logger from '../helpers/Logger';
import {
  filterByInclusion,
  filterByExactMatch,
  filterByUnixDate,
  filterContactByStatus
} from '../helpers/filterHelpers';
import { credentialMapper, credentialReceivedMapper } from '../APIs/helpers/credentialHelpers';
import { useSession } from './useSession';
import { useDebounce } from './useDebounce';

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

export const useCredentialsIssuedListWithFilters = credentialsManager => {
  const { t } = useTranslation();
  const [credentials, setCredentials] = useState([]);
  const [filteredCredentials, setFilteredCredentials] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [sortingBy, setSortingBy] = useState('createdOn');
  const [sortDirection, setSortDirection] = useState(SORTING_DIRECTIONS.ascending);

  const filters = useCredentialsFilters();
  const { name, credentialStatus, credentialType, contactStatus, date } = filters.values;

  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const applyFrontendFilters = useCallback(
    aCredentialsList =>
      aCredentialsList.filter(item => {
        const matchName = filterByInclusion(name, item.contactData.contactName);
        const matchExternalId = filterByInclusion(name, item.contactData.externalId);
        const matchContactStatus = filterContactByStatus(
          contactStatus,
          item.contactData.connectionStatus
        );
        const matchStatus = filterByExactMatch(credentialStatus, item.status);

        return (matchName || matchExternalId) && matchStatus && matchContactStatus;
      }),
    [contactStatus, credentialStatus, name]
  );

  const applyFrontendSorting = useCallback(
    aCredentialsList =>
      _.orderBy(
        aCredentialsList,
        o => o[sortingBy] || o.contactData[sortingBy],
        sortDirection === SORTING_DIRECTIONS.ascending ? 'asc' : 'desc'
      ),
    [sortingBy, sortDirection]
  );

  const getCredentials = useCallback(
    ({ isFetchAll } = {}) => {
      if (isLoading || isSearching) return;
      setIsSearching(true);
      const pageSize = isFetchAll ? MAX_CREDENTIAL_PAGE_SIZE : CREDENTIAL_PAGE_SIZE;
      return (hasMore
        ? credentialsManager.getCredentials(
            pageSize,
            credentials.length,
            filters.values,
            sortingBy && {
              field: sortingBy,
              direction: sortDirection
            }
          )
        : Promise.resolve([])
      )

        .then(newlyFetchedCredentials => {
          if (newlyFetchedCredentials.length < CREDENTIAL_PAGE_SIZE) setHasMore(false);

          const credentialTypes = credentialsManager.getCredentialTypes();
          const mappedCredentials = newlyFetchedCredentials.map(cred =>
            credentialMapper(cred, credentialTypes)
          );
          const updatedCredentials = credentials.concat(mappedCredentials);

          setCredentials(updatedCredentials);
          removeUnconfirmedAccountError();
          return updatedCredentials;
        })
        .then(updatedCredentials => applyFrontendFilters(updatedCredentials))
        .catch(error => {
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
        })
        .finally(() => {
          setHasMore(false);
          setIsLoading(false);
          setIsSearching(false);
        });
    },
    [
      isLoading,
      isSearching,
      credentials,
      credentialsManager,
      removeUnconfirmedAccountError,
      showUnconfirmedAccountError,
      applyFrontendFilters,
      t,
      filters,
      sortingBy,
      hasMore,
      sortDirection
    ]
  );

  const refreshCredentialsIssued = async () => {
    try {
      if (isLoading || isSearching) return;
      setIsLoading(true);
      const refreshedCredentials = await credentialsManager.getCredentials(
        credentials.length,
        null,
        filters.values,
        sortingBy && {
          field: CREDENTIAL_SORTING_KEYS[sortingBy],
          direction: sortDirection
        }
      );

      const credentialTypes = credentialsManager.getCredentialTypes();
      const mappedCredentials = refreshedCredentials.map(cred =>
        credentialMapper(cred, credentialTypes)
      );

      setCredentials(mappedCredentials);
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
      setHasMore(false);
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!credentials.length && !isLoading && !isSearching && hasMore) {
      setIsLoading(true);
      getCredentials({});
    }
  }, [credentials, isLoading, isSearching, hasMore, getCredentials]);

  useEffect(() => {
    const sortedCredentials = Object.keys(CREDENTIAL_SORTING_KEYS).includes(sortingBy)
      ? credentials
      : applyFrontendSorting(credentials);

    const newSortedAndFilteredCredentials = applyFrontendFilters(sortedCredentials);
    setFilteredCredentials(newSortedAndFilteredCredentials);
  }, [
    credentials,
    name,
    credentialStatus,
    contactStatus,
    sortingBy,
    sortDirection,
    applyFrontendFilters,
    applyFrontendSorting
  ]);

  const applyBackendFilters = useDebounce(refreshCredentialsIssued, [credentialType, date]);

  useEffect(() => {
    applyBackendFilters();
  }, [credentialType, date, applyBackendFilters]);

  const applyBackendSorting = useDebounce(refreshCredentialsIssued, [sortingBy, sortDirection]);

  useEffect(() => {
    if (!sortingBy || Object.keys(CREDENTIAL_SORTING_KEYS).includes(sortingBy)) {
      applyBackendSorting();
    }
  }, [sortingBy, sortDirection, applyBackendSorting]);

  useEffect(() => {
    /* if the amount of filtered credentials is less than the page size,
    there might be unfetched credentials that match the filters to show */
    const isSomeFilterSet = [name, credentialStatus, credentialType, contactStatus, date].some(
      val => val
    );

    if (isSomeFilterSet && filteredCredentials.length < CREDENTIAL_PAGE_SIZE && hasMore)
      getCredentials({});
  }, [
    filteredCredentials,
    name,
    credentialStatus,
    credentialType,
    contactStatus,
    date,
    getCredentials,
    hasMore
  ]);

  const fetchAll = onFinish => getCredentials({ isFetchAll: true, onFinish });

  return {
    fetchCredentialsIssued: getCredentials,
    refreshCredentialsIssued,
    credentialsIssued: credentials,
    setCredentialsIssued: setCredentials,
    filtersIssued: {
      ...filters.values,
      ...filters.setters
    },
    filteredCredentialsIssued: filteredCredentials,
    hasMoreIssued: hasMore,
    isLoading,
    isSearching,
    fetchAll,
    sortingBy,
    setSortingBy,
    sortDirection,
    setSortDirection
  };
};

export const useCredentialsReceivedListWithFilters = api => {
  const { t } = useTranslation();
  const [credentials, setCredentials] = useState([]);
  const [filteredCredentials, setFilteredCredentials] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  const filters = useCredentialsFilters();
  const { name, credentialType, date } = filters.values;

  const credentialTypes = api.credentialsManager.getCredentialTypes();

  useEffect(() => {
    const applyFrontendFilters = aCredentialsList =>
      aCredentialsList.filter(item => {
        const matchName = filterByInclusion(name, item.contactData.contactName);
        const matchExternalId = filterByInclusion(name, item.contactData.externalId);
        const matchType = filterByExactMatch(credentialType, item.credentialType?.id);
        const matchDate = filterByUnixDate(date, item.storedAt);

        return (matchName || matchExternalId) && matchType && matchDate;
      });

    const newFilteredCredentials = applyFrontendFilters(credentials);
    setFilteredCredentials(newFilteredCredentials);
  }, [credentials, name, credentialType, date]);

  const getCredentials = async () => {
    try {
      if (isLoading) return;
      setIsLoading(true);
      const newlyFetchedCredentials = await api.credentialsReceivedManager.getReceivedCredentials();
      const credentialWithIssuanceProofPromises = newlyFetchedCredentials.map(credential =>
        api.credentialsManager
          .getBlockchainData(credential.encodedSignedCredential)
          .then(issuanceProof => Object.assign({ issuanceProof }, credential))
      );
      const credentialsWithIssuanceProof = await Promise.all(credentialWithIssuanceProofPromises);

      const mappedCredentials = credentialsWithIssuanceProof.map(cred =>
        credentialReceivedMapper(cred, credentialTypes)
      );

      const updatedCredentials = credentials.concat(mappedCredentials);

      setCredentials(updatedCredentials);
    } catch (error) {
      Logger.error(
        '[CredentialContainer.getCredentialsRecieved] Error while getting Credentials',
        error
      );
      message.error(t('errors.errorGetting', { model: 'Credentials' }));
    } finally {
      setHasMore(false);
      setIsLoading(false);
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
    noReceivedCredentials: !credentials.length,
    isLoading,
    hasMoreReceived: hasMore
  };
};
