import React from 'react';
import PropTypes from 'prop-types';
import { Badge, Dropdown, Menu } from 'antd';
import FilterIconComponent from '../../../common/Atoms/Icons/FilterIconComponent';
import GroupAdditionalFiltersMenu from './GroupAdditionalFiltersMenu';

const GroupAdditionalFilters = ({ filterSortingProps }) => {
  const { hasAdditionalFiltersApplied, setFilterValue } = filterSortingProps;

  const filtersMenu = (
    <Menu className="FiltersMenuContainer">
      <GroupAdditionalFiltersMenu setFilterValue={setFilterValue} />
    </Menu>
  );

  return (
    <Badge dot={hasAdditionalFiltersApplied} style={{ top: '1em', right: '1em', zIndex: 500 }}>
      <Dropdown.Button
        overlay={filtersMenu}
        trigger={['click']}
        className="FiltersButton theme-outline"
        icon={<FilterIconComponent />}
      />
    </Badge>
  );
};

GroupAdditionalFilters.propTypes = {
  filterSortingProps: PropTypes.shape({
    hasAdditionalFiltersApplied: PropTypes.bool,
    setFilterValue: PropTypes.func.isRequired
  }).isRequired
};

export default GroupAdditionalFilters;
