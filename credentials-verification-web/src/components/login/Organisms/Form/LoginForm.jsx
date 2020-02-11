import React, { forwardRef } from 'react';
import { Input } from 'antd';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { noEmptyInput } from '../../../../helpers/formRules';
import { ENTER } from '../../../../helpers/constants';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';

const LoginForm = forwardRef(({ handleLogin }, ref) => {
  const { t } = useTranslation();

  const items = [
    {
      fieldDecoratorData: {
        rules: [noEmptyInput(t('login.form.emptyPassword'))]
      },
      label: t('login.form.password'),
      key: 'password',
      className: '',
      input: (
        <Input.Password
          onKeyUp={({ key }) => {
            if (key === ENTER) handleLogin();
          }}
          visibilityToggle={false}
        />
      )
    }
  ];

  return <CustomForm items={items} ref={ref} />;
});

LoginForm.propTypes = {
  handleLogin: PropTypes.func.isRequired
};

export default LoginForm;
