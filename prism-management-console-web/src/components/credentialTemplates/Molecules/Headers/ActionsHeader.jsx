import React from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import TemplatesAdditionalFilters from '../Filters/TemplatesAdditionalFilters';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import { useTemplatesPageStore } from '../../../../hooks/useTemplatesPageStore';
import './_style.scss';

const ActionsHeader = observer(() => {
  const { t } = useTranslation();
  const { filterSortingProps, templateCategories } = useTemplatesPageStore();

  const { nameFilter, setFilterValue } = filterSortingProps;

  return (
    <div className="ActionsHeader flex">
      <SearchBar
        searchText={nameFilter}
        setSearchText={value => setFilterValue('nameFilter', value)}
        placeholder={t('templates.actions.searchPlaceholder')}
      />
      <CreateTemplateButton />
      <TemplatesAdditionalFilters
        showDateFilters
        templateCategories={templateCategories}
        filterSortingProps={filterSortingProps}
      />
    </div>
  );
});

export default ActionsHeader;
