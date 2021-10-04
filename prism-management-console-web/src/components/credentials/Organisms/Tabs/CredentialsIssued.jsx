import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import CreateCredentialsButton from '../../Atoms/Buttons/CreateCredentialsButton';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CONFIRMED, CREDENTIALS_ISSUED } from '../../../../helpers/constants';
import { credentialTabShape } from '../../../../helpers/propShapes';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import BulkActionsHeader from '../../Molecules/BulkActionsHeader/BulkActionsHeader';
import { useSession } from '../../../../hooks/useSession';
import TableOptions from '../../Molecules/BulkActionsHeader/TableOptions';

const CredentialsIssued = observer(
  ({
    tableProps,
    bulkActionsProps,
    showCredentialData,
    fetchCredentials,
    loadingSelection,
    initialLoading,
    searchDueGeneralScroll,
    filterProps,
    credentialTypes
  }) => {
    const { t } = useTranslation();
    const [loading, setLoading] = useState(false);
    const [selectedLength, setSelectedLength] = useState();

    const { accountStatus } = useSession();

    const { name, date, credentialType, credentialStatus, contactStatus } = filterProps;
    const { credentials, selectionType, searching, sortingProps } = tableProps;
    const { selectedRowKeys } = selectionType || {};

    useEffect(() => {
      const keys = Object.keys(selectedRowKeys);
      setSelectedLength(keys.length);
    }, [selectedRowKeys]);

    const getMoreData = useCallback(async () => {
      setLoading(true);
      await fetchCredentials();
      setLoading(false);
    }, [fetchCredentials]);

    const expandedTableProps = {
      ...tableProps,
      tab: CREDENTIALS_ISSUED,
      onView: showCredentialData,
      searchDueGeneralScroll
    };

    const emptyProps = {
      photoSrc: noCredentialsPicture,
      model: t('credentials.title'),
      isFilter: name || date || credentialType || credentialStatus || contactStatus,
      button: accountStatus === CONFIRMED && <CreateCredentialsButton />
    };

    const renderContent = () => {
      if (initialLoading || searching) return <SimpleLoading size="md" />;
      if (!credentials.length) return <EmptyComponent {...emptyProps} />;
      return (
        <>
          <TableOptions
            bulkActionsProps={bulkActionsProps}
            loadingSelection={loadingSelection}
            selectedLength={selectedLength}
            sortingProps={sortingProps}
          />
          <CredentialsTable getMoreData={getMoreData} loading={loading} {...expandedTableProps} />;
        </>
      );
    };

    return (
      <>
        <BulkActionsHeader
          bulkActionsProps={bulkActionsProps}
          loadingSelection={loadingSelection}
          selectedLength={selectedLength}
          selectedRowKeys={selectedRowKeys}
          filterProps={{ ...filterProps, credentialTypes }}
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
