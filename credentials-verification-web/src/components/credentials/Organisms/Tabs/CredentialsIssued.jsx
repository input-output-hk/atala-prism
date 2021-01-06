import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import CreateCredentialsButton from '../../Atoms/Buttons/CreateCredentialsButton';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CREDENTIALS_ISSUED } from '../../../../helpers/constants';
import { credentialTabShape } from '../../../../helpers/propShapes';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import BulkActionsHeader from '../../Molecules/BulkActionsHeader/BulkActionsHeader';
import { useSession } from '../../../providers/SessionContext';

const CredentialsIssued = ({
  showEmpty,
  tableProps,
  bulkActionsProps,
  showCredentialData,
  fetchCredentials,
  loadingSelection,
  initialLoading
}) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [selectedLength, setSelectedLength] = useState();

  const { accountIsConfirmed } = useSession();

  useEffect(() => {
    const keys = Object.keys(selectedRowKeys);
    setSelectedLength(keys.length);
  }, [tableProps.selectionType.selectedRowKeys]);

  const getMoreData = () => {
    setLoading(true);
    return fetchCredentials().finally(() => setLoading(false));
  };

  const { credentials, selectionType, searching } = tableProps;
  const { selectedRowKeys } = selectionType || {};

  const expandedTableProps = {
    ...tableProps,
    tab: CREDENTIALS_ISSUED,
    onView: showCredentialData
  };

  const emptyProps = {
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: !showEmpty && !credentials.length,
    button: showEmpty && accountIsConfirmed && <CreateCredentialsButton />
  };

  const renderEmptyComponent = !credentials.length || showEmpty;

  const renderContent = () => {
    if (!credentials.length && (initialLoading || searching)) return <SimpleLoading size="md" />;
    if (renderEmptyComponent) return <EmptyComponent {...emptyProps} />;
    return <CredentialsTable getMoreData={getMoreData} loading={loading} {...expandedTableProps} />;
  };

  return (
    <>
      {!renderEmptyComponent && (
        <BulkActionsHeader
          bulkActionsProps={bulkActionsProps}
          loadingSelection={loadingSelection}
          selectedLength={selectedLength}
          selectedRowKeys={selectedRowKeys}
        />
      )}
      {renderContent()}
    </>
  );
};

CredentialsIssued.defaultProps = {
  initialLoading: false
};

CredentialsIssued.propTypes = credentialTabShape;

export default CredentialsIssued;
