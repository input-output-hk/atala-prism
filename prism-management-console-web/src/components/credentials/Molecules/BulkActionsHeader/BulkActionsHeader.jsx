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
  const { selectAllProps } = bulkActionsProps;
  const disableActions = !selectedRowKeys?.length || loadingSelection;

  return (
    <div className="BulkActionsRow">
      <Checkbox className="checkboxCredential" {...selectAllProps}>
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
  selectedRowKeys: PropTypes.arrayOf(PropTypes.string)
};

export default BulkActionsHeader;
