import React from 'react';
import PropTypes from 'prop-types';
import { Badge, Dropdown, Menu } from 'antd';
import TemplateFilters from './TemplateFilters';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import FilterIcon from '../../../common/Atoms/Icons/FilterIconComponent';

const TemplatesAdditionalFilters = ({
  templateCategories,
  showDateFilters,
  filterSortingProps
}) => {
  const { hasAdditionalFiltersApplied } = filterSortingProps;

  const filtersMenu = (
    <Menu className="FiltersMenuContainer">
      <TemplateFilters
        templateCategories={templateCategories}
        showDateFilter={showDateFilters}
        {...filterSortingProps}
      />
    </Menu>
  );

  return (
    <Badge dot={hasAdditionalFiltersApplied} style={{ top: '1em', right: '1em', zIndex: 500 }}>
      <Dropdown.Button
        overlay={filtersMenu}
        trigger={['click']}
        className="SelectBtn theme-outline"
        icon={<FilterIcon />}
      />
    </Badge>
  );
};

TemplatesAdditionalFilters.defaultProps = {
  showDateFilters: false,
  hasAdditionalFiltersApplied: false
};

TemplatesAdditionalFilters.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
  showDateFilters: PropTypes.bool,
  filterSortingProps: PropTypes.shape({
    hasAdditionalFiltersApplied: PropTypes.bool,
    setFilterValue: PropTypes.func.isRequired
  }).isRequired
};

export default TemplatesAdditionalFilters;
