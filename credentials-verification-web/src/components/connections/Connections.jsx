import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import ConnectionsFilter from './Molecules/filter/ConnectionsFilter';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import noContacts from '../../images/noConnections.svg';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import AddUserButtons from './Atoms/AddUsersButtons/AddUsersButtons';
import { contactShape } from '../../helpers/propShapes';
import { withRedirector } from '../providers/withRedirector';

import './_style.scss';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';

const Connections = ({
  tableProps,
  inviteContact,
  handleContactsRequest,
  refreshContacts,
  loading,
  searching,
  filterProps,
  redirector: { redirectToContactDetails }
}) => {
  const { t } = useTranslation();

  const [connectionToken, setConnectionToken] = useState('');
  const [QRModalIsOpen, showQRModal] = useState(false);

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

  const renderContent = () => {
    if (!tableProps.contacts.length && !loading && !searching)
      return <EmptyComponent {...emptyProps} />;
    if (!tableProps.contacts.length && (loading || searching)) return <SimpleLoading size="md" />;
    return (
      <ConnectionsTable
        inviteContact={inviteContactAndShowQR}
        viewContactDetail={redirectToContactDetails}
        handleContactsRequest={handleContactsRequest}
        searching={searching}
        {...tableProps}
      />
    );
  };

  return (
    <div className="Wrapper">
      <div className="ContentHeader">
        <h1>{t('contacts.title')}</h1>
        <AddUserButtons />
      </div>
      <ConnectionsFilter {...filterProps} fetchContacts={handleContactsRequest} />
      {renderContent()}
      <QRModal
        visible={QRModalIsOpen}
        onCancel={onQRClosed}
        qrValue={connectionToken}
        tPrefix="contacts"
      />
    </div>
  );
};

Connections.defaultProps = {
  loading: false,
  searching: false
};

Connections.propTypes = {
  handleContactsRequest: PropTypes.func.isRequired,
  tableProps: PropTypes.shape({
    contacts: PropTypes.arrayOf(PropTypes.shape(contactShape)),
    hasMore: PropTypes.bool.isRequired
  }).isRequired,
  loading: PropTypes.bool,
  searching: PropTypes.bool,
  inviteContact: PropTypes.func.isRequired,
  refreshContacts: PropTypes.func.isRequired,
  filterProps: PropTypes.shape({
    searchText: PropTypes.string,
    setSearchText: PropTypes.func,
    status: PropTypes.string,
    setStatus: PropTypes.func
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToContactDetails: PropTypes.func
  }).isRequired
};

export default withRedirector(Connections);
