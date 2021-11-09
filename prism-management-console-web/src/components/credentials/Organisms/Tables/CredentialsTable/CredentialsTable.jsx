import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import InfiniteScrollTable from '../../../../common/Organisms/Tables/InfiniteScrollTable';
import { credentialShape, emptyPropsShape } from '../../../../../helpers/propShapes';
import { CREDENTIALS_ISSUED, CREDENTIALS_RECEIVED } from '../../../../../helpers/constants';
import { useScrolledToBottom } from '../../../../../hooks/useScrolledToBottom';
import EmptyComponent from '../../../../common/Atoms/EmptyComponent/EmptyComponent';
import {
  getCredentialsIssuedColumns,
  getCredentialsReceivedColumns
} from '../../../../../helpers/tableDefinitions/credentials';
import './_style.scss';

const CredentialsTable = ({
  credentials,
  loading,
  getMoreData,
  isFetching,
  hasMore,
  onView,
  revokeSingleCredential,
  signSingleCredential,
  sendSingleCredential,
  selectionType,
  tab,
  emptyProps,
  searchDueGeneralScroll
}) => {
  const { t } = useTranslation();
  const [loadingRevokeSingle, setLoadingRevokeSingle] = useState();
  const [loadingSignSingle, setLoadingSignSingle] = useState();
  const [loadingSendSingle, setLoadingSendSingle] = useState();
  const { timesScrolledToBottom } = useScrolledToBottom(hasMore, loading, 'CredentialsTable');

  const [lastUpdated, setLastUpdated] = useState(timesScrolledToBottom);

  // leave this trigger for backward compatibility,
  // when all tables uses useScrolledToBottom remove searchDueGeneralScroll
  const handleGetMoreData = () => !searchDueGeneralScroll && getMoreData();

  useEffect(() => {
    if (timesScrolledToBottom !== lastUpdated && hasMore && searchDueGeneralScroll) {
      setLastUpdated(timesScrolledToBottom);
      getMoreData();
    }
  }, [timesScrolledToBottom, lastUpdated, hasMore, getMoreData, searchDueGeneralScroll]);

  const wrapRevokeSingleCredential = async credentialId => {
    setLoadingRevokeSingle(true);
    await revokeSingleCredential(credentialId);
    setLoadingRevokeSingle(false);
  };

  const wrapSignSingleCredential = async credentialId => {
    setLoadingSignSingle(true);
    await signSingleCredential(credentialId);
    setLoadingSignSingle(false);
  };

  const wrapSendSingleCredential = async credentialId => {
    setLoadingSendSingle(true);
    await sendSingleCredential(credentialId);
    setLoadingSendSingle(false);
  };

  const columns = {
    [CREDENTIALS_ISSUED]: getCredentialsIssuedColumns(
      onView,
      wrapRevokeSingleCredential,
      wrapSignSingleCredential,
      wrapSendSingleCredential,
      loadingRevokeSingle,
      loadingSignSingle,
      loadingSendSingle
    ),
    [CREDENTIALS_RECEIVED]: getCredentialsReceivedColumns(t('actions.view'), onView)
  };

  const tableClassName = {
    [CREDENTIALS_ISSUED]: 'credentialsIssued',
    [CREDENTIALS_RECEIVED]: 'credentialsReceived'
  };

  return (
    <div className={`CredentialsTable InfiniteScrollTableContainer ${tableClassName[tab]}`}>
      {emptyProps.isEmpty ? (
        <EmptyComponent {...emptyProps} />
      ) : (
        <InfiniteScrollTable
          columns={columns[tab]}
          data={credentials}
          loading={loading}
          getMoreData={handleGetMoreData}
          hasMore={hasMore}
          rowKey="credentialId"
          selectionType={selectionType}
          fetchingMore={isFetching}
          renderEmpty={() => <EmptyComponent {...emptyProps} />}
        />
      )}
    </div>
  );
};

CredentialsTable.defaultProps = {
  credentials: [],
  selectionType: null,
  searchDueGeneralScroll: false,
  isFetching: false,
  loading: false,
  revokeSingleCredential: null,
  signSingleCredential: null,
  sendSingleCredential: null
};

CredentialsTable.propTypes = {
  credentials: PropTypes.arrayOf(credentialShape),
  getMoreData: PropTypes.func.isRequired,
  isFetching: PropTypes.bool,
  loading: PropTypes.bool,
  hasMore: PropTypes.bool.isRequired,
  onView: PropTypes.func.isRequired,
  revokeSingleCredential: PropTypes.func,
  signSingleCredential: PropTypes.func,
  sendSingleCredential: PropTypes.func,
  selectionType: PropTypes.shape({
    selectedRowKeys: PropTypes.arrayOf(PropTypes.string),
    onChange: PropTypes.func
  }),
  tab: PropTypes.oneOf([CREDENTIALS_ISSUED, CREDENTIALS_RECEIVED]).isRequired,
  searchDueGeneralScroll: PropTypes.bool,
  emptyProps: emptyPropsShape.isRequired
};

export default CredentialsTable;
