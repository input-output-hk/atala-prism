import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import PropTypes from 'prop-types';
import Connections from './Connections';
import Logger from '../../helpers/Logger';
import { CONTACT_PAGE_SIZE } from '../../helpers/constants';
import { withApi } from '../providers/withApi';
import { getLastArrayElementOrEmpty } from '../../helpers/genericHelpers';
import { contactMapper } from '../../APIs/helpers';

const ConnectionsContainer = ({ api }) => {
  const { t } = useTranslation();

  const [contacts, setContacts] = useState([]);
  const [filteredContacts, setFilteredContacts] = useState([]);
  const [hasMore, setHasMore] = useState(true);

  const getContacts = ({ pageSize, lastId, _name, _status, _email, isRefresh, oldContacts = [] }) =>
    (hasMore || isRefresh ? api.contactsManager.getContacts(lastId, pageSize) : Promise.resolve([]))
      .then(recievedContacts => {
        if (recievedContacts.length < CONTACT_PAGE_SIZE) {
          setHasMore(false);
        } else {
          Logger.warn(
            'There were more rows than expected. Frontend-only filters will yield incomplete results'
          );
        }

        const contactsWithKey = recievedContacts.map(contactMapper);

        const updatedContacts = oldContacts.concat(contactsWithKey);

        const filteredRecievedContacts = updatedContacts.filter(it => {
          const caseInsensitiveMatch = (str1 = '', str2 = '') =>
            str1.toLowerCase().includes(str2.toLowerCase());

          const matchesName = caseInsensitiveMatch(it.fullname, _name);
          const matchesEmail = caseInsensitiveMatch(it.email, _email);
          // 0 is a valid status so it's not possible to check for !_status
          const matchesStatus = [undefined, '', it.status].includes(_status);

          return matchesStatus && matchesName && matchesEmail;
        });

        setContacts(updatedContacts);
        setFilteredContacts(filteredRecievedContacts);
      })
      .catch(error => {
        Logger.error('[Connections.getContacts] Error while getting connections', error);
        message.error(t('errors.errorGetting', { model: 'Contacts' }));
      });

  const refreshContacts = () => getContacts({ pageSize: contacts.length, isRefresh: true });

  const handleContactsRequest = (_name, _email, _status) => {
    const { contactid } = getLastArrayElementOrEmpty(contacts);

    return getContacts({
      pageSize: CONTACT_PAGE_SIZE,
      lastId: contactid,
      _name,
      _status,
      _email,
      oldContacts: contacts
    });
  };

  const inviteContact = contactId => api.contactsManager.generateConnectionToken(contactId);

  useEffect(() => {
    if (!contacts.length) handleContactsRequest();
  }, []);

  // Wrapper to preserve 'this' context
  const getCredentials = connectionId => api.connector.getMessagesForConnection(connectionId);

  const tableProps = {
    contacts: filteredContacts,
    hasMore,
    getCredentials
  };

  return (
    <Connections
      tableProps={tableProps}
      handleContactsRequest={handleContactsRequest}
      inviteContact={inviteContact}
      refreshContacts={refreshContacts}
    />
  );
};

ConnectionsContainer.propTypes = {
  api: PropTypes.shape().isRequired
};

export default withApi(ConnectionsContainer);
