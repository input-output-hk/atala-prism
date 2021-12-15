import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Badge, Dropdown, Menu } from 'antd';
import TemplateFilters from './TemplateFilters';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import FilterIconComponent from '../../../common/Atoms/Icons/FilterIconComponent';

const TemplateFiltersContainer = ({
  templateCategories,
  showDateFilters,
  ...filterSortingProps
}) => {
  const { t } = useTranslation();

  const { nameFilter, hasAditionalFiltersApplied, setFilterValue } = filterSortingProps;

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
    <>
      <SearchBar
        searchText={nameFilter}
        setSearchText={value => setFilterValue('nameFilter', value)}
        placeholder={t('templates.actions.searchPlaceholder')}
      />
      <Badge dot={hasAditionalFiltersApplied} style={{ top: '1em', right: '1em', zIndex: 500 }}>
        <Dropdown.Button
          overlay={filtersMenu}
          trigger={['click']}
          className="SelectBtn theme-outline"
          icon={<FilterIconComponent />}
        />
      </Badge>
    </>
  );
};

TemplateFiltersContainer.defaultProps = {
  showDateFilters: false
};

TemplateFiltersContainer.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
  nameFilter: PropTypes.string.isRequired,
  hasAditionalFiltersApplied: PropTypes.bool.isRequired,
  setFilterValue: PropTypes.func.isRequired,
  showDateFilters: PropTypes.bool
};

export default TemplateFiltersContainer;
