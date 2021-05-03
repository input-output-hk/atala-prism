import { useState, useEffect, useCallback } from 'react';
import i18n from 'i18next';
import { message } from 'antd';
import {
  CONNECTED,
  CONNECTION_STATUSES,
  CONTACT_PAGE_SIZE,
  PENDING_CONNECTION,
  MAX_CONTACTS,
  UNKNOWN_DID_SUFFIX_ERROR_CODE
} from '../helpers/constants';
import { getLastArrayElementOrEmpty } from '../helpers/genericHelpers';
import { contactMapper } from '../APIs/helpers/contactHelpers';
import Logger from '../helpers/Logger';
import { filterByInclusion } from '../helpers/filterHelpers';
import { useSession } from '../components/providers/SessionContext';

const useGetContacts = (contactsManager, allowPreload = true) => {
  const [contacts, setContacts] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [isSearching, setIsSearching] = useState(false);

  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const getContacts = useCallback(
    ({ pageSize, lastId, oldContacts = [], isRefresh = false, groupName, onFinish, onResults }) => {
      if (isSearching) return;

      setIsSearching(true);

      (hasMore || isRefresh
        ? contactsManager.getContacts(lastId, pageSize, groupName)
        : Promise.resolve([])
      )
        .then(newContacts => {
          if (newContacts.length < CONTACT_PAGE_SIZE) {
            setHasMore(false);
          } else {
            Logger.warn(
              'There were more rows than expected. Frontend-only filters will yield incomplete results'
            );
          }
          const contactsWithKey = newContacts.map(contactMapper);
          const updatedContacts = oldContacts.concat(contactsWithKey);

          setContacts(updatedContacts);
          removeUnconfirmedAccountError();
          if (onResults) onResults(updatedContacts);
        })
        .catch(error => {
          if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
            showUnconfirmedAccountError();
          } else {
            removeUnconfirmedAccountError();
            Logger.error('[Contacts.getContacts] Error while getting contacts', error);
            message.error(i18n.t('errors.errorGetting', { model: 'Contacts' }));
          }
        })
        .finally(() => {
          setIsLoading(false);
          setIsSearching(false);
          if (onFinish) onFinish();
        });
    },
    [
      contactsManager,
      hasMore,
      isSearching,
      removeUnconfirmedAccountError,
      showUnconfirmedAccountError
    ]
  );

  useEffect(() => {
    if (hasMore && !contacts.length && !isLoading && !isSearching && allowPreload) {
      setIsLoading(true);
      getContacts({});
    }
  }, [hasMore, contacts, isLoading, isSearching, getContacts, allowPreload]);

  return { contacts, getContacts, hasMore, isLoading, isSearching };
};

const useGetContactsNotInGroup = contactsManager => {
  const [contacts, setContacts] = useState([]);
  const [hasMore, setHasMore] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);

  const getContacts = useCallback(
    ({ pageSize, lastId, oldContacts = [], isRefresh = false, groupName, onFinish, onResults }) => {
      if (isSearching) return;

      setIsSearching(true);

      (isRefresh || hasMore ? contactsManager.getContacts(lastId, pageSize) : Promise.resolve([]))
        .then(newContacts => {
          if (newContacts.length < CONTACT_PAGE_SIZE) {
            setHasMore(false);
          } else {
            Logger.warn(
              'There were more rows than expected. Frontend-only filters will yield incomplete results'
            );
          }

          const contactsWithKey = newContacts.map(contactMapper);
          const updatedContacts = oldContacts.concat(contactsWithKey);

          return contactsManager.getContacts(lastId, pageSize, groupName).then(contactsInGroup => {
            const contactIdsInGroup = new Set(contactsInGroup.map(item => item.contactid));

            const contactsNotInGroup = updatedContacts.filter(
              item => !contactIdsInGroup.has(item.contactid)
            );

            setContacts(contactsNotInGroup);
            if (onResults) onResults(contactsNotInGroup);
          });
        })
        .catch(error => {
          Logger.error('[Contacts.getContacts] Error while getting contacts', error);
          message.error(i18n.t('errors.errorGetting', { model: 'Contacts' }));
        })
        .finally(() => {
          setIsLoading(false);
          setIsSearching(false);
          if (onFinish) onFinish();
        });
    },
    [contactsManager, hasMore, isSearching]
  );

  return { contacts, getContacts, hasMore, isLoading, isSearching };
};

export const useContactsWithFilteredList = (contactsManager, allowPreload) => {
  const { contacts, getContacts, hasMore, isLoading, isSearching } = useGetContacts(
    contactsManager,
    allowPreload
  );
  const [searchText, setSearchText] = useState();
  const [groupName, setGroupName] = useState();
  const [status, setStatus] = useState();
  const [filteredContacts, setFilteredContacts] = useState([]);

  const applyFilters = useCallback(
    (contactsToFilter, cb) => {
      const allowedStatuses = {
        [PENDING_CONNECTION]: [
          CONNECTION_STATUSES.statusInvitationMissing,
          CONNECTION_STATUSES.statusConnectionMissing
        ],
        [CONNECTED]: [CONNECTION_STATUSES.statusConnectionAccepted]
      };

      const statusMatch = (contactStatus, filterStatus) =>
        !filterStatus || allowedStatuses[filterStatus].includes(contactStatus);

      const newFilteredContacts = contactsToFilter.filter(it => {
        const matchesName = filterByInclusion(searchText, it.contactName);
        const matchesExternalId = filterByInclusion(searchText, it.externalid);
        const matchesStatus = statusMatch(it.status, status);
        return matchesStatus && (matchesName || matchesExternalId);
      });

      setFilteredContacts(newFilteredContacts);
      if (cb) cb(newFilteredContacts);
    },
    [searchText, status]
  );

  useEffect(() => {
    applyFilters(contacts);
  }, [contacts, applyFilters]);

  const handleContactsRequest = useCallback(
    ({ groupNameParam, isRefresh = false, onFinish }) => {
      const { contactid } = getLastArrayElementOrEmpty(contacts);
      if (groupNameParam) setGroupName(groupNameParam);

      return getContacts({
        pageSize: CONTACT_PAGE_SIZE,
        lastId: isRefresh ? undefined : contactid,
        oldContacts: isRefresh ? [] : contacts,
        groupName: groupNameParam || groupName,
        isRefresh,
        onFinish
      });
    },
    [contacts, getContacts, groupName]
  );

  useEffect(() => {
    /* if the amount of filtered contacts is less than the page size,
      there might be unfetched contacts that match the filters to show */
    if ((searchText || status) && filteredContacts.length < CONTACT_PAGE_SIZE && hasMore) {
      handleContactsRequest();
    }
  }, [filteredContacts, searchText, status, handleContactsRequest, hasMore]);

  const filterProps = {
    searchText,
    setSearchText,
    status,
    setStatus
  };

  const fetchAll = cb =>
    getContacts({
      pageSize: MAX_CONTACTS,
      lastId: null,
      onResults: contactsReceived => applyFilters(contactsReceived, cb)
    });

  return {
    contacts,
    filteredContacts,
    filterProps,
    getContacts,
    handleContactsRequest,
    hasMore,
    isLoading,
    isSearching,
    fetchAll
  };
};

export const useContactsWithFilteredListAndNotInGroup = contactsManager => {
  const { contacts, getContacts, hasMore, isLoading, isSearching } = useGetContactsNotInGroup(
    contactsManager
  );
  const [searchText, setSearchText] = useState();
  const [groupName, setGroupName] = useState();
  const [filteredContacts, setFilteredContacts] = useState([]);

  const applyFilters = useCallback(
    (contactsToFilter, cb) => {
      const newFilteredContacts = contactsToFilter.filter(it => {
        const matchesName = filterByInclusion(searchText, it.contactName);
        const matchesExternalId = filterByInclusion(searchText, it.externalid);
        return matchesName || matchesExternalId;
      });
      setFilteredContacts(newFilteredContacts);
      if (cb) cb(newFilteredContacts);
    },
    [searchText]
  );

  useEffect(() => {
    applyFilters(contacts);
  }, [contacts, applyFilters]);

  const handleContactsRequest = useCallback(
    ({ groupNameParam, isRefresh = false, onFinish }) => {
      const { contactid } = getLastArrayElementOrEmpty(contacts);
      if (groupNameParam) setGroupName(groupNameParam);

      return getContacts({
        pageSize: MAX_CONTACTS,
        lastId: isRefresh ? undefined : contactid,
        oldContacts: isRefresh ? [] : contacts,
        groupName: groupNameParam || groupName,
        isRefresh,
        onFinish
      });
    },
    [contacts, getContacts, groupName]
  );

  useEffect(() => {
    /* if the amount of filtered contacts is less than the page size,
      there might be unfetched contacts that match the filters to show */
    if (searchText && filteredContacts.length < CONTACT_PAGE_SIZE && hasMore) {
      handleContactsRequest();
    }
  }, [filteredContacts, searchText, handleContactsRequest, hasMore]);

  const filterProps = {
    searchText,
    setSearchText
  };

  const fetchAll = cb =>
    getContacts({
      pageSize: MAX_CONTACTS,
      lastId: null,
      groupName,
      onResults: contactsReceived => applyFilters(contactsReceived, cb)
    });

  return {
    contacts,
    filteredContacts,
    filterProps,
    getContacts,
    handleContactsRequest,
    hasMore,
    isLoading,
    isSearching,
    fetchAll
  };
};

export const useAllContacts = contactsManager => {
  const { contacts, getContacts, hasMore, isLoading } = useGetContacts(contactsManager);

  useEffect(() => {
    if (hasMore && !isLoading)
      getContacts({
        pageSize: MAX_CONTACTS,
        lastId: null
      });
  }, [hasMore, isLoading, getContacts]);

  return { allContacts: contacts };
};
