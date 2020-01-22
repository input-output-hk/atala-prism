import React from 'react';
import { Col, Select } from 'antd';
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
    <Col xs={2} sm={2} md={2} lg={10}>
      <Select {...selectProps}>
        {languages.map(lang => (
          <Select.Option value={lang} disabled={lang === currentLanguage}>
            <img src={flags[lang]} alt={`${t(`languages.${lang}`)}-${t('languages.flag')}`} />
            {customUpperCase(t(`languages.${lang}`))}
          </Select.Option>
        ))}
      </Select>
    </Col>
  );
};

export default LanguageSelector;
