import React, { useEffect } from 'react';
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
import { useCredentialsIssuedPageStore } from '../../../../hooks/useCredentialsIssuedPageStore';

const CredentialsIssued = observer(({ credentialActionsProps, showCredentialData }) => {
  const { t } = useTranslation();
  const {
    filterSortingProps: { hasFiltersApplied },
    isSearching,
    credentials,
    selectedCredentials,
    handleCherryPickSelection,
    isFetching,
    hasMore,
    fetchMoreData,
    isLoadingFirstPage,
    initCredentialsIssuedStore
  } = useCredentialsIssuedPageStore();

  const { accountStatus } = useSession();

  const { bulkActionsProps } = credentialActionsProps;

  useEffect(() => {
    initCredentialsIssuedStore();
  }, [initCredentialsIssuedStore]);

  const selectedLength = selectedCredentials.length;

  const expandedTableProps = {
    ...credentialActionsProps,
    selectionType: {
      selectedRowKeys: selectedCredentials,
      type: 'checkbox',
      onChange: handleCherryPickSelection
    },
    credentials,
    tab: CREDENTIALS_ISSUED,
    onView: showCredentialData,
    isFetching,
    hasMore,
    loading: isSearching
  };

  const emptyProps = {
    photoSrc: noCredentialsPicture,
    model: t('credentials.title'),
    isFilter: hasFiltersApplied,
    button: accountStatus === CONFIRMED && <CreateCredentialsButton />
  };

  return (
    <>
      <BulkActionsHeader bulkActionsProps={bulkActionsProps} selectedLength={selectedLength} />
      {isLoadingFirstPage ? (
        <SimpleLoading size="md" />
      ) : (
        <>
          <TableOptions bulkActionsProps={bulkActionsProps} selectedLength={selectedLength} />
          <CredentialsTable
            getMoreData={fetchMoreData}
            emptyProps={emptyProps}
            {...expandedTableProps}
          />
        </>
      )}
    </>
  );
});

CredentialsIssued.defaultProps = {
  initialLoading: false
};

CredentialsIssued.propTypes = {
  credentialActionsProps: {
    revokeSingleCredential: PropTypes.func,
    signSingleCredential: PropTypes.func,
    sendSingleCredential: PropTypes.func,
    bulkActionsProps: PropTypes.shape({
      revokeSelectedCredentials: PropTypes.func,
      signSelectedCredentials: PropTypes.func,
      sendSelectedCredentials: PropTypes.func
    }).isRequired
  },
  showCredentialData: PropTypes.func.isRequired
};

export default CredentialsIssued;
