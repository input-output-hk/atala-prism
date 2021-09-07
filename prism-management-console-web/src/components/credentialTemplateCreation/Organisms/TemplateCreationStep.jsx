import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  DESIGN_TEMPLATE,
  SELECT_TEMPLATE_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../../helpers/constants';
import DesignTemplateStep from './DesignTemplateStep/DesignTemplateStep';
import TemplateCategorySelectionStep from './CategoryStep/TemplateCategorySelector';
import SuccessBanner from '../../common/Molecules/SuccessPage/SuccessBanner';
import { withRedirector } from '../../providers/withRedirector';
import { templateCreationStepShape } from '../../../helpers/propShapes';

const TemplateCreationStep = ({ currentStep, redirector: { redirectToCredentialTemplates } }) => {
  const { t } = useTranslation();

  switch (currentStep) {
    default:
    case SELECT_TEMPLATE_CATEGORY:
      return <TemplateCategorySelectionStep />;
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
  }
};

TemplateCreationStep.propTypes = {
  currentStep: templateCreationStepShape,
  redirector: PropTypes.shape({
    redirectToCredentialTemplates: PropTypes.func.isRequired
  }).isRequired
};

export default withRedirector(TemplateCreationStep);
