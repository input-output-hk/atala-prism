import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { message } from 'antd';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

const TemplatesActionButtons = ({ template }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => message.warn('Not implemented yet')
        }}
        buttonText={t('templates.actions.preview')}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => message.warn('Not implemented yet')
        }}
        buttonText={t('templates.actions.edit')}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => message.warn('Not implemented yet')
        }}
        buttonText={t('templates.actions.copy')}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => message.warn('Not implemented yet')
        }}
        buttonText={t('templates.actions.delete')}
      />
    </div>
  );
};

TemplatesActionButtons.propTypes = {
  // TODO: fix proptypes
};

export default TemplatesActionButtons;
