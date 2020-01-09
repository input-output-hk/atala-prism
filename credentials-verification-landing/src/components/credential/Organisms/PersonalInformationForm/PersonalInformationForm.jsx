import React from 'react';
import { useTranslation } from 'react-i18next';
import { DatePicker, Input } from 'antd';
import moment from 'moment';
import localeKa from 'moment/locale/ka';
import localeEn from 'moment/locale/en-gb';
import { noEmptyInput, futureDate, pastDate } from '../../../../helpers/formRules';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';

import { getBrowserLanguage } from '../../../../helpers/languageUtils';

const getStartDate = formRef => formRef.current.getForm().getFieldValue('startDate');

const getInput = (key, t) => ({
  fieldDecoratorData: {
    rules: [noEmptyInput(t('errors.form.emptyField'))]
  },
  label: t(`credential.personalInformation.form.${key}`),
  key,
  className: '',
  input: <Input size="large" />
});

const TemplateForm = React.forwardRef((_props, ref) => {
  const { t } = useTranslation();

  const locale = getBrowserLanguage() === 'en' ? localeEn : localeKa;

  const items = [
    getInput('name', t),
    getInput('lastName', t),
    // getInput('universityName', t),
    // getInput('degreeName', t),
    getInput('award', t),
    {
      fieldDecoratorData: {
        rules: [
          {
            validator: (_, value, cb) => futureDate(value, cb, moment.now()),
            message: t('errors.futureError')
          }
        ]
      },
      label: t('credential.personalInformation.form.startDate'),
      key: 'startDate',
      className: 'DatePickerContainer firstElement',
      input: <DatePicker allowClear={false} size="large" showToday={false} locale={locale} />
    },
    {
      fieldDecoratorData: {
        rules: [
          {
            validator: (_, value, cb) => futureDate(value, cb, moment.now()),
            message: t('errors.futureError')
          },
          {
            validator: (_, value, cb) => pastDate(value, cb, getStartDate(ref)),
            message: t('errors.pastError')
          }
        ]
      },
      label: t('credential.personalInformation.form.graduationDate'),
      key: 'graduationDate',
      className: 'DatePickerContainer',
      input: <DatePicker allowClear={false} size="large" locale={locale} />
    }
  ];

  return <CustomForm items={items} ref={ref} />;
});

export default TemplateForm;
