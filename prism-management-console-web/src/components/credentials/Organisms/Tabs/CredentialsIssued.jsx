import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import CreateCredentialsButton from '../../Atoms/Buttons/CreateCredentialsButton';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CONFIRMED, CREDENTIALS_ISSUED } from '../../../../helpers/constants';
import { credentialTabShape } from '../../../../helpers/propShapes';
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
    tableProps,
    bulkActionsProps,
    showCredentialData,
    initialLoading,
    searchDueGeneralScroll
  }) => {
    const { t } = useTranslation();
    const [selectedLength, setSelectedLength] = useState();
    const { credentials, isFetching, fetchMoreData } = useCredentialIssuedStore();
    const { hasFiltersApplied, isSearching, isSorting } = useCredentialIssuedUiState();

    const { accountStatus } = useSession();

    const { selectionType } = tableProps;

    const { selectedRowKeys } = selectionType || {};

    useEffect(() => {
      const keys = Object.keys(selectedRowKeys);
      setSelectedLength(keys.length);
    }, [selectedRowKeys]);

    useEffect(() => {
      fetchMoreData();
    }, [fetchMoreData]);

    const expandedTableProps = {
      ...tableProps,
      credentials,
      tab: CREDENTIALS_ISSUED,
      onView: showCredentialData,
      searchDueGeneralScroll,
      isFetching,
      loading: isSearching || isSorting
    };

    const emptyProps = {
      photoSrc: noCredentialsPicture,
      model: t('credentials.title'),
      isFilter: hasFiltersApplied,
      button: accountStatus === CONFIRMED && <CreateCredentialsButton />
    };

    const renderContent = () => {
      if (initialLoading) return <SimpleLoading size="md" />;
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
  initialLoading: false,
  searchDueGeneralScroll: false
};

CredentialsIssued.propTypes = credentialTabShape;

export default CredentialsIssued;
