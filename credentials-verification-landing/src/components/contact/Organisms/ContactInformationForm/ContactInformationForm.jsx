import React from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from 'antd';
import { noEmptyInput, emailFormatValidation } from '../../../../helpers/formRules';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';

import './_style.scss';

const { TextArea } = Input;

const TemplateForm = React.forwardRef((_props, ref) => {
  const { t } = useTranslation();

  const getInput = (key, inputProps, extraRules) => {
    const rules = extraRules
      ? [noEmptyInput(t('errors.form.emptyField')), extraRules]
      : [noEmptyInput(t('errors.form.emptyField'))];

    return {
      fieldDecoratorData: {
        rules
      },
      label: t(`credential.contactInformation.form.${key}`),
      key,
      className: 'itemFormInfo',
      input: <Input size="large" placeholder="..." {...inputProps} />
    };
  };

  const items = [
    getInput('fullName'),
    getInput(
      'email',
      {},
      {
        validator: (_, value, cb) => emailFormatValidation(value, cb),
        message: t('errors.email')
      }
    ),
    { ...getInput('formMessage'), input: <TextArea rows={4} placeholder="Tell us more..." /> }
  ];

  return <CustomForm items={items} ref={ref} />;
});

export default TemplateForm;
