import React from 'react';
import { useTranslation } from 'react-i18next';
import { useTemplateContext } from '../../../providers/TemplateContext';
import ColorPicker from './ColorPicker';

const themeColors = ['#FF2D3B', '#40EAEA', '#2BB5B5', '#2ACA9A'];
const backgroundColors = ['#0C8762', '#2ACA9A', '#79CEB5'];

const ThemeOptions = () => {
  const { t } = useTranslation();
  const { templateSettings } = useTemplateContext();

  return (
    <>
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
    </>
  );
};

ThemeOptions.propTypes = {};

export default ThemeOptions;
