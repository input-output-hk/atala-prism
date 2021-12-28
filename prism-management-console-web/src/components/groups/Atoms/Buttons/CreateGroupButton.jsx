import React from 'react';
import { useTranslation } from 'react-i18next';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useRedirector } from '../../../../hooks/useRedirector';

const CreateGroupButton = () => {
  const { t } = useTranslation();
  const { redirectToGroupCreation } = useRedirector();
  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          onClick: redirectToGroupCreation,
          className: 'theme-outline'
        }}
        buttonText={t('groups.createNewGroup')}
      />
    </div>
  );
};

export default CreateGroupButton;
