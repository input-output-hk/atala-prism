import React, { useEffect } from 'react';
import { PropTypes } from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CREDENTIALS_RECEIVED } from '../../../../helpers/constants';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import {
  useCredentialReceivedStore,
  useCredentialReceivedUiState
} from '../../../../hooks/useCredentialReceivedStore';

const CredentialsReceived = observer(({ showEmpty, showCredentialData }) => {
  const { t } = useTranslation();
  const {
    credentials,
    isFetching,
    hasMore,
    fetchMoreData,
    isLoadingFirstPage
  } = useCredentialReceivedStore();
  const { hasFiltersApplied } = useCredentialReceivedUiState({
    reset: true
  });

  useEffect(() => {
    fetchMoreData();
  }, [fetchMoreData]);

  const expandedTableProps = {
    credentials,
    getMoreData: fetchMoreData,
    tab: CREDENTIALS_RECEIVED,
    onView: showCredentialData,
    searchDueGeneralScroll: true,
    hasMore,
    loading: isFetching
  };

  const emptyProps = {
    isEmpty: !credentials.length || showEmpty,
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: hasFiltersApplied
  };

  if (isLoadingFirstPage) return <SimpleLoading size="md" />;
  return <CredentialsTable {...expandedTableProps} emptyProps={emptyProps} />;
});

CredentialsReceived.defaultProps = {
  showEmpty: false
};

CredentialsReceived.propTypes = {
  showEmpty: PropTypes.bool,
  showCredentialData: PropTypes.func.isRequired
};

export default CredentialsReceived;
