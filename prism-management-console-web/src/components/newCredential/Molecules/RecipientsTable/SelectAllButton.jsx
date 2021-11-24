import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Checkbox } from 'antd';
import { PulseLoader } from 'react-spinners';
import { checkboxPropShape } from '../../../../helpers/propShapes';

const SelectAllButton = ({ isLoadingSelection, selectedEntities, checkboxProps }) => {
  const { t } = useTranslation();

  const selectedLabel = selectedEntities.length ? `  (${selectedEntities.length})  ` : null;

  return (
    <div className="SelectAllCheckboxContainer">
      <Checkbox className="SelectAllCheckbox" {...checkboxProps}>
        {isLoadingSelection ? (
          <div className="LoadingSelection">
            <PulseLoader size={3} color="#FFAEB3" />
          </div>
        ) : (
          <span>
            {t('newCredential.targetsSelection.selectAll')}
            {selectedLabel}
          </span>
        )}
      </Checkbox>
    </div>
  );
};

SelectAllButton.defaultProps = {
  selectedEntities: []
};

SelectAllButton.propTypes = {
  isLoadingSelection: PropTypes.bool.isRequired,
  selectedEntities: PropTypes.arrayOf(PropTypes.string),
  checkboxProps: checkboxPropShape.isRequired
};

export default SelectAllButton;
