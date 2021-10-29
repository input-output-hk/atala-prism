import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import CredentialButtons from '../../Atoms/Buttons/CredentialButtons';
import { credentialTypeShape } from '../../../../helpers/propShapes';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';

const BulkActionsHeader = ({
  bulkActionsProps,
  loadingSelection,
  selectedRowKeys,
  filterProps
}) => {
  const { t } = useTranslation();
  const disableActions = !selectedRowKeys?.length || loadingSelection;

  return (
    <div className="BulkActionsRow">
      <SearchBar
        searchText={filterProps.name}
        setSearchText={filterProps.setName}
        placeholder={t('credentials.filters.search')}
      />
      <CredentialButtons
        {...bulkActionsProps}
        filterProps={filterProps}
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
      onChange: PropTypes.func
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
