import React from 'react';
import { useTranslation } from 'react-i18next';
import { DatePicker, Input, Col } from 'antd';
import moment from 'moment';
import PropTypes from 'prop-types';
import { noEmptyInput, futureDate, pastDate, minOneElement } from '../../../../helpers/formRules';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import FileUploader from '../../../common/Molecules/FileUploader/FileUploader';

import './_style.scss';

const getStartDate = formRef => formRef.current.getForm().getFieldValue('startDate');

const getInput = (key, initialValue, t) => ({
  fieldDecoratorData: {
    rules: [noEmptyInput(t('errors.form.emptyField'))],
    initialValue
  },
  label: t(`newCredential.form.${key}`),
  key,
  className: '',
  input: <Input size="large" />
});

const TemplateForm = React.forwardRef(
  (
    {
      savePicture,
      credentialValues: { award, degreeName, logoUniversity, startDate, graduationDate }
    },
    ref
  ) => {
    const { t } = useTranslation();

    const items = [
      getInput('degreeName', degreeName, t),
      getInput('award', award, t),
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
        input: <DatePicker allowClear={false} size="large" showToday={false} />
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
        input: <DatePicker allowClear={false} size="large" />
      },
      {
        fieldDecoratorData: {
          rules: [
            {
              validator: (_, value, cb) => minOneElement(value, cb),
              message: t('errors.form.emptyField')
            }
          ],
          initialValue: logoUniversity ? [logoUniversity] : []
        },
        label: t('newCredential.form.logoUniversity'),
        key: 'logoUniversity',
        className: '',
        input: (
          <FileUploader
            initialValue={logoUniversity}
            hint="newCredential.form.logoHint"
            field="logoUniversity"
            savePicture={savePicture}
            uploadText="newCredential.form.uploadButton"
            formRef={ref}
          />
        )
      }
    ];

    return <CustomForm items={items} ref={ref} />;
  }
);

TemplateForm.propTypes = {
  savePicture: PropTypes.func.isRequired,
  credentialValues: PropTypes.shape({
    startDate: PropTypes.instanceOf(moment),
    graduationDate: PropTypes.instanceOf(moment),
    logoUniversity: PropTypes.arrayOf(PropTypes.shape),
    degreeName: PropTypes.string,
    award: PropTypes.string
  }).isRequired
};

export default TemplateForm;
