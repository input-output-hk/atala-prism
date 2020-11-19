import React, { useState } from 'react';
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

const CredentialsIssued = ({
  showEmpty,
  tableProps,
  bulkActionsProps,
  showCredentialData,
  fetchCredentials,
  loadingSelection
}) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);

  const emptyProps = {
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: showEmpty,
    button: <CreateCredentialsButton />
  };

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
            t('credentials.actions.selectAll')
          )}{' '}
        </Checkbox>
        <CredentialButtons
          {...bulkActionsProps}
          disableSign={!selectedRowKeys?.length || loadingSelection}
          disableSend={!selectedRowKeys?.length || loadingSelection}
        />
      </div>
      <Row>
        {showEmpty || renderEmptyComponent ? (
          <EmptyComponent {...emptyProps} />
        ) : (
          <CredentialsTable getMoreData={getMoreData} loading={loading} {...expandedTableProps} />
        )}
      </Row>
    </>
  );
};

CredentialsIssued.defaultProps = {
  showEmpty: false
};

CredentialsIssued.propTypes = credentialTabShape;

export default CredentialsIssued;
