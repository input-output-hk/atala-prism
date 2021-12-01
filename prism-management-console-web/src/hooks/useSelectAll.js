import _ from 'lodash';
import { useState } from 'react';
import { getCheckedAndIndeterminateProps } from '../helpers/selectionHelpers';

// TODO: remove when all select-all cases are refactored
export const useSelectAll = ({
  displayedEntities,
  entitiesFetcher,
  entityKey,
  selectedEntities,
  setSelectedEntities,
  shouldSelectRecipients = true,
  isFetching
}) => {
  const [loadingSelection, setLoadingSelection] = useState(false);

  const handleSelectAll = async ev => {
    setLoadingSelection(true);
    const { checked } = ev.target;
    const entitiesToSelect = await entitiesFetcher();
    const filteredEntitiesToSelect = _.uniqBy(entitiesToSelect, e => e[entityKey]);
    handleSetSelection(checked, filteredEntitiesToSelect);
    setLoadingSelection(false);
  };

  const handleSetSelection = (checked, entitiesList) => {
    setSelectedEntities(checked ? entitiesList.map(e => e[entityKey]) : []);
  };

  const checkboxProps = {
    ...getCheckedAndIndeterminateProps(displayedEntities, selectedEntities),
    disabled: !shouldSelectRecipients || isFetching || !displayedEntities.length,
    onChange: handleSelectAll
  };

  return {
    loadingSelection,
    checkboxProps
  };
};
