import React from 'react';
import TemplateCategorySection from './TemplateCategorySection';
import TemplateNameSection from './TemplateNameSection';
import TemplateIconSection from './TemplateIconSection';
import './_style.scss';

const TemplateNameIconCategoryStep = () => (
  <div className="TemplateNameIconCategoryStep flex">
    <div className="LeftPanel">
      <TemplateIconSection />
    </div>
    <div className="RighPanel">
      <TemplateNameSection />
      <TemplateCategorySection />
    </div>
  </div>
);

export default TemplateNameIconCategoryStep;
