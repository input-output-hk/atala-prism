import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CredentialButtons from '../../Atoms/Buttons/CredentialButtons';
import { credentialTypeShape } from '../../../../helpers/propShapes';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import { useCredentialIssuedUiState } from '../../../../hooks/useCredentialIssuedStore';

const BulkActionsHeader = ({ bulkActionsProps, selectedRowKeys }) => {
  const { t } = useTranslation();
  const disableActions =
    !selectedRowKeys?.length || bulkActionsProps.selectAllProps.loadingSelection;

  const { nameFilter, setFilterValue } = useCredentialIssuedUiState();

  return (
    <div className="BulkActionsRow">
      <SearchBar
        searchText={nameFilter}
        setSearchText={value => setFilterValue('searchTextFilter', value)}
        placeholder={t('credentials.filters.searchTextPlaceholder')}
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
    signSelectedCredentials: PropTypes.func,
    sendSelectedCredentials: PropTypes.func,
    selectAllProps: PropTypes.shape({
      checked: PropTypes.bool,
      indeterminate: PropTypes.bool,
      disabled: PropTypes.bool,
      onChange: PropTypes.func,
      loadingSelection: PropTypes.bool
    })
  }).isRequired,
  loadingSelection: PropTypes.bool,
  selectedLength: PropTypes.number,
  selectedRowKeys: PropTypes.arrayOf(PropTypes.string),
  filterProps: PropTypes.shape({
    name: PropTypes.string,
    setName: PropTypes.func,
    credentialTypes: PropTypes.arrayOf(credentialTypeShape),
    credentialType: PropTypes.string,
    setCredentialType: PropTypes.func,
    credentialStatus: PropTypes.number,
    setCredentialStatus: PropTypes.func,
    contactStatus: PropTypes.string,
    setContactStatus: PropTypes.func,
    date: PropTypes.string,
    setDate: PropTypes.func
  }).isRequired
};

export default BulkActionsHeader;
