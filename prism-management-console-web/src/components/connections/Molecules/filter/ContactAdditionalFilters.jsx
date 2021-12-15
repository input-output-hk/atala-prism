import React from 'react';
import PropTypes from 'prop-types';
import { Badge, Dropdown, Menu } from 'antd';
import FilterIconComponent from '../../../common/Atoms/Icons/FilterIconComponent';
import ContactAdditionalFiltersMenu from './ContactAdditionalFiltersMenu';

const ContactAdditionalFilters = ({ filterSortingProps }) => {
  const { hasAdditionalFiltersApplied } = filterSortingProps;

  const filtersMenu = (
    <Menu className="FiltersMenuContainer">
      <ContactAdditionalFiltersMenu {...filterSortingProps} />
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

ContactAdditionalFilters.propTypes = {
  filterSortingProps: PropTypes.shape({
    hasAdditionalFiltersApplied: PropTypes.bool
  }).isRequired
};

export default ContactAdditionalFilters;
