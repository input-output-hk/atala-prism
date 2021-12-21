import React from 'react';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { useRedirector } from '../../../../hooks/useRedirector';

const CreateGroupButton = () => {
  const { t } = useTranslation();
  const { redirectToGroupCreation } = useRedirector();
  return (
    <CustomButton
      buttonProps={{
        onClick: redirectToGroupCreation,
        className: 'theme-outline',
        icon: <PlusOutlined />
      }}
      buttonText={t('groups.createNewGroup')}
    />
  );
};

export default CreateGroupButton;
