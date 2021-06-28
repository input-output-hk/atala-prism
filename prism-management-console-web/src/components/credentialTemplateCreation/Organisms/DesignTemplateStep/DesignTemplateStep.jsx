import React from 'react';
import TemplatePreview from '../../Molecules/DesignTemplateStep/TemplatePreview';
import TemplateSettings from '../../Molecules/DesignTemplateStep/TemplateSettings';
import { antdV4FormShape } from '../../../../helpers/propShapes';

const DesignTemplateStep = ({ form }) => (
  <div className="flex">
    <TemplatePreview />
    <TemplateSettings />
  </div>
);

DesignTemplateStep.propTypes = {
  form: antdV4FormShape.isRequired
};

export default DesignTemplateStep;
