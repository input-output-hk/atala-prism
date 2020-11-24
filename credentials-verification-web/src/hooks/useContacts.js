import { useState, useEffect } from 'react';
import i18n from 'i18next';
import { message } from 'antd';
import { CONTACT_PAGE_SIZE } from '../helpers/constants';
import { getLastArrayElementOrEmpty } from '../helpers/genericHelpers';
import { contactMapper } from '../APIs/helpers/contactHelpers';
import Logger from '../helpers/Logger';
import { filterByInclusion } from '../helpers/filterHelpers';

const useGetContacts = (contactsManager, setContacts, setLoading) => {
  const [hasMore, setHasMore] = useState(true);

  const getContacts = ({ pageSize, lastId, oldContacts = [], isRefresh = false }) =>
    (hasMore || isRefresh ? contactsManager.getContacts(lastId, pageSize) : Promise.resolve([]))
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
      })
      .catch(error => {
        Logger.error('[Contacts.getContacts] Error while getting contacts', error);
        message.error(i18n.t('errors.errorGetting', { model: 'Contacts' }));
      })
      .finally(() => setLoading && setLoading(false));

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

export const useContactsWithFilteredList = (contactsManager, setLoading) => {
  const [contacts, setContacts] = useState([]);
  const [getContacts, hasMore] = useGetContacts(contactsManager, setContacts, setLoading);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState('');
  const [filteredContacts, setFilteredContacts] = useState([]);

  useEffect(() => {
    const newFilteredContacts = contacts.filter(it => {
      const matchesName = filterByInclusion(name, it.contactName);
      const matchesEmail = filterByInclusion(email, it.email);
      // 0 is a valid status so it's not possible to check for !_status
      const matchesStatus = [undefined, '', it.status].includes(status);

      return matchesStatus && matchesName && matchesEmail;
    });
    setFilteredContacts(newFilteredContacts);
  }, [contacts]);

  const handleContactsRequest = (_name, _email, _status) => {
    const { contactid } = getLastArrayElementOrEmpty(contacts);
    setName(_name);
    setEmail(_email);
    setStatus(_status);

    return getContacts({
      pageSize: CONTACT_PAGE_SIZE,
      lastId: contactid,
      oldContacts: contacts
    });
  };

  return { contacts, filteredContacts, getContacts, handleContactsRequest, hasMore };
};
