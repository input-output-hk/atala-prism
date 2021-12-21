import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import GroupAdditionalFilters from '../Filters/GroupAdditionalFilters';
import CreateGroupButton from '../../Atoms/Buttons/CreateGroupButton';

import './_style.scss';

const GroupActionsHeader = ({ filterSortingProps }) => {
  const { t } = useTranslation();

  const { setFilterValue } = filterSortingProps;

  return (
    <div className="FilterControls">
      <div className="GroupFilters">
        <SearchBar
          setSearchText={textValue => setFilterValue('nameFilter', textValue)}
          placeholder={t('contacts.filters.search')}
        />
        <CreateGroupButton />
        <GroupAdditionalFilters filterSortingProps={filterSortingProps} />
      </div>
    </div>
  );
};

GroupActionsHeader.propTypes = {
  filterSortingProps: PropTypes.shape({
    hasAdditionalFiltersApplied: PropTypes.bool,
    setFilterValue: PropTypes.func.isRequired
  }).isRequired
};

export default GroupActionsHeader;
