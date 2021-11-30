import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import { Badge, Dropdown, Menu } from 'antd';
import { FilterOutlined } from '@ant-design/icons';
import TemplateFilters from './TemplateFilters';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import { useTemplatePageStore } from '../../../../hooks/useTemplatesPageStore';

const TemplateFiltersContainer = observer(({ showDateFilters }) => {
  const { t } = useTranslation();
  
  const { filterSortingProps } = useTemplatePageStore();
  const { nameFilter, hasAditionalFiltersApplied, setFilterValue } = filterSortingProps;

  const filtersMenu = (
    <Menu className="FiltersMenuContainer">
      <TemplateFilters showDateFilter={showDateFilters} />
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
          icon={<FilterOutlined />}
        />
      </Badge>
    </>
  );
});

TemplateFiltersContainer.defaultProps = {
  showDateFilters: false
};

TemplateFiltersContainer.propTypes = {
  showDateFilters: PropTypes.bool
};

export default TemplateFiltersContainer;
