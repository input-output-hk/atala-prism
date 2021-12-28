import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { GROUP_SORTING_KEYS, GROUP_SORTING_KEYS_TRANSLATOR } from '../../../../helpers/constants';
import SortControls from '../../../common/Molecules/Sorting/SortControls';

import './_style.scss';

const GroupsPageTableOptions = ({ filterSortingProps }) => {
  const { t } = useTranslation();

  const sortingOptions = Object.values(GROUP_SORTING_KEYS).map(column => ({
    key: column,
    label: t(`groups.filters.${GROUP_SORTING_KEYS_TRANSLATOR[column]}`)
  }));

  return (
    <div className="GroupsPageTableOptions">
      <div className="LeftOptions">
        <SortControls options={sortingOptions} {...filterSortingProps} />
      </div>
    </div>
  );
};

GroupsPageTableOptions.propTypes = {
  filterSortingProps: PropTypes.shape({
    sortDirection: PropTypes.string.isRequired,
    toggleSortDirection: PropTypes.func.isRequired,
    sortingBy: PropTypes.string.isRequired,
    setSortingBy: PropTypes.func.isRequired
  }).isRequired
};

export default GroupsPageTableOptions;
