import React, { useContext } from 'react';
import { observer } from 'mobx-react-lite';
import { Badge, Dropdown, Menu } from 'antd';
import { FilterOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import TemplateFilters from '../Filters/TemplateFilters';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import { UiStateContext } from '../../../../stores/ui/UiState';
import './_style.scss';

const ActionsHeader = observer(() => {
  const { t } = useTranslation();

  const { templateUiState } = useContext(UiStateContext);
  const { nameFilter, setFilterValue, hasAditionalFiltersApplied } = templateUiState;

  const filtersMenu = (
    <Menu className="FiltersMenuContainer">
      <TemplateFilters />
    </Menu>
  );

  return (
    <div className="ActionsHeader flex">
      <SearchBar
        searchText={nameFilter}
        setSearchText={value => setFilterValue('nameFilter', value)}
        placeholder={t('templates.actions.searchPlaceholder')}
      />
      <CreateTemplateButton />
      <Badge dot={hasAditionalFiltersApplied} style={{ top: '1em', right: '1em', zIndex: 500 }}>
        <Dropdown.Button
          overlay={filtersMenu}
          trigger={['click']}
          className="SelectBtn theme-outline"
          icon={<FilterOutlined />}
        />
      </Badge>
    </div>
  );
});

export default ActionsHeader;
