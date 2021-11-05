import React from 'react';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import CredentialsTable from '../Tables/CredentialsTable/CredentialsTable';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import noCredentialsPicture from '../../../../images/noCredentials.svg';
import { CREDENTIALS_RECEIVED } from '../../../../helpers/constants';
import { credentialTabShape } from '../../../../helpers/propShapes';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import {
  useCredentialReceivedStore,
  useCredentialReceivedUiState
} from '../../../../hooks/useCredentialReceivedStore';

const CredentialsReceived = observer(({ showEmpty, showCredentialData, initialLoading }) => {
  const { t } = useTranslation();

  const { displayedCredentials, hasFiltersApplied } = useCredentialReceivedUiState({
    reset: true
  });
  const {
    credentials: credentialsReceived,
    fetchCredentialsNextPage: fetchCredentialsReceivedNextPage,
    getCredentialsToSelect: getCredentialsReceivedToSelect,
    refreshCredentials: refreshCredentialsReceived,
    isFetching: isFetchingCredentialsReceived,
    isLoadingFirstPage: isLoadingReceived
  } = useCredentialReceivedStore();

  const emptyProps = {
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: hasFiltersApplied
  };

  const expandedTableProps = {
    credentials: displayedCredentials,
    getMoreData: fetchCredentialsReceivedNextPage,
    tab: CREDENTIALS_RECEIVED,
    onView: showCredentialData
  };

  const renderEmptyComponent = !displayedCredentials.length || showEmpty;

  const renderContent = () => {
    if (!displayedCredentials.length && initialLoading) return <SimpleLoading size="md" />;
    if (renderEmptyComponent) return <EmptyComponent {...emptyProps} />;
    return <CredentialsTable {...expandedTableProps} />;
  };

  return renderContent();
});

CredentialsReceived.defaultProps = {
  showEmpty: false,
  initialLoading: false
};

CredentialsReceived.propTypes = credentialTabShape;

export default CredentialsReceived;
