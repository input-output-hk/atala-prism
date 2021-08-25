import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  DESIGN_TEMPLATE,
  SELECT_TEMPLATE_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../../helpers/constants';
import { useCredentialTypes, useTemplateCategories } from '../../../hooks/useCredentialTypes';
import DesignTemplateStep from './DesignTemplateStep/DesignTemplateStep';
import TemplateCategorySelectionStep from './CategoryStep/TemplateCategorySelector';
import SuccessBanner from '../../common/Molecules/SuccessPage/SuccessBanner';
import { withRedirector } from '../../providers/withRedirector';
import {
  credentialTypesManagerShape,
  templateCreationStepShape
} from '../../../helpers/propShapes';

const TemplateCreationStep = ({
  currentStep,
  credentialTypesManager,
  redirector: { redirectToCredentialTemplates }
}) => {
  const { t } = useTranslation();
  // TODO: remove when backend implements template categories
  const [mockedCategories, setMockedCategories] = useState([]);

  const { credentialTypes } = useCredentialTypes(credentialTypesManager);
  const { templateCategories } = useTemplateCategories(credentialTypesManager);

  // TODO: remove when backend implements template categories
  const handleAddMockedCategory = ({ categoryName, categoryIcon }) => {
    const logo = categoryIcon.isCustomIcon ? categoryIcon.file.thumbUrl : categoryIcon.src;
    const newCategory = { id: Math.random(), name: categoryName, logo, state: 1 };
    setMockedCategories(mockedCategories.concat(newCategory));
  };

  // TODO: remove when backend implements template categories
  const mockCategoriesProps = { mockedCategories, addMockedCategory: handleAddMockedCategory };

  switch (currentStep) {
    default:
    case SELECT_TEMPLATE_CATEGORY:
      return (
        <TemplateCategorySelectionStep
          templateCategories={templateCategories.concat(mockedCategories)}
          existingTemplates={credentialTypes}
          // TODO: remove when backend implements template categories
          mockCategoriesProps={mockCategoriesProps}
        />
      );
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
  credentialTypesManager: credentialTypesManagerShape,
  redirector: PropTypes.shape({
    redirectToCredentialTemplates: PropTypes.func.isRequired
  }).isRequired
};

export default withRedirector(TemplateCreationStep);
