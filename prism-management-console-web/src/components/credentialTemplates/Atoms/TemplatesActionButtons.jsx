import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { message } from 'antd';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';
import { credentialTypeShape } from '../../../helpers/propShapes';

const TemplatesActionButtons = ({ template, onPreview }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          className: 'theme-link',
          onClick: () => onPreview(template)
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
  template: PropTypes.shape(credentialTypeShape).isRequired,
  onPreview: PropTypes.func.isRequired
};

export default TemplatesActionButtons;
