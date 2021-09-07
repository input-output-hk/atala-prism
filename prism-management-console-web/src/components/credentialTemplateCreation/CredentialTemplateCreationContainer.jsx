import React, { useState } from 'react';
import { observer } from 'mobx-react-lite';
import { SELECT_TEMPLATE_CATEGORY } from '../../helpers/constants';
import CredentialTemplateCreation from './CredentialTemplateCreation';
import TemplateCreationStep from './Organisms/TemplateCreationStep';
import { withTemplateSketchProvider } from '../providers/TemplateSketchContext';
import { useTemplateStore } from '../../hooks/useStore';

const CredentialTemplateCreationContainer = observer(() => {
  useTemplateStore({ fetch: true });

  const [currentStep, setCurrentStep] = useState(SELECT_TEMPLATE_CATEGORY);
  return (
    <div className="TemplateMainContent">
      <CredentialTemplateCreation currentStep={currentStep} changeStep={setCurrentStep} />
      <TemplateCreationStep currentStep={currentStep} />
    </div>
  );
});

export default withTemplateSketchProvider(CredentialTemplateCreationContainer);
