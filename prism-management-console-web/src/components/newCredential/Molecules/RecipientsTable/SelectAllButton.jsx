import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Checkbox } from 'antd';
import { PulseLoader } from 'react-spinners';
import { checkboxPropShape } from '../../../../helpers/propShapes';

const SelectAllButton = ({ loadingSelection, selectedEntities, checkboxProps }) => {
  const { t } = useTranslation();

  return (
    <div className="SelectAllCheckboxContainer">
      <Checkbox className="SelectAllCheckbox" {...checkboxProps}>
        {loadingSelection ? (
          <div className="LoadingSelection">
            <PulseLoader size={3} color="#FFAEB3" />
          </div>
        ) : (
          <span>
            {t('newCredential.targetsSelection.selectAll')}
            {selectedEntities.length ? `  (${selectedEntities.length})  ` : null}
          </span>
        )}
      </Checkbox>
    </div>
  );
};

SelectAllButton.propTypes = {
  loadingSelection: PropTypes.bool.isRequired,
  selectedEntities: PropTypes.arrayOf(PropTypes.string).isRequired,
  checkboxProps: checkboxPropShape.isRequired
};

export default SelectAllButton;
