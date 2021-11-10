import _ from 'lodash';
import { useState } from 'react';
import { getCheckedAndIndeterminateProps } from '../helpers/selectionHelpers';

const { MAX_CONTACTS } = require('../helpers/constants');

export const useSelectAllContacts = (contactsManager, setSelected) => async e => {
  if (e.target.checked) {
    const list = await contactsManager.getContacts(null, MAX_CONTACTS);
    setSelected(list.map(contact => contact.contactId));
  } else setSelected([]);
};

export const useSelectAllGroups = (groupsManager, setSelected) => async e => {
  if (e.target.checked) {
    const list = await groupsManager.getAllGroups();
    setSelected(list.map(group => group.name));
  } else setSelected([]);
};

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
