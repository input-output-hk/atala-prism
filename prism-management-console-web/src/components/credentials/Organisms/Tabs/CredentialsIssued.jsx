import React, { useState, useEffect } from 'react';
import { PropTypes } from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import CreateCredentialsButton from '../../Atoms/Buttons/CreateCredentialsButton';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CONFIRMED, CREDENTIALS_ISSUED } from '../../../../helpers/constants';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import BulkActionsHeader from '../../Molecules/BulkActionsHeader/BulkActionsHeader';
import { useSession } from '../../../../hooks/useSession';
import TableOptions from '../../Molecules/BulkActionsHeader/TableOptions';
import {
  useCredentialIssuedStore,
  useCredentialIssuedUiState
} from '../../../../hooks/useCredentialIssuedStore';

const CredentialsIssued = observer(
  ({
    selectionType,
    bulkActionsProps,
    showCredentialData,
    searchDueGeneralScroll,
    ...credentialsIssuedProps
  }) => {
    const { t } = useTranslation();
    const [selectedLength, setSelectedLength] = useState();

    const {
      credentials,
      isFetching,
      hasMore,
      fetchMoreData,
      isLoadingFirstPage
    } = useCredentialIssuedStore();
    const { hasFiltersApplied, isSearching, isSorting } = useCredentialIssuedUiState();

    const { accountStatus } = useSession();

    const { selectedRowKeys } = selectionType || {};

    useEffect(() => {
      const keys = Object.keys(selectedRowKeys);
      setSelectedLength(keys.length);
    }, [selectedRowKeys]);

    useEffect(() => {
      fetchMoreData();
    }, [fetchMoreData]);

    const expandedTableProps = {
      ...credentialsIssuedProps,
      selectionType,
      credentials,
      tab: CREDENTIALS_ISSUED,
      onView: showCredentialData,
      searchDueGeneralScroll,
      isFetching,
      hasMore,
      loading: isSearching || isSorting
    };

    const emptyProps = {
      photoSrc: noCredentialsPicture,
      model: t('credentials.title'),
      isFilter: hasFiltersApplied,
      button: accountStatus === CONFIRMED && <CreateCredentialsButton />
    };

    const renderContent = () => {
      if (isLoadingFirstPage) return <SimpleLoading size="md" />;
      return (
        <>
          <TableOptions bulkActionsProps={bulkActionsProps} selectedLength={selectedLength} />
          <CredentialsTable
            getMoreData={fetchMoreData}
            emptyProps={emptyProps}
            {...expandedTableProps}
          />
        </>
      );
    };

    return (
      <>
        <BulkActionsHeader
          bulkActionsProps={bulkActionsProps}
          selectedLength={selectedLength}
          selectedRowKeys={selectedRowKeys}
        />
        {renderContent()}
      </>
    );
  }
);

CredentialsIssued.defaultProps = {
  initialLoading: false
};

CredentialsIssued.propTypes = {
  credentials: PropTypes.arrayOf(PropTypes.shape()),
  selectionType: PropTypes.shape({
    selectedRowKeys: PropTypes.arrayOf(PropTypes.string)
  }),
  hasMore: PropTypes.bool,
  searching: PropTypes.bool,
  sortingProps: PropTypes.shape({
    sortingBy: PropTypes.string,
    setSortingBy: PropTypes.func,
    sortDirection: PropTypes.string,
    setSortDirection: PropTypes.func
  }).isRequired,
  bulkActionsProps: PropTypes.shape({
    signSelectedCredentials: PropTypes.func,
    sendSelectedCredentials: PropTypes.func,
    toggleSelectAll: PropTypes.func,
    selectAll: PropTypes.bool,
    indeterminateSelectAll: PropTypes.bool
  }).isRequired,
  showEmpty: PropTypes.bool
};

export default CredentialsIssued;
