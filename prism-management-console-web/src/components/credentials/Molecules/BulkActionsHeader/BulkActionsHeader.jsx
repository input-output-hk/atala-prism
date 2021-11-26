import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CredentialButtons from '../../Atoms/Buttons/CredentialButtons';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import { useCredentialsIssuedPageStore } from '../../../../hooks/useCredentialsIssuedPageStore';

const BulkActionsHeader = ({ bulkActionsProps, selectedLength }) => {
  const { t } = useTranslation();

  const {
    isLoadingSelection,
    filterSortingProps: { textFilter, setFilterValue }
  } = useCredentialsIssuedPageStore();

  const disableActions = !selectedLength || isLoadingSelection;

  return (
    <div className="BulkActionsRow">
      <SearchBar
        searchText={textFilter}
        setSearchText={value => setFilterValue('textFilter', value)}
        placeholder={t('credentials.filters.textFilterPlaceholder')}
      />
      <CredentialButtons
        {...bulkActionsProps}
        disableRevoke={disableActions}
        disableSign={disableActions}
        disableSend={disableActions}
      />
    </div>
  );
};

BulkActionsHeader.defaultProps = {
  loadingSelection: false,
  selectedLength: 0,
  selectedRowKeys: []
};

BulkActionsHeader.propTypes = {
  bulkActionsProps: PropTypes.shape({
    revokeSelectedCredentials: PropTypes.func,
    signSelectedCredentials: PropTypes.func,
    sendSelectedCredentials: PropTypes.func
  }).isRequired,
  selectedLength: PropTypes.number
};

export default BulkActionsHeader;
