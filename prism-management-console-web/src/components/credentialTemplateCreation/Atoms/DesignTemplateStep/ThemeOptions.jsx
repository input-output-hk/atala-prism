import React from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import { backgroundColors, themeColors } from '../../../../helpers/colors';
import { useTemplateCreationStore } from '../../../../hooks/useTemplatesPageStore';
import ColorPicker from './ColorPicker';
import './_style.scss';

const ThemeOptions = observer(() => {
  const { t } = useTranslation();
  const { templateSketch } = useTemplateCreationStore();

  return (
    <>
      <div className="ColorPickerContainer">
        <h3>{t('credentialTemplateCreation.step2.style.themeOptions')}</h3>
        <ColorPicker
          name="themeColor"
          label={t('credentialTemplateCreation.step2.style.themeColor')}
          colors={themeColors}
          selected={templateSketch.themeColor}
        />
        <ColorPicker
          name="backgroundColor"
          label={t('credentialTemplateCreation.step2.style.backgroundColor')}
          colors={backgroundColors}
          selected={templateSketch.backgroundColor}
        />
      </div>
    </>
  );
});

export default ThemeOptions;
