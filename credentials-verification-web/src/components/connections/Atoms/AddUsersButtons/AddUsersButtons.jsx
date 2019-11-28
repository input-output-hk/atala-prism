import React from 'react';
import { useTranslation } from 'react-i18next';
import { Icon } from 'antd';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import Logger from '../../../../helpers/Logger';

const AddUsersButtons = () => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-outline',
          onClick: () => Logger.info('placeholder function')
        }}
        buttonText={t('connections.buttons.bulk')}
        icon={<Icon type="plus" />}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-secondary',
          onClick: () => Logger.info('placeholder function')
        }}
        buttonText={t('connections.buttons.manual')}
        icon={<Icon type="plus" />}
      />
    </div>
  );
};

export default AddUsersButtons;
