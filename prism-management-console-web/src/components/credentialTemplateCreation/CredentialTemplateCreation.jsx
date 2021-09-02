import React, { useContext, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import { nanoid } from 'nanoid';
import GenericStepsButtons from '../common/Molecules/GenericStepsButtons/GenericStepsButtons';
import WizardTitle from '../common/Atoms/WizardTitle/WizardTitle';
import {
  DESIGN_TEMPLATE,
  NEW_TEMPLATE_STEP_UNIT,
  SELECT_TEMPLATE_CATEGORY,
  TEMPLATE_CREATION_RESULT
} from '../../helpers/constants';
import { withRedirector } from '../providers/withRedirector';
import { useTemplateSketchContext } from '../providers/TemplateSketchContext';
import { PrismStoreContext } from '../../stores/domain/PrismStore';
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
  const { form, templateSettings, templatePreview } = useTemplateSketchContext();
  const { addCredentialTemplate } = useContext(PrismStoreContext).templateStore;
  const [loadingNext, setLoadingNext] = useState(false);

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

  const createTemplate = async () => {
    setLoadingNext(true);
    const newTemplate = {
      ...templateSettings,
      template: templatePreview,
      category: templateSettings?.category,
      state: 1,
      id: nanoid()
    };
    await addCredentialTemplate(newTemplate);
    setLoadingNext(false);
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
      <GenericStepsButtons steps={steps} currentStep={currentStep} loading={loadingNext} />
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
