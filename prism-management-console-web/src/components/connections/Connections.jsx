import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import ContactActionsHeader from './Molecules/Header/ContactActionsHeader';
import ConnectionsTable from './Organisms/table/ConnectionsTable';
import QRModal from '../common/Organisms/Modals/QRModal/QRModal';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import { useSession } from '../../hooks/useSession';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';
import { useContactsPageStore } from '../../hooks/useContactsPageStore';
import { useRedirector } from '../../hooks/useRedirector';

import './_style.scss';

const Connections = observer(
  ({ qrModalIsVisible, connectionToken, onCloseQR, onInviteContact }) => {
    const { t } = useTranslation();
    const { redirectToContactDetails } = useRedirector();
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

    return (
      <div className="ConnectionsContainer Wrapper">
        {accountStatus === UNCONFIRMED && <WaitBanner />}
        <div className="ContentHeader">
          <div className="title">
            <h1>{t('contacts.title')}</h1>
          </div>
          <div className="ConnectionFilterWrapper">
            {accountStatus === CONFIRMED && (
              <ContactActionsHeader filterSortingProps={filterSortingProps} />
            )}
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
