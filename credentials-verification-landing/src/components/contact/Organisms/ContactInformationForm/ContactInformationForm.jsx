import React from 'react';
import { useTranslation } from 'react-i18next';
import { Input, Select } from 'antd';
import { noEmptyInput, emailFormatValidation } from '../../../../helpers/formRules';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';

import './_style.scss';

const { TextArea } = Input;
const { Option } = Select;
const inquiryTypeOptionsValues = ['Product', 'Commercial', 'General'];

const TemplateForm = React.forwardRef((_props, ref) => {
  const { t } = useTranslation();

  const getInput = (key, inputProps, extraRules, normalize) => {
    const rules = extraRules
      ? [noEmptyInput(t('errors.form.emptyField')), extraRules]
      : [noEmptyInput(t('errors.form.emptyField'))];

    return {
      fieldDecoratorData: {
        rules,
        normalize
      },
      label: t(`credential.contactInformation.form.${key}`),
      key,
      className: 'itemFormInfo',
      input: <Input placeholder="..." {...inputProps} />
    };
  };

  const InquiryTypeInput = {
    fieldDecoratorData: {
      rules: [noEmptyInput(t('errors.form.emptyField'))]
    },
    label: t(`credential.contactInformation.form.inquiryType`),
    key: 'inquiryType',
    className: 'itemFormInfo',
    input: (
      <Select placeholder="Select option">
        {inquiryTypeOptionsValues.map(key => (
          <Option key={key} value={key}>
            {t(`credential.contactInformation.form.inquiryTypeOptions.${key}`)}
          </Option>
        ))}
      </Select>
    )
  };

  const items = [
    getInput('fullName'),
    getInput(
      'email',
      {},
      {
        validator: (_, value, cb) => emailFormatValidation(value, cb),
        message: t('errors.email')
      },
      value => value && value.trim()
    ),
    InquiryTypeInput,
    { ...getInput('formMessage'), input: <TextArea rows={4} placeholder="Tell us more..." /> }
  ];

  return <CustomForm items={items} ref={ref} />;
});

export default TemplateForm;
