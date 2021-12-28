import React from 'react';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useRedirector } from '../../../../hooks/useRedirector';
import './_style.scss';

const CredentialsButtons = () => {
  const { t } = useTranslation();
  const { redirectToNewCredential } = useRedirector();
  return (
    <div className="MainOption">
      <CustomButton
        buttonProps={{
          className: 'theme-outline',
          onClick: redirectToNewCredential
        }}
        buttonText={t('credentials.actions.createCredential')}
      />
    </div>
  );
};

export default CredentialsButtons;
