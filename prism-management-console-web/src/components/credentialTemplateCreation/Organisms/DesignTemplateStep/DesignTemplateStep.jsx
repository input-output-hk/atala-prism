import React from 'react';
import TemplatePreview from '../../Molecules/DesignTemplateStep/TemplatePreview';
import TemplateSettings from '../../Molecules/DesignTemplateStep/TemplateSettings';

const DesignTemplateStep = () => (
  <div className="flex">
    <TemplatePreview />
    <TemplateSettings />
  </div>
);

export default DesignTemplateStep;
