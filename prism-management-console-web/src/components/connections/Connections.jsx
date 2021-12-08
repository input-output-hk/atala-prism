import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import ConnectionsFilter from './Molecules/filter/ConnectionsFilter';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';
import AddUsersButton from './Atoms/AddUsersButtons/AddUsersButton';
import { useContactsPageStore } from '../../hooks/useContactsPageStore';
import { useRedirector } from '../../hooks/useRedirector';

import './_style.scss';

const Connections = observer(
  ({ qrModalIsVisible, connectionToken, onCloseQR, onInviteContact }) => {
    const { t } = useTranslation();
    const { redirectToContactDetails, redirectToImportContacts } = useRedirector();
    const { accountStatus } = useSession();
    const {
      contacts,
      filterSortingProps,
      isSearching,
      isLoadingFirstPage,
      fetchMoreData,
      isFetching,
      hasMore
    } = useContactsPageStore();
    const { hasFiltersApplied } = filterSortingProps;

    const newGroupButton = <AddUsersButton onClick={redirectToImportContacts} />;

    return (
      <div className="ConnectionsContainer Wrapper">
        {accountStatus === UNCONFIRMED && <WaitBanner />}
        <div className="ContentHeader">
          <div className="title">
            <h1>{t('contacts.title')}</h1>
          </div>
          <div className="ConnectionFilterWrapper">
            <ConnectionsFilter filterSortingProps={filterSortingProps} />
            {accountStatus === CONFIRMED && newGroupButton}
          </div>
        </div>
        <div className="ConnectionsTable InfiniteScrollTableContainer">
          <ConnectionsTable
            contacts={contacts}
            fetchMoreData={fetchMoreData}
            hasMore={hasMore}
            hasFiltersApplied={hasFiltersApplied}
            isLoading={isLoadingFirstPage || isSearching}
            isFetchingMore={isFetching}
            inviteContact={onInviteContact}
            viewContactDetail={redirectToContactDetails}
            searchDueGeneralScroll
            newContactButton={newGroupButton}
          />
        </div>
        <QRModal
          visible={qrModalIsVisible}
          onCancel={onCloseQR}
          qrValue={connectionToken}
          tPrefix="contacts"
        />
      </div>
    );
  }
);

Connections.propTypes = {
  qrModalIsVisible: PropTypes.bool.isRequired,
  connectionToken: PropTypes.string.isRequired,
  onCloseQR: PropTypes.func.isRequired,
  onInviteContact: PropTypes.func.isRequired
};

export default Connections;
