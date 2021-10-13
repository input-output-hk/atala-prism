import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Checkbox } from 'antd';
import { PulseLoader } from 'react-spinners';
import { getCheckedAndIndeterminateProps } from '../../../../helpers/selectionHelpers';
import { contactShape, groupShape } from '../../../../helpers/propShapes';

const SelectAllButton = ({
  displayedEntities,
  entitiesFetcher,
  entityKey,
  selectedEntities,
  setSelectedEntities,
  shouldSelectRecipients
}) => {
  const { t } = useTranslation();
  const [loadingSelection, setLoadingSelection] = useState(false);

  const handleSelectAll = async ev => {
    setLoadingSelection(true);
    const { checked } = ev.target;
    const entitiesToSelect = await entitiesFetcher();
    handleSetSelection(checked, entitiesToSelect);
    setLoadingSelection(false);
  };

  const handleSetSelection = (checked, entitiesList) => {
    setSelectedEntities(checked ? entitiesList.map(e => e[entityKey]) : []);
  };

  const selectAllContactsProps = {
    ...getCheckedAndIndeterminateProps(displayedEntities, selectedEntities),
    disabled: !shouldSelectRecipients || !displayedEntities.length,
    onChange: handleSelectAll
  };

  return (
    <div className="SelectAllCheckboxContainer">
      <Checkbox className="SelectAllCheckbox" {...selectAllContactsProps}>
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
  displayedEntities: PropTypes.arrayOf(PropTypes.oneOfType([contactShape, groupShape])).isRequired,
  entitiesFetcher: PropTypes.func.isRequired,
  entityKey: PropTypes.string.isRequired,
  selectedEntities: PropTypes.arrayOf(PropTypes.string).isRequired,
  setSelectedEntities: PropTypes.func.isRequired,
  shouldSelectRecipients: PropTypes.bool.isRequired
};

export default SelectAllButton;
