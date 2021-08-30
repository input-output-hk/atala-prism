import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { Badge, Dropdown, Menu } from 'antd';
import { FilterOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import TemplateFilters from '../Filters/TemplateFilters';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import { templateCategoryShape } from '../../../../helpers/propShapes';
import { UiStateContext } from '../../../../stores/ui/UiState';
import './_style.scss';

const ActionsHeader = ({ templateCategories }) => {
  const { t } = useTranslation();
  const { nameFilter, setNameFilter, hasFiltersApplied } = useContext(UiStateContext).templateUiState;

  const filtersMenu = (
    <Menu className="FiltersMenuContainer">
      <TemplateFilters templateCategories={templateCategories} />
    </Menu>
  );
  return (
    <div className="ActionsHeader flex">
      <SearchBar
        searchText={nameFilter}
        setSearchText={setNameFilter}
        placeholder={t('templates.actions.searchPlaceholder')}
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
    </div>
  );
};

ActionsHeader.propTypes = {
  templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired
};

export default ActionsHeader;
