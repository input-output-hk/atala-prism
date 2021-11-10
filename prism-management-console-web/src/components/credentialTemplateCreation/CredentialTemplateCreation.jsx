import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import GenericStepsButtons from '../common/Molecules/GenericStepsButtons/GenericStepsButtons';
import WizardTitle from '../common/Atoms/WizardTitle/WizardTitle';
import {
  DESIGN_TEMPLATE,
  NEW_TEMPLATE_STEP_UNIT,
  TEMPLATE_NAME_ICON_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../helpers/constants';
import { useTemplateSketch } from '../../hooks/useTemplateSketch';
import { useRedirector } from '../../hooks/useRedirector';
import './_style.scss';

const fieldsByStep = {
  [TEMPLATE_NAME_ICON_CATEGORY]: ['name', 'category'],
  [DESIGN_TEMPLATE]: [
    'layout',
    'themeColor',
    'backgroundColor',
    'credentialTitle',
    'credentialSubtitle',
    'credentialBody'
  ]
};

const CredentialTemplateCreation = observer(({ currentStep, changeStep }) => {
  const { t } = useTranslation();
  const { redirectToCredentialTemplates } = useRedirector();

  const { form, createTemplateFromSketch } = useTemplateSketch();
  const [loadingNext, setLoadingNext] = useState(false);

  const validateByStep = () =>
    form
      .validateFields()
      .then(() => ({ errors: [] }))
      .catch(({ errorFields }) => {
        const stepFields = fieldsByStep[currentStep];
        const partialErrorsFields = errorFields.filter(errField =>
          stepFields.includes(...errField.name)
        );
        const errors = partialErrorsFields.map(errorField => errorField.errors);
        return { errors };
      });

  const advanceStep = () => changeStep(currentStep + NEW_TEMPLATE_STEP_UNIT);

  const validateAndAdvance = async () => (await validate()) && advanceStep();

  const validate = async () => {
    const { errors } = await validateByStep();
    const hasPartialErrors = Boolean(errors.length);
    if (hasPartialErrors) errors.forEach(msg => message.error(t(msg)));
    return !hasPartialErrors;
  };

  const createTemplateAndAdvance = async () => {
    setLoadingNext(true);
    const isPartiallyValid = await validate();
    if (isPartiallyValid) {
      await createTemplateFromSketch();
      advanceStep();
    }
    setLoadingNext(false);
  };

  const goBack = () => changeStep(currentStep - NEW_TEMPLATE_STEP_UNIT);

  const steps = [
    { key: '0', back: redirectToCredentialTemplates, next: validateAndAdvance },
    { key: '1', back: goBack, next: createTemplateAndAdvance },
    { key: '2', back: goBack, next: redirectToCredentialTemplates }
  ];

  const defaultStepText = {
    title: t(`credentialTemplateCreation.step${currentStep + 1}.title`),
    subtitle: t(`credentialTemplateCreation.step${currentStep + 1}.subtitle`)
  };

  const getStepText = {
    [TEMPLATE_NAME_ICON_CATEGORY]: defaultStepText,
    [DESIGN_TEMPLATE]: defaultStepText,
    [TEMPLATE_CREATION_RESULT]: {}
  };

  return (
    <div className="TitleContainer">
      <GenericStepsButtons steps={steps} currentStep={currentStep} loading={loadingNext} />
      <WizardTitle {...getStepText[currentStep]} />
    </div>
  );
});

CredentialTemplateCreation.propTypes = {
  currentStep: PropTypes.number.isRequired,
  changeStep: PropTypes.func.isRequired
};

export default CredentialTemplateCreation;
