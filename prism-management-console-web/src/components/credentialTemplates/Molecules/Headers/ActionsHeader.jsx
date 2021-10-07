import React from 'react';
import { observer } from 'mobx-react-lite';
import TemplateFiltersContainer from '../Filters/TemplateFiltersContainer';
import './_style.scss';

const ActionsHeader = observer(() => (
  <div className="ActionsHeader flex">
    <TemplateFiltersContainer showDateFilters />
  </div>
));

export default ActionsHeader;
