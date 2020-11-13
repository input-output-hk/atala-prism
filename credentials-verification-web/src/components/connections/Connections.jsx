import React, { useState, useEffect } from 'react';
import { Drawer, message } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import ConnectionsFilter from './Molecules/filter/ConnectionsFilter';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import noContacts from '../../images/noConnections.svg';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import AddUserButtons from './Atoms/AddUsersButtons/AddUsersButtons';
import { drawerWidth } from '../../helpers/constants';
import CredentialListDetail from '../common/Organisms/Detail/CredentialListDetail';
import { contactShape } from '../../helpers/propShapes';
import { withRedirector } from '../providers/withRedirector';

import './_style.scss';

const Connections = ({
  tableProps,
  inviteContact,
  handleContactsRequest,
  refreshContacts,
  redirector: { redirectToContactDetails }
}) => {
  const { t } = useTranslation();

  const [connectionToken, setConnectionToken] = useState('');
  const [QRModalIsOpen, showQRModal] = useState(false);
  const [currentContact, setCurrentContact] = useState({});
  const [showDrawer, setShowDrawer] = useState();

  useEffect(() => {
    if (!showDrawer) setCurrentContact({});
  }, [showDrawer]);

  const inviteContactAndShowQR = async contactId => {
    const token = await inviteContact(contactId);
    setConnectionToken(token);
    showQRModal(true);
  };

  const emptyProps = {
    photoSrc: noContacts,
    model: t('contacts.title')
  };

  const onQRClosed = () => {
    showQRModal(false);
    refreshContacts();
  };

  return (
    <div className="Wrapper">
      <Drawer
        title={t('contacts.detail.title')}
        placement="right"
        onClose={() => setShowDrawer(false)}
        visible={showDrawer}
        width={drawerWidth}
        destroyOnClose
      >
        {showDrawer && <CredentialListDetail {...currentContact} />}
      </Drawer>
      <div className="ContentHeader">
        <h1>{t('contacts.title')}</h1>
        <AddUserButtons />
      </div>
      <ConnectionsFilter fetchContacts={handleContactsRequest} />
      {tableProps.contacts.length ? (
        <ConnectionsTable
          inviteContact={inviteContactAndShowQR}
          viewContactDetail={redirectToContactDetails}
          handleContactsRequest={handleContactsRequest}
          {...tableProps}
        />
      ) : (
        <EmptyComponent {...emptyProps} />
      )}
      <QRModal
        visible={QRModalIsOpen}
        onCancel={onQRClosed}
        qrValue={connectionToken}
        tPrefix="contacts"
      />
    </div>
  );
};

Connections.propTypes = {
  handleContactsRequest: PropTypes.func.isRequired,
  tableProps: PropTypes.shape({
    contacts: PropTypes.arrayOf(PropTypes.shape(contactShape)),
    getCredentials: PropTypes.func.isRequired,
    hasMore: PropTypes.bool.isRequired
  }).isRequired,
  inviteContact: PropTypes.func.isRequired,
  refreshContacts: PropTypes.func.isRequired
};

export default withRedirector(Connections);
