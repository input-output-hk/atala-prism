import React from 'react';
import { useTranslation } from 'gatsby-plugin-react-i18next';
import { DatePicker, Input } from 'antd';
import moment from 'moment';
import localeKa from 'moment/locale/ka';
import localeEn from 'moment/locale/en-gb';
import { noEmptyInput, futureDate } from '../../../../helpers/formRules';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import { getBrowserLanguage } from '../../../../helpers/languageUtils';

import './_style.scss';

const getInput = (key, t) => ({
  fieldDecoratorData: {
    rules: [noEmptyInput(t('errors.form.emptyField'))]
  },
  label: t(`credential.personalInformation.form.${key}`),
  key,
  className: 'itemFormInfo',
  input: <Input />
});

const PersonalInformationForm = React.forwardRef((_props, ref) => {
  const { t } = useTranslation();

  const locale = getBrowserLanguage() === 'en' ? localeEn : localeKa;

  const items = [
    getInput('firstName', t),
    {
      fieldDecoratorData: {},
      label: t('credential.personalInformation.form.lastName'),
      key: 'lastName',
      className: 'itemFormInfo',
      input: <Input size="large" disabled />
    },
    {
      fieldDecoratorData: {
        rules: [
          {
            validator: (_, value, cb) => futureDate(value, cb, moment.now()),
            message: t('errors.futureDate')
          }
        ]
      },
      label: t('credential.personalInformation.form.dateOfBirth'),
      key: 'dateOfBirth',
      className: 'DatePickerContainer',
      input: <DatePicker allowClear={false} locale={locale} />
    }
  ];

  return <CustomForm items={items} ref={ref} />;
});

export default PersonalInformationForm;
