import React from 'react';
import { useTranslation } from 'react-i18next';
import { DatePicker, Input } from 'antd';
import moment from 'moment';
import PropTypes from 'prop-types';
import localeKa from 'moment/locale/ka';
import localeEn from 'moment/locale/en-gb';
import { noEmptyInput, futureDate, pastDate, minOneElement } from '../../../../helpers/formRules';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import FileUploader from '../../../common/Molecules/FileUploader/FileUploader';

import './_style.scss';
import { getCurrentLanguage } from '../../../../helpers/languageUtils';
import { DEFAULT_DATE_FORMAT } from '../../../../helpers/constants';

const getStartDate = formRef => formRef.current.getForm().getFieldValue('startDate');

const getInput = (key, initialValue, t, onChange) => ({
  fieldDecoratorData: {
    rules: [noEmptyInput(t('errors.form.emptyField'))],
    initialValue
  },
  label: t(`newCredential.form.${key}`),
  key,
  className: '',
  input: <Input size="large" onChange={onChange} />
});

const TemplateForm = React.forwardRef(
  (
    { credentialValues: { degreeName, startDate, graduationDate }, updateExampleCredential },
    ref
  ) => {
    const { t } = useTranslation();

    const locale = getCurrentLanguage() === 'en' ? localeEn : localeKa;

    const items = [
      getInput('degreeName', degreeName, t, ({ target: { value } }) =>
        updateExampleCredential('degreeName', value)
      ),
      {
        fieldDecoratorData: {
          rules: [
            {
              validator: (_, value, cb) => futureDate(value, cb, moment.now()),
              message: t('newCredential.form.errors.futureError')
            }
          ],
          initialValue: startDate
        },
        label: t('newCredential.form.startDate'),
        key: 'startDate',
        className: 'DatePickerContainer firstElement',
        input: (
          <DatePicker
            onChange={({ _d }) => updateExampleCredential('startDate', 1000 * moment(_d).unix())}
            allowClear={false}
            size="large"
            showToday={false}
            format={DEFAULT_DATE_FORMAT}
            locale={locale}
          />
        )
      },
      {
        fieldDecoratorData: {
          rules: [
            {
              validator: (_, value, cb) => futureDate(value, cb, moment.now()),
              message: t('newCredential.form.errors.futureError')
            },
            {
              validator: (_, value, cb) => pastDate(value, cb, getStartDate(ref)),
              message: t('newCredential.form.errors.pastError')
            }
          ],
          initialValue: graduationDate
        },
        label: t('newCredential.form.graduationDate'),
        key: 'graduationDate',
        className: 'DatePickerContainer',
        input: (
          <DatePicker
            onChange={({ _d }) =>
              updateExampleCredential('graduationDate', 1000 * moment(_d).unix())
            }
            allowClear={false}
            format={DEFAULT_DATE_FORMAT}
            size="large"
            locale={locale}
          />
        )
      }
    ];

    return <CustomForm items={items} ref={ref} />;
  }
);

TemplateForm.propTypes = {
  credentialValues: PropTypes.shape({
    startDate: PropTypes.instanceOf(moment),
    graduationDate: PropTypes.instanceOf(moment),
    logoUniversity: PropTypes.arrayOf(PropTypes.shape),
    degreeName: PropTypes.string
  }).isRequired,
  updateExampleCredential: PropTypes.func.isRequired
};

export default TemplateForm;
