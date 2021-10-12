import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import ConnectionsFilter from './Molecules/filter/ConnectionsFilter';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import { contactShape } from '../../helpers/propShapes';
import { withRedirector } from '../providers/withRedirector';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';
import AddUsersButton from './Atoms/AddUsersButtons/AddUsersButton';
import { useContactStore } from '../../hooks/useContactStore';

import './_style.scss';

const Connections = observer(
  ({ redirector: { redirectToContactDetails, redirectToImportContacts } }) => {
    const { t } = useTranslation();

    const { contacts, refreshContacts } = useContactStore();

    const [connectionToken, setConnectionToken] = useState('');
    const [QRModalIsOpen, showQRModal] = useState(false);
    const { accountStatus } = useSession();

    const inviteContactAndShowQR = async contactId => {
      const contactToInvite = contacts.find(c => c.contactId === contactId);
      setConnectionToken(contactToInvite.connectionToken);
      showQRModal(true);
    };

    const onQRClosed = () => {
      showQRModal(false);
      refreshContacts();
    };

    const newGroupButton = <AddUsersButton onClick={redirectToImportContacts} />;

    return (
      <div className="ConnectionsContainer Wrapper">
        {accountStatus === UNCONFIRMED && <WaitBanner />}
        <div className="ContentHeader">
          <div className="title">
            <h1>{t('contacts.title')}</h1>
          </div>
          <div className="flex spaceBetween fullWidth">
            <ConnectionsFilter />
            {accountStatus === CONFIRMED && newGroupButton}
          </div>
        </div>
        <div className="ConnectionsTable InfiniteScrollTableContainer">
          <ConnectionsTable
            inviteContact={inviteContactAndShowQR}
            viewContactDetail={redirectToContactDetails}
            searchDueGeneralScroll
            newContactButton={newGroupButton}
          />
        </div>
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
