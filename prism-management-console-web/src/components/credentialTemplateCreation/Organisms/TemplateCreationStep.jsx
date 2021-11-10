import React from 'react';
import { useTranslation } from 'react-i18next';
import {
  DESIGN_TEMPLATE,
  TEMPLATE_NAME_ICON_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../../helpers/constants';
import DesignTemplateStep from './DesignTemplateStep/DesignTemplateStep';
import TemplateNameIconCategoryStep from './NameIconCategoryStep/TemplateNameIconCategoryStep';
import SuccessBanner from '../../common/Molecules/SuccessPage/SuccessBanner';
import { templateCreationStepShape } from '../../../helpers/propShapes';
import { useRedirector } from '../../../hooks/useRedirector';

const TemplateCreationStep = ({ currentStep }) => {
  const { t } = useTranslation();
  const { redirectToCredentialTemplates } = useRedirector();

  switch (currentStep) {
    case DESIGN_TEMPLATE:
      return <DesignTemplateStep />;
    case TEMPLATE_CREATION_RESULT: {
      return (
        <SuccessBanner
          title={t('credentialTemplateCreation.step3.successTitle')}
          message={t('credentialTemplateCreation.step3.successMessage')}
          buttonText={t('credentialTemplateCreation.step3.continueButton')}
          onContinue={redirectToCredentialTemplates}
        />
      );
    }
    case TEMPLATE_NAME_ICON_CATEGORY:
    default:
      return <TemplateNameIconCategoryStep />;
  }
};

TemplateCreationStep.propTypes = {
  currentStep: templateCreationStepShape.isRequired
};

export default TemplateCreationStep;
