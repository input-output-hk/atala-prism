import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { message } from 'antd';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { credentialTypeShape } from '../../../../helpers/propShapes';

const TemplatesActionButtons = ({ template, onPreview }) => {
  const { t } = useTranslation();

  const actions = [
    {
      name: 'preview',
      call: () => onPreview(template)
    },
    { name: 'edit' },
    { name: 'copy' },
    { name: 'delete' }
  ];

  const defaultAction = () => message.warn(t('errors.notImplementedYet'));

  return (
    <div className="ControlButtons">
      {actions.map(a => (
        <CustomButton
          buttonProps={{
            className: 'theme-link',
            onClick: a.call || defaultAction
          }}
          buttonText={t(`templates.actions.${a.name}`)}
        />
      ))}
    </div>
  );
};

TemplatesActionButtons.propTypes = {
  template: credentialTypeShape.isRequired,
  onPreview: PropTypes.func.isRequired
};

export default TemplatesActionButtons;
