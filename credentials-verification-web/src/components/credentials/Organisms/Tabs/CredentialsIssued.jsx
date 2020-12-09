import React, { useState, useEffect } from 'react';
import { Checkbox, Row } from 'antd';
import { useTranslation } from 'react-i18next';
import { PulseLoader } from 'react-spinners';
import CreateCredentialsButton from '../../Atoms/Buttons/CreateCredentialsButton';
import CredentialButtons from '../../Atoms/Buttons/CredentialButtons';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CREDENTIALS_ISSUED } from '../../../../helpers/constants';
import { credentialTabShape } from '../../../../helpers/propShapes';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';

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

  const emptyProps = {
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: showEmpty,
    button: <CreateCredentialsButton />
  };

  useEffect(() => {
    const keys = Object.keys(selectedRowKeys);
    setSelectedLength(keys.length);
  }, [tableProps.selectionType.selectedRowKeys]);

  const { credentials, selectionType } = tableProps;
  const { selectedRowKeys } = selectionType || {};
  const { selectAll, indeterminateSelectAll, toggleSelectAll } = bulkActionsProps;

  const expandedTableProps = {
    ...tableProps,
    tab: CREDENTIALS_ISSUED,
    onView: showCredentialData
  };

  const getMoreData = () => {
    setLoading(true);
    return fetchCredentials().finally(() => setLoading(false));
  };

  const renderEmptyComponent = !credentials.length || showEmpty;

  const renderContent = () => {
    if (initialLoading && !loading) return <SimpleLoading size="md" />;
    if (renderEmptyComponent) return <EmptyComponent {...emptyProps} />;
    return <CredentialsTable getMoreData={getMoreData} loading={loading} {...expandedTableProps} />;
  };

  return (
    <>
      <div className="bulkActionsRow">
        <Checkbox
          indeterminate={indeterminateSelectAll}
          className="checkboxCredential"
          onChange={toggleSelectAll}
          checked={selectAll}
          disabled={loadingSelection}
        >
          {loadingSelection ? (
            <PulseLoader size={3} color="#FFAEB3" />
          ) : (
            <span>
              {t('credentials.actions.selectAll')}
              {selectedLength ? `  (${selectedLength})  ` : null}
            </span>
          )}
        </Checkbox>
        <CredentialButtons
          {...bulkActionsProps}
          disableSign={!selectedRowKeys?.length || loadingSelection}
          disableSend={!selectedRowKeys?.length || loadingSelection}
        />
      </div>
      {renderContent()}
    </>
  );
};

CredentialsIssued.defaultProps = {
  initialLoading: false
};

CredentialsIssued.propTypes = credentialTabShape;

export default CredentialsIssued;
