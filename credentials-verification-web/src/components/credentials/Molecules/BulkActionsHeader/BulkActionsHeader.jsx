import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Checkbox } from 'antd';
import { PulseLoader } from 'react-spinners';
import CredentialButtons from '../../Atoms/Buttons/CredentialButtons';

const BulkActionsHeader = ({
  bulkActionsProps,
  loadingSelection,
  selectedLength,
  selectedRowKeys
}) => {
  const { t } = useTranslation();
  const { selectAll, indeterminateSelectAll, toggleSelectAll } = bulkActionsProps;
  return (
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
    toggleSelectAll: PropTypes.func,
    selectAll: PropTypes.bool,
    indeterminateSelectAll: PropTypes.bool
  }).isRequired,
  loadingSelection: PropTypes.bool,
  selectedLength: PropTypes.number,
  selectedRowKeys: PropTypes.arrayOf(PropTypes.string)
};

export default BulkActionsHeader;
