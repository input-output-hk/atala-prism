import { useState, useEffect, useCallback, useRef } from 'react';
import i18n from 'i18next';
import { message } from 'antd';
import {
  CONTACT_PAGE_SIZE,
  CONTACT_SORTING_KEYS,
  MAX_CONTACT_PAGE_SIZE,
  SORTING_DIRECTIONS,
  UNKNOWN_DID_SUFFIX_ERROR_CODE
} from '../helpers/constants';
import { contactMapper } from '../APIs/helpers/contactHelpers';
import Logger from '../helpers/Logger';
import { useSession } from './useSession';
import { useDebounce } from './useDebounce';

export const useContacts = (contactsManager, allowPreload = true) => {
  const [contacts, setContacts] = useState([]);
  const [scrollId, setScrollId] = useState(null);
  const [hasMore, setHasMore] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [shouldRun, setShouldRun] = useState(false);

  const [searchText, setSearchText] = useState();
  const [createdAt, setCreatedAt] = useState();
  const [status, setStatus] = useState();

  const [sortingField, setSortingField] = useState(CONTACT_SORTING_KEYS.name);
  const [sortingDirection, setSortingDirection] = useState(SORTING_DIRECTIONS.ascending);

  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const getContacts = useDebounce(
    ({
      pageSize,
      oldContacts = [],
      groupName,
      currentScrollId,
      dateCreated,
      field,
      direction,
      search,
      connectionStatus,
      onFinish,
      onResults
    }) => {
      setIsSearching(true);
      setShouldRun(false);

      contactsManager
        .getContacts({
          limit: pageSize,
          groupName,
          scrollId: currentScrollId,
          createdAt: dateCreated,
          field,
          direction,
          searchText: search,
          status: connectionStatus
        })
        .then(({ contactsList: newContacts, newScrollId }) => {
          setScrollId(newScrollId);
          if (newContacts.length < CONTACT_PAGE_SIZE) setHasMore(false);
          else
            Logger.warn(
              'There were more rows than expected. Frontend-only filters will yield incomplete results'
            );

          const contactsWithKey = newContacts.map(contactMapper);
          const updatedContacts = oldContacts.concat(contactsWithKey);

          setContacts(updatedContacts);
          removeUnconfirmedAccountError();
          if (onResults) onResults(updatedContacts);
        })
        .catch(error => {
          if (error.code === UNKNOWN_DID_SUFFIX_ERROR_CODE) {
            setHasMore(false);
            showUnconfirmedAccountError();
          } else {
            removeUnconfirmedAccountError();
            Logger.error('[Contacts.getContacts] Error while getting contacts', error);
            message.error(i18n.t('errors.errorGetting', { model: 'Contacts' }));
          }
        })
        .finally(() => {
          setHasMore(false);
          setIsLoading(false);
          setIsSearching(false);
          if (onFinish) onFinish();
        });
    },
    [contactsManager, removeUnconfirmedAccountError, showUnconfirmedAccountError]
  );

  useEffect(() => {
    setHasMore(true);
    setScrollId(null);
    setShouldRun(true);
  }, [sortingDirection, sortingField, searchText, status, createdAt]);

  useEffect(() => {
    if (shouldRun)
      getContacts({
        currentScrollId: scrollId,
        dateCreated: createdAt,
        field: sortingField,
        direction: sortingDirection,
        search: searchText,
        connectionStatus: status
      });
  }, [
    shouldRun,
    contacts,
    createdAt,
    getContacts,
    scrollId,
    searchText,
    sortingDirection,
    sortingField,
    status
  ]);

  const getMoreContacts = useCallback(
    ({ onFinish }) => {
      if (hasMore)
        getContacts({
          oldContacts: contacts,
          currentScrollId: scrollId,
          dateCreated: createdAt,
          field: sortingField,
          direction: sortingDirection,
          search: searchText,
          connectionStatus: status,
          onFinish
        });
      else onFinish();
    },
    [
      hasMore,
      contacts,
      createdAt,
      getContacts,
      scrollId,
      searchText,
      sortingDirection,
      sortingField,
      status
    ]
  );

  const refreshContacts = useCallback(() => {
    getContacts({
      pageSize: contacts.length,
      dateCreated: createdAt,
      field: sortingField,
      direction: sortingDirection,
      search: searchText,
      connectionStatus: status
    });
  }, [contacts, createdAt, getContacts, searchText, sortingDirection, sortingField, status]);

  const loadContacts = useRef(
    () =>
      allowPreload &&
      getContacts({
        field: sortingField,
        direction: sortingDirection
      })
  );

  useEffect(() => {
    loadContacts.current();
  }, []);

  return {
    contacts,
    refreshContacts,
    getMoreContacts,
    hasMore,
    isLoading,
    isSearching,
    filterProps: {
      searchText,
      setSearchText,
      createdAt,
      setCreatedAt,
      status,
      setStatus
    },
    sortProps: {
      sortingField,
      setSortingField,
      sortingDirection,
      setSortingDirection
    }
  };
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

      (isRefresh || hasMore
        ? contactsManager.getContacts({ lastSeenContactId: lastId, limit: pageSize })
        : Promise.resolve({ contactsList: [] })
      )
        .then(({ contactsList: newContacts }) => {
          if (newContacts.length < CONTACT_PAGE_SIZE) {
            setHasMore(false);
          } else {
            Logger.warn(
              'There were more rows than expected. Frontend-only filters will yield incomplete results'
            );
          }

          const contactsWithKey = newContacts.map(contactMapper);
          const updatedContacts = oldContacts.concat(contactsWithKey);

          return contactsManager
            .getContacts({ lastSeenContactId: lastId, limit: pageSize, groupName })
            .then(({ contactsList: contactsInGroup }) => {
              const contactIdsInGroup = new Set(contactsInGroup.map(item => item.contactId));

              const contactsNotInGroup = updatedContacts.filter(
                item => !contactIdsInGroup.has(item.contactId)
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
          setHasMore(false);
          setIsLoading(false);
          setIsSearching(false);
          if (onFinish) onFinish();
        });
    },
    [contactsManager, hasMore, isSearching]
  );

  return { contacts, getContacts, hasMore, isLoading, isSearching };
};

export const useContactsWithFilteredListAndNotInGroup = contactsManager => {
  const { contacts, getContacts, hasMore, isLoading, isSearching } = useGetContactsNotInGroup(
    contactsManager
  );
  const [searchText, setSearchText] = useState();
  const [groupName, setGroupName] = useState();

  const handleContactsRequest = useCallback(
    ({ groupNameParam, isRefresh = false, onFinish }) => {
      if (groupNameParam) setGroupName(groupNameParam);

      return getContacts({
        pageSize: MAX_CONTACT_PAGE_SIZE,
        oldContacts: isRefresh ? [] : contacts,
        groupName: groupNameParam || groupName,
        isRefresh,
        searchText,
        onFinish
      });
    },
    [contacts, getContacts, groupName, searchText]
  );

  const filterProps = {
    searchText,
    setSearchText
  };

  const fetchAll = onResults =>
    getContacts({
      pageSize: MAX_CONTACT_PAGE_SIZE,
      lastId: null,
      groupName,
      isFetchAll: true,
      onResults
    });

  return {
    contacts,
    filterProps,
    getContacts,
    handleContactsRequest,
    hasMore,
    isLoading,
    isSearching,
    fetchAll
  };
};
