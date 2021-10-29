import React from 'react';
import { observer } from 'mobx-react-lite';
import TemplateFiltersContainer from '../Filters/TemplateFiltersContainer';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import './_style.scss';

const ActionsHeader = observer(() => (
  <div className="ActionsHeader flex">
    <TemplateFiltersContainer showDateFilters />
    <CreateTemplateButton />
  </div>
));

export default ActionsHeader;
