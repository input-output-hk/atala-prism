import React from 'react';
import TemplatePreview from '../../Molecules/DesignTemplateStep/TemplatePreview';
import TemplateSettings from '../../Molecules/DesignTemplateStep/TemplateSettings';
import '../../_style.scss';

const DesignTemplateStep = () => (
  <div className="DesignTemplateStep flex">
    <TemplatePreview />
    <TemplateSettings />
  </div>
);

export default DesignTemplateStep;
