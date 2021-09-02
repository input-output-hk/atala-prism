import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { SELECT_TEMPLATE_CATEGORY } from '../../helpers/constants';
import CredentialTemplateCreation from './CredentialTemplateCreation';
import TemplateCreationStep from './Organisms/TemplateCreationStep';
import { useTemplatesInit } from '../../hooks/useTemplatesInit';
import { withTemplateSketchProvider } from '../providers/TemplateSketchContext';

const CredentialTemplateCreationContainer = observer(() => {
  useTemplatesInit();

  const [currentStep, setCurrentStep] = useState(SELECT_TEMPLATE_CATEGORY);
  return (
    <div className="TemplateMainContent">
      <CredentialTemplateCreation currentStep={currentStep} changeStep={setCurrentStep} />
      <TemplateCreationStep currentStep={currentStep} />
    </div>
  );
});

export default withTemplateSketchProvider(CredentialTemplateCreationContainer);
