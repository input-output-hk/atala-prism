import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Input } from 'antd';
import StepCard from '../../Atoms/StepCard/StepCard';
import CustomForm from '../../../common/Organisms/Forms/CustomForm';
import {
  noEmptyInput,
  passwordValidation,
  passwordFormatValidation
} from '../../../../helpers/formRules';
import { refShape } from '../../../../helpers/propShapes';

import './_style.scss';

const getPassValue = (formRef, field) => formRef.current.getForm().getFieldValue(field);

const PasswordSetup = ({ password, passwordRef }) => {
  const { t } = useTranslation();

  const items = [
    {
      fieldDecoratorData: {
        rules: [
          noEmptyInput(t('errors.form.emptyField')),
          {
            validator: (_, value, cb) =>
              passwordValidation(value, cb, getPassValue(passwordRef, 'passwordConfirmation')),
            message: t('registration.password.passwordMissmatch')
          },
          {
            validator: (_rule, value, cb) => passwordFormatValidation(value, cb, t)
          }
        ],
        initialValue: password
      },
      label: t('registration.password.passwordInput'),
      key: 'password',
      className: '',
      input: <Input.Password />
    },
    {
      fieldDecoratorData: {
        rules: [
          noEmptyInput(t('errors.form.emptyField')),
          {
            validator: (_, value, cb) =>
              passwordValidation(value, cb, getPassValue(passwordRef, 'password')),
            message: t('registration.password.passwordMissmatch')
          },
          {
            validator: (_rule, value, cb) => passwordFormatValidation(value, cb, t)
          }
        ],
        initialValue: password
      },
      label: t('registration.password.passwordConfirmation'),
      key: 'passwordConfirmation',
      className: '',
      input: <Input.Password />
    }
  ];

  return (
    <div className="RegisterStep">
      <StepCard
        comment="registration.password.begin"
        title="registration.password.title"
        info="registration.password.info"
      />
      <CustomForm items={items} ref={passwordRef} />
    </div>
  );
};

PasswordSetup.defaultProps = {
  password: ''
};

PasswordSetup.propTypes = {
  password: PropTypes.string,
  passwordRef: refShape.isRequired
};

export default PasswordSetup;
