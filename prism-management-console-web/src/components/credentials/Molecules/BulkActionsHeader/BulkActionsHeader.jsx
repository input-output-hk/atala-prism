import React from 'react';
import PropTypes from 'prop-types';
import CredentialButtons from '../../Atoms/Buttons/CredentialButtons';
import SearchBar from '../../Atoms/Buttons/SearchBar';
import { credentialTypeShape } from '../../../../helpers/propShapes';

const BulkActionsHeader = ({
  bulkActionsProps,
  loadingSelection,
  selectedRowKeys,
  filterProps
}) => {
  const disableActions = !selectedRowKeys?.length || loadingSelection;

  return (
    <div className="BulkActionsRow">
      <SearchBar {...bulkActionsProps} filterProps={filterProps} />
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
    credentialTypes: PropTypes.arrayOf(PropTypes.shape(credentialTypeShape)),
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
