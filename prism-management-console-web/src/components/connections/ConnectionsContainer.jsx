import { observer } from 'mobx-react-lite';
import React, { useEffect, useState } from 'react';
import { useContactStore } from '../../hooks/useContactStore';
import Connections from './Connections';

const ConnectionsContainer = observer(() => {
  const [connectionToken, setConnectionToken] = useState('');
  const [qrModalIsVisible, setQrModalIsVisible] = useState(false);
  const { contacts, initContactStore, refreshContacts } = useContactStore();

  useEffect(() => {
    initContactStore();
  }, [initContactStore]);

  const handleInviteContactAndShowQR = async contactId => {
    const contactToInvite = contacts.find(c => c.contactId === contactId);
    setConnectionToken(contactToInvite.connectionToken);
    setQrModalIsVisible(true);
  };

  const handleCloseQR = () => {
    setQrModalIsVisible(false);
    refreshContacts();
  };

  return (
    <Connections
      connectionToken={connectionToken}
      qrModalIsVisible={qrModalIsVisible}
      onCloseQR={handleCloseQR}
      onInviteContact={handleInviteContactAndShowQR}
    />
  );
});
export default ConnectionsContainer;
