import React from 'react';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useRedirector } from '../../../../hooks/useRedirector';

const CreateContactButton = () => {
  const { t } = useTranslation();
  const { redirectToImportContacts } = useRedirector();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-outline',
          onClick: redirectToImportContacts
        }}
        buttonText={t('contacts.buttons.import')}
      />
    </div>
  );
};

export default CreateContactButton;
