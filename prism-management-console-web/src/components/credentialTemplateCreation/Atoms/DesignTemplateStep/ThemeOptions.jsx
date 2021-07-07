import React from 'react';
import { useTranslation } from 'react-i18next';
import { useTemplateContext } from '../../../providers/TemplateContext';
import ColorPicker from './ColorPicker';
import './_style.scss';

const themeColors = [
  '#D8D8D8',
  '#FF2D3B',
  '#40EAEA',
  '#2BB5B5',
  '#2ACA9A',
  '#0C8762',
  '#EC2691',
  '#FFD100',
  '#FF8718',
  '#AF2DFF',
  '#4A2DFF',
  '#011727',
  '#808080'
];

const backgroundColors = ['#FFFFFF', '#0C8762', '#2ACA9A', '#79CEB5', '#D8D8D8', '#000000'];

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
