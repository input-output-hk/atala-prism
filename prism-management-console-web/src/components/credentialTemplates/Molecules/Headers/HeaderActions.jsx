import React from 'react';
import PropTypes from 'prop-types';
import { Badge, Dropdown, Menu } from 'antd';
import { FilterOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import TemplateFilters from '../Filters/TemplateFilters';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import { templateCategoryShape, templateFiltersShape } from '../../../../helpers/propShapes';

const HeaderActions = ({ filterProps, templateCategories }) => {
  const { t } = useTranslation();
  const hasFiltersApplied = filterProps.name || filterProps.category;

  const filtersMenu = (
    <Menu className="FiltersMenuContainer">
      <TemplateFilters filterProps={filterProps} templateCategories={templateCategories} />
    </Menu>
  );

  return (
    <>
      <SearchBar
        searchText={filterProps.name}
        setSearchText={filterProps.setName}
        placeholder={t('templates.table.columns.name')}
      />
      <CreateTemplateButton />
      <Badge dot={hasFiltersApplied} style={{ top: '1em', right: '1em', zIndex: 500 }}>
        <Dropdown.Button
          overlay={filtersMenu}
          trigger={['click']}
          className="SelectBtn theme-outline"
          icon={<FilterOutlined />}
        />
      </Badge>
    </>
  );
};

HeaderActions.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
  filterProps: PropTypes.shape(templateFiltersShape).isRequired
};

export default HeaderActions;
