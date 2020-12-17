import { useState, useEffect } from 'react';
import { message } from 'antd';
import { useTranslation } from 'react-i18next';
import { CREDENTIAL_PAGE_SIZE } from '../helpers/constants';
import Logger from '../helpers/Logger';
import { getLastArrayElementOrEmpty } from '../helpers/genericHelpers';
import {
  filterByInclusion,
  filterByExactMatch,
  filterByUnixDate,
  filterContactByStatus
} from '../helpers/filterHelpers';
import { credentialMapper } from '../APIs/helpers/credentialHelpers';

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

export const useCredentialsIssuedListWithFilters = (credentialsManager, setLoading) => {
  const { t } = useTranslation();
  const [credentials, setCredentials] = useState([]);
  const [filteredCredentials, setFilteredCredentials] = useState([]);
  const [noCredentials, setNoCredentials] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const filters = useCredentialsFilters();
  const { name, credentialStatus, credentialType, contactStatus, date } = filters.values;

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
      getCredentials();
    }
  }, [filteredCredentials, ...Object.values(filters.values)]);

  const setLoadingByKey = (key, value) =>
    setLoading(previousLoading => ({ ...previousLoading, [key]: value }));

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

  const getCredentials = async () => {
    try {
      const { credentialid } = getLastArrayElementOrEmpty(credentials);

      const newlyFetchedCredentials = await credentialsManager.getCredentials(
        CREDENTIAL_PAGE_SIZE,
        credentialid
      );

      if (newlyFetchedCredentials.length < CREDENTIAL_PAGE_SIZE) {
        setHasMore(false);
      }

      const credentialTypes = credentialsManager.getCredentialTypes();
      const mappedCredentials = newlyFetchedCredentials.map(cred =>
        credentialMapper(cred, credentialTypes)
      );
      const updatedCredentials = credentials.concat(mappedCredentials);
      const newFilteredCredentials = applyFilters(updatedCredentials);

      setCredentials(updatedCredentials);
      setFilteredCredentials(newFilteredCredentials);
    } catch (error) {
      Logger.error(
        '[CredentialContainer.getCredentialsIssued] Error while getting Credentials',
        error
      );
      message.error(t('errors.errorGetting', { model: 'Credentials' }));
    } finally {
      setLoadingByKey('issued', false);
    }
  };

  return {
    fetchCredentialsIssued: getCredentials,
    credentialsIssued: credentials,
    setCredentialsIssued: setCredentials,
    filtersIssued: {
      ...filters.values,
      ...filters.setters
    },
    filteredCredentialsIssued: filteredCredentials,
    noIssuedCredentials: noCredentials,
    hasMoreIssued: hasMore
  };
};
