import React from 'react';
import { observer } from 'mobx-react-lite';
import TemplateFiltersContainer from '../Filters/TemplateFiltersContainer';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import { useTemplatesPageStore } from '../../../../hooks/useTemplatesPageStore';
import './_style.scss';

const ActionsHeader = observer(() => {
  const { filterSortingProps, templateCategories } = useTemplatesPageStore();
  return (
    <div className="ActionsHeader flex">
      <TemplateFiltersContainer
        templateCategories={templateCategories}
        showDateFilters
        {...filterSortingProps}
      />
      <CreateTemplateButton />
    </div>
  );
});

export default ActionsHeader;
