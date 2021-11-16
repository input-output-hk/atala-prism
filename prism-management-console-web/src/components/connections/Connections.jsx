import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import ConnectionsFilter from './Molecules/filter/ConnectionsFilter';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';
import AddUsersButton from './Atoms/AddUsersButtons/AddUsersButton';
import { useContactStore, useContactUiState } from '../../hooks/useContactStore';
import { useRedirector } from '../../hooks/useRedirector';

import './_style.scss';

const Connections = observer(() => {
  const { t } = useTranslation();
  const { redirectToContactDetails, redirectToImportContacts } = useRedirector();
  const [connectionToken, setConnectionToken] = useState('');
  const [QRModalIsOpen, showQRModal] = useState(false);
  const { accountStatus } = useSession();
  const { hasFiltersApplied, isSearching, isSorting } = useContactUiState();
  const {
    contacts,
    initContactStore,
    refreshContacts,
    isLoadingFirstPage,
    fetchMoreData,
    isFetching,
    hasMore
  } = useContactStore();

  useEffect(() => {
    initContactStore();
  }, [initContactStore]);

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
          contacts={contacts}
          fetchMoreData={fetchMoreData}
          hasMore={hasMore}
          hasFiltersApplied={hasFiltersApplied}
          isLoading={isLoadingFirstPage || isLoadingFirstPage || isSorting}
          isFetchingMore={isFetching || isSearching}
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
});

export default Connections;
