import React from 'react';
import { Select } from 'antd';
import { useTranslation } from 'react-i18next';
import {
  getLanguages,
  getCurrentLanguage,
  changeLanguage
} from '../../../../helpers/languageUtils';
import { customUpperCase } from '../../../../helpers/genericHelpers';
import flagEn from '../../../../images/en.png';
import flagKa from '../../../../images/ge.png';

import './_style.scss';

const flags = {
  en: flagEn,
  ka: flagKa
};

const LanguageSelector = () => {
  const { t } = useTranslation();
  const languages = getLanguages();

  const currentLanguage = getCurrentLanguage();

  const selectProps = {
    size: 'large',
    onSelect: changeLanguage,
    defaultValue: currentLanguage
  };

  return (
    <div className="LanguajeMenu">
      <Select {...selectProps}>
        {languages.map(lang => (
          <Select.Option value={lang} disabled={lang === currentLanguage} key={`language-${lang}`}>
            <div className="LanguajeOption">
              <img src={flags[lang]} alt={`${t(`languages.${lang}`)}-${t('languages.flag')}`} />
              {customUpperCase(t(`languages.${lang}`))}
            </div>
          </Select.Option>
        ))}
      </Select>
    </div>
  );
};

export default LanguageSelector;
