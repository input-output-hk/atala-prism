import React from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from 'antd';
import PropTypes from 'prop-types';
import { noEmptyInput } from '../../../../helpers/formRules';
import CustomForm from '../../Organisms/Forms/CustomForm';
import { refPropShape } from '../../../../helpers/propShapes';

const i18nPrefix = 'groupCreation.form';

const GroupForm = ({ updateForm, formValues, formRef }) => {
  const { t } = useTranslation();

  const { groupName } = formValues;

  const getInput = (key, initialValue, onChange) => ({
    fieldDecoratorData: {
      rules: [noEmptyInput(t('errors.form.emptyField'))],
      initialValue
    },
    key,
    className: '',
    input: <Input onChange={onChange} placeholder={t(`${i18nPrefix}.${key}`)} allowClear />
  });

  const items = [getInput('groupName', groupName, ({ target }) => updateForm(target.value))];

  return <CustomForm items={items} ref={formRef} />;
};

GroupForm.defaultProps = {
  formValues: {
    groupName: ''
  }
};

GroupForm.propTypes = {
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape({
    groupName: PropTypes.string
  }),
  formRef: refPropShape.isRequired
};

export default GroupForm;
