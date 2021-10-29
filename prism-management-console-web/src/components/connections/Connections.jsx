import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import ConnectionsFilter from './Molecules/filter/ConnectionsFilter';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import noContacts from '../../images/noConnections.svg';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import AddUserButtons from './Atoms/AddUsersButtons/AddUsersButtons';
import { contactShape } from '../../helpers/propShapes';
import { withRedirector } from '../providers/withRedirector';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';

import './_style.scss';

const Connections = observer(
  ({
    tableProps,
    handleContactsRequest,
    refreshContacts,
    loading,
    searching,
    filterProps,
    sortProps,
    redirector: { redirectToContactDetails }
  }) => {
    const { t } = useTranslation();

    const [connectionToken, setConnectionToken] = useState('');
    const [QRModalIsOpen, showQRModal] = useState(false);
    const { accountStatus } = useSession();

    const inviteContactAndShowQR = async contactId => {
      const contactToInvite = tableProps.contacts.find(c => c.contactId === contactId);
      setConnectionToken(contactToInvite.connectionToken);
      showQRModal(true);
    };

    const emptyProps = {
      photoSrc: noContacts,
      model: t('contacts.title'),
      isFilter: filterProps.searchText || filterProps.status,
      button: (
        <div className="flex justifyCenter">
          {!tableProps.contacts.length && accountStatus === CONFIRMED && <AddUserButtons />}
        </div>
      )
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
          searchDueGeneralScroll
          {...tableProps}
        />
      );
    };

    return (
      <div className="ConnectionsContainer Wrapper">
        {accountStatus === UNCONFIRMED && <WaitBanner />}
        <div className="ContentHeader">
          <div className="title">
            <h1>{t('contacts.title')}</h1>
          </div>
          <div className="flex spaceBetween fullWidth">
            <ConnectionsFilter
              {...filterProps}
              {...sortProps}
              fetchContacts={handleContactsRequest}
            />
            {accountStatus === CONFIRMED && <AddUserButtons />}
          </div>
        </div>
        {renderContent()}
        <QRModal
          visible={QRModalIsOpen}
          onCancel={onQRClosed}
          qrValue={connectionToken}
          tPrefix="contacts"
        />
      </div>
    );
  }
);

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
  refreshContacts: PropTypes.func.isRequired,
  filterProps: PropTypes.shape({
    searchText: PropTypes.string,
    setSearchText: PropTypes.func,
    status: PropTypes.string,
    setStatus: PropTypes.func
  }).isRequired,
  sortProps: PropTypes.shape({
    sortingField: PropTypes.string,
    setSortingField: PropTypes.func,
    sortingDirection: PropTypes.string,
    setSortingDirection: PropTypes.func
  }).isRequired,
  redirector: PropTypes.shape({
    redirectToContactDetails: PropTypes.func
  }).isRequired
};

export default withRedirector(Connections);
