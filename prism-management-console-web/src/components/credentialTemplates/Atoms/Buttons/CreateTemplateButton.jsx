import React from 'react';
import { useTranslation } from 'react-i18next';
import { useRedirector } from '../../../../hooks/useRedirector';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const CreateTemplatesButtons = () => {
  const { t } = useTranslation();
  const { redirectToCredentialTemplateCreation } = useRedirector();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-outline',
          onClick: redirectToCredentialTemplateCreation
        }}
        buttonText={t('templates.actions.createTemplate')}
      />
    </div>
  );
};

export default CreateTemplatesButtons;
