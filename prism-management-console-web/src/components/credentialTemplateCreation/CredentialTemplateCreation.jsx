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
import { antdV4FormShape } from '../../helpers/propShapes';
import { useTemplateContext } from '../providers/TemplateContext';
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
  renderStep,
  redirector: { redirectToCredentialTemplates }
}) => {
  const { t } = useTranslation();
  const { form } = useTemplateContext();

  const validateByStep = () =>
    form.validateFields().catch(({ errorFields }) => {
      const stepFields = fieldsByStep[currentStep];
      const partialErrorsFields = errorFields.filter(errField =>
        stepFields.includes(...errField.name)
      );
      return { errors: partialErrorsFields.map(errorField => errorField.errors) };
    });

  const goToDesignCredential = async () => {
    // TODO: implement design template
    // if (selectedCategory) changeStep(DESIGN_TEMPLATE);
    const { errors } = await validateByStep(SELECT_TEMPLATE_CATEGORY);
    const isPartiallyValid = !errors;
    if (isPartiallyValid) message.warn(t('errors.notImplementedYet'));
    else errors.map(msg => message.error(t(msg)));
  };

  const createTemplates = async () => {
    const stepValidation = await validateByStep(DESIGN_TEMPLATE);
    const isPartiallyValid = !stepValidation;
    if (isPartiallyValid) changeStep(TEMPLATE_CREATION_RESULT);
    else stepValidation.map(msg => message.error(t(msg)));
  };

  const goBack = () => changeStep(currentStep - NEW_TEMPLATE_STEP_UNIT);

  const steps = [
    { back: redirectToCredentialTemplates, next: goToDesignCredential },
    { back: goBack, next: createTemplates },
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
    <React.Fragment>
      <div className="TemplateMainContent">
        <div className="TitleContainer">
          <GenericStepsButtons steps={steps} currentStep={currentStep} />
          <WizardTitle {...getStepText[currentStep]} />
        </div>
        {renderStep()}
      </div>
    </React.Fragment>
  );
};

CredentialTemplateCreation.propTypes = {
  currentStep: PropTypes.number.isRequired,
  changeStep: PropTypes.func.isRequired,
  renderStep: PropTypes.func.isRequired,
  redirector: PropTypes.shape({ redirectToCredentialTemplates: PropTypes.func }).isRequired,
  form: antdV4FormShape.isRequired
};

export default withRedirector(CredentialTemplateCreation);
