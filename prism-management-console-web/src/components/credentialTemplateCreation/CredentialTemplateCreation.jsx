import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import GenericStepsButtons from '../common/Molecules/GenericStepsButtons/GenericStepsButtons';
import WizardTitle from '../common/Atoms/WizardTitle/WizardTitle';
import {
  DESIGN_TEMPLATE,
  NEW_TEMPLATE_STEP_UNIT,
  SELECT_TEMPLATE_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../helpers/constants';
import { withRedirector } from '../providers/withRedirector';
import { useTemplateContext } from '../providers/TemplateContext';
import { useMockDataContext } from '../providers/MockDataProvider';
import './_style.scss';

const fieldsByStep = {
  [SELECT_TEMPLATE_CATEGORY]: ['name', 'category'],
  [DESIGN_TEMPLATE]: [
    'layout',
    'themeColor',
    'backgroundColor',
    'credentialTitle',
    'credentialSubtitle',
    'credentialBody'
  ]
};

const CredentialTemplateCreation = ({
  currentStep,
  changeStep,
  redirector: { redirectToCredentialTemplates }
}) => {
  const { t } = useTranslation();
  const { form, templateSettings, templatePreview } = useTemplateContext();
  const { mockDataDispatch } = useMockDataContext();

  const validateByStep = () =>
    form.validateFields().catch(({ errorFields }) => {
      const stepFields = fieldsByStep[currentStep];
      const partialErrorsFields = errorFields.filter(errField =>
        stepFields.includes(...errField.name)
      );
      return { errors: partialErrorsFields.map(errorField => errorField.errors) };
    });

  const advanceStep = async () => {
    const { errors } = await validateByStep(currentStep);
    const isPartiallyValid = !errors;
    if (isPartiallyValid) changeStep(currentStep + NEW_TEMPLATE_STEP_UNIT);
    else errors.map(msg => message.error(t(msg)));
  };

  const goBack = () => changeStep(currentStep - NEW_TEMPLATE_STEP_UNIT);

  const createTemplate = () => {
    const newTemplate = {
      ...templateSettings,
      template: templatePreview,
      category: parseInt(templateSettings?.category, 10)
    };
    mockDataDispatch({
      type: 'ADD_MOCK_CREDENTIAL_TEMPLATE',
      payload: { newTemplate }
    });
    advanceStep();
  };

  const steps = [
    { back: redirectToCredentialTemplates, next: advanceStep },
    { back: goBack, next: createTemplate },
    { back: goBack, next: redirectToCredentialTemplates }
  ];

  const defaultStepText = {
    title: t(`credentialTemplateCreation.step${currentStep + 1}.title`),
    subtitle: t(`credentialTemplateCreation.step${currentStep + 1}.subtitle`)
  };

  const getStepText = {
    [SELECT_TEMPLATE_CATEGORY]: defaultStepText,
    [DESIGN_TEMPLATE]: defaultStepText,
    [TEMPLATE_CREATION_RESULT]: {}
  };

  return (
    <div className="TitleContainer">
      <GenericStepsButtons steps={steps} currentStep={currentStep} />
      <WizardTitle {...getStepText[currentStep]} />
    </div>
  );
};

CredentialTemplateCreation.propTypes = {
  currentStep: PropTypes.number.isRequired,
  changeStep: PropTypes.func.isRequired,
  redirector: PropTypes.shape({ redirectToCredentialTemplates: PropTypes.func }).isRequired
};

export default withRedirector(CredentialTemplateCreation);
