import React from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from 'antd';
import PropTypes from 'prop-types';
import { noEmptyInput } from '../../../../helpers/formRules';
import CustomForm from '../../Organisms/Forms/CustomForm';

const i18nPrefix = 'credentialTemplateCreation.form';

const TemplateForm = ({ updateForm, formValues, ref }) => {
  const { t } = useTranslation();

  const { templateName } = formValues;

  const getInput = (key, initialValue, onChange) => ({
    fieldDecoratorData: {
      rules: [noEmptyInput(t('errors.form.emptyField'))],
      initialValue
    },
    key,
    className: '',
    input: <Input onChange={onChange} placeholder={t(`${i18nPrefix}.${key}`)} allowClear />
  });

  const items = [getInput('templateName', templateName, ({ target }) => updateForm(target.value))];

  return <CustomForm items={items} ref={ref} />;
};

TemplateForm.defaultProps = {
  formValues: {
    templateName: ''
  }
};

TemplateForm.propTypes = {
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape({
    templateName: PropTypes.string
  }),
  ref: PropTypes.shape().isRequired
};

export default TemplateForm;
