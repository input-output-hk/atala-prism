import React, { forwardRef } from 'react';
import { Input } from 'antd';
import { useTranslation } from 'react-i18next';
import { noEmptyInput } from '../../../../helpers/formRules';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';

const LoginForm = forwardRef((_, ref) => {
  const { t } = useTranslation();

  const items = [
    {
      fieldDecoratorData: {
        rules: [noEmptyInput(t('login.form.emptyPassword'))]
      },
      label: t('login.form.password'),
      key: 'password',
      className: '',
      input: <Input.Password visibilityToggle={false} />
    }
  ];

  return <CustomForm items={items} ref={ref} />;
});

export default LoginForm;
