import { useState, useEffect } from 'react';
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

const useGetContacts = (contactsManager, setContacts, setLoading, setSearching) => {
  const [hasMore, setHasMore] = useState(true);
  const { showUnconfirmedAccountError, removeUnconfirmedAccountError } = useSession();

  const getContacts = ({ pageSize, lastId, oldContacts = [], isRefresh = false, groupName }) =>
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
        if (setLoading) setLoading(false);
        if (setSearching) setSearching(false);
      });

  return [getContacts, hasMore];
};

const useGetContactsNotInGroup = (contactsManager, setContacts, setLoading, setSearching) => {
  const [hasMore, setHasMore] = useState(true);

  const getContacts = async ({
    pageSize,
    lastId,
    oldContacts = [],
    isRefresh = false,
    groupName
  }) => {
    try {
      const newContacts =
        isRefresh || hasMore ? await contactsManager.getContacts(lastId, pageSize) : [];
      if (newContacts.length < CONTACT_PAGE_SIZE) {
        setHasMore(false);
      } else {
        Logger.warn(
          'There were more rows than expected. Frontend-only filters will yield incomplete results'
        );
      }

      const contactsWithKey = newContacts.map(contactMapper);
      const updatedContacts = oldContacts.concat(contactsWithKey);

      const contactsInGroup = await contactsManager.getContacts(lastId, pageSize, groupName);
      const contactIdsInGroup = new Set(contactsInGroup.map(item => item.contactid));

      const contactsNotInGroup = updatedContacts.filter(
        item => !contactIdsInGroup.has(item.contactid)
      );

      setContacts(contactsNotInGroup);
    } catch (error) {
      Logger.error('[Contacts.getContacts] Error while getting contacts', error);
      message.error(i18n.t('errors.errorGetting', { model: 'Contacts' }));
    } finally {
      if (setLoading) setLoading(false);
      if (setSearching) setSearching(false);
    }
  };

  return [getContacts, hasMore];
};

export const useContacts = (contactsManager, setLoading) => {
  const [contacts, setContacts] = useState([]);
  const [getContacts, hasMore] = useGetContacts(contactsManager, setContacts, setLoading);

  const handleContactsRequest = () => {
    const { contactid } = getLastArrayElementOrEmpty(contacts);

    return getContacts({
      pageSize: CONTACT_PAGE_SIZE,
      lastId: contactid,
      oldContacts: contacts
    });
  };

  return [contacts, handleContactsRequest, hasMore];
};

export const useContactsWithFilteredList = (contactsManager, setLoading, setSearching) => {
  const [contacts, setContacts] = useState([]);
  const [getContacts, hasMore] = useGetContacts(
    contactsManager,
    setContacts,
    setLoading,
    setSearching
  );
  const [searchText, setSearchText] = useState();
  const [groupName, setGroupName] = useState();
  const [status, setStatus] = useState();
  const [filteredContacts, setFilteredContacts] = useState([]);

  const statusMatch = (contactStatus, filterStatus) =>
    !filterStatus || allowedStatuses[filterStatus].includes(contactStatus);

  const allowedStatuses = {
    [PENDING_CONNECTION]: [
      CONNECTION_STATUSES.invitationMissing,
      CONNECTION_STATUSES.connectionMissing
    ],
    [CONNECTED]: [CONNECTION_STATUSES.connectionAccepted]
  };

  useEffect(() => {
    const newFilteredContacts = applyFilters();
    setFilteredContacts(newFilteredContacts);
  }, [contacts, searchText, status]);

  useEffect(() => {
    /* if the amount of filtered contacts is less than the page size,
      there might be unfetched contacts that match the filters to show */
    if ((searchText || status) && filteredContacts.length < CONTACT_PAGE_SIZE && hasMore) {
      handleContactsRequest(groupName);
    }
  }, [filteredContacts, searchText, status]);

  const applyFilters = () =>
    contacts.filter(it => {
      const matchesName = filterByInclusion(searchText, it.contactName);
      const matchesExternalId = filterByInclusion(searchText, it.externalid);
      const matchesStatus = statusMatch(it.status, status);
      return matchesStatus && (matchesName || matchesExternalId);
    });

  const handleContactsRequest = (groupNameParam, isRefresh = false) => {
    const { contactid } = getLastArrayElementOrEmpty(contacts);
    if (groupNameParam) setGroupName(groupNameParam);

    setSearching(true);
    return getContacts({
      pageSize: CONTACT_PAGE_SIZE,
      lastId: isRefresh ? undefined : contactid,
      oldContacts: isRefresh ? [] : contacts,
      groupName: groupNameParam || groupName,
      isRefresh
    });
  };

  const filterProps = {
    searchText,
    setSearchText,
    status,
    setStatus
  };

  return {
    contacts,
    filteredContacts,
    setFilteredContacts,
    filterProps,
    getContacts,
    handleContactsRequest,
    hasMore
  };
};

export const useContactsWithFilteredListAndNotInGroup = (
  contactsManager,
  setLoading,
  setSearching
) => {
  const [contacts, setContacts] = useState([]);
  const [getContacts, hasMore] = useGetContactsNotInGroup(
    contactsManager,
    setContacts,
    setLoading,
    setSearching
  );
  const [searchText, setSearchText] = useState();
  const [groupName, setGroupName] = useState();
  const [filteredContacts, setFilteredContacts] = useState([]);

  useEffect(() => {
    const newFilteredContacts = applyFilters();
    setFilteredContacts(newFilteredContacts);
  }, [contacts, searchText]);

  useEffect(() => {
    /* if the amount of filtered contacts is less than the page size,
      there might be unfetched contacts that match the filters to show */
    if (searchText && filteredContacts.length < CONTACT_PAGE_SIZE && hasMore) {
      handleContactsRequest(groupName);
    }
  }, [filteredContacts, searchText]);

  const applyFilters = () =>
    contacts.filter(it => {
      const matchesName = filterByInclusion(searchText, it.contactName);
      const matchesExternalId = filterByInclusion(searchText, it.externalid);
      return matchesName || matchesExternalId;
    });

  const handleContactsRequest = (groupNameParam, isRefresh = false) => {
    const { contactid } = getLastArrayElementOrEmpty(contacts);
    if (groupNameParam) setGroupName(groupNameParam);

    setSearching(true);
    return getContacts({
      pageSize: MAX_CONTACTS,
      lastId: isRefresh ? undefined : contactid,
      oldContacts: isRefresh ? [] : contacts,
      groupName: groupNameParam || groupName,
      isRefresh
    });
  };

  const filterProps = {
    searchText,
    setSearchText
  };

  return {
    contacts,
    filteredContacts,
    filterProps,
    getContacts,
    handleContactsRequest,
    hasMore
  };
};

export const useAllContacts = contactsManager => {
  const [contacts, setContacts] = useState(null);
  const [getContacts] = useGetContacts(contactsManager, setContacts);

  useEffect(() => {
    getContacts({
      pageSize: MAX_CONTACTS,
      lastId: null
    });
  }, []);

  return { contacts };
};
