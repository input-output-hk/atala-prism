import React from 'react';
import { useTranslation } from 'react-i18next';
import { backgroundColors, themeColors } from '../../../../helpers/colors';
import { useTemplateContext } from '../../../providers/TemplateContext';
import ColorPicker from './ColorPicker';
import './_style.scss';

const ThemeOptions = () => {
  const { t } = useTranslation();
  const { templateSettings } = useTemplateContext();

  return (
    <>
      <div className="ColorPickerContainer">
        <h3>{t('credentialTemplateCreation.step2.style.themeOptions')}</h3>
        <ColorPicker
          name="themeColor"
          label={t('credentialTemplateCreation.step2.style.themeColor')}
          colors={themeColors}
          selected={templateSettings.themeColor}
        />
        <ColorPicker
          name="backgroundColor"
          label={t('credentialTemplateCreation.step2.style.backgroundColor')}
          colors={backgroundColors}
          selected={templateSettings.backgroundColor}
        />
      </div>
    </>
  );
};

export default ThemeOptions;
