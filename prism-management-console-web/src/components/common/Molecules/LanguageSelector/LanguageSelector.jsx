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
    <div className="LanguageMenu">
      <Select {...selectProps}>
        {languages.map(lang => {
          const language = t(`languages.${lang}`);
          return (
            <Select.Option
              value={lang}
              disabled={lang === currentLanguage}
              key={`language-${lang}`}
            >
              <div className="LanguageOption">
                <img src={flags[lang]} alt={`${language}-${t('languages.flag')}`} />
                {customUpperCase(language)}
              </div>
            </Select.Option>
          );
        })}
      </Select>
    </div>
  );
};

export default LanguageSelector;
