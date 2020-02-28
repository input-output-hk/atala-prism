import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Input } from 'antd';
import { noEmptyInput } from '../../helpers/formRules';
import CustomForm from '../common/Organisms/Forms/CustomForm';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const i18nPrefix = 'groupCreation.form.';

const getInput = (key, initialValue, t, onChange) => ({
  fieldDecoratorData: {
    rules: [noEmptyInput(t('errors.form.emptyField'))],
    initialValue
  },
  label: t(`${i18nPrefix}${key}`),
  key,
  className: '',
  input: <Input onChange={onChange} />
});

const GroupForm = React.forwardRef(({ ref, updateForm, groupName }) => {
  const { t } = useTranslation();

  const items = [getInput('groupName', groupName, t, ({ target: { value } }) => updateForm(value))];

  return <CustomForm items={items} ref={ref} />;
});

const GroupCreation = ({ createGroup, formRef, updateForm, formValues }) => {
  const { groupName } = formValues;
  const { t } = useTranslation();

  return (
    <div className="Wrapper">
      <div className="Header">
        <h1>{t('groupCreation.title')}</h1>
      </div>
      <div className="GroupCreationContent">
        <div className="box">
          <h3>Write a group name</h3>
          <GroupForm ref={formRef} updateForm={updateForm} formValues={formValues} />
          <CustomButton
            buttonProps={{ className: 'theme-outline', onClick: () => createGroup(groupName) }}
            buttonText={t('groupCreation.form.buttonText')}
          />
        </div>
      </div>
    </div>
  );
};

GroupForm.defaultProps = {
  groupName: ''
};

GroupForm.propTypes = {
  ref: PropTypes.shape().isRequired,
  updateForm: PropTypes.func.isRequired,
  groupName: PropTypes.string
};

GroupCreation.propTypes = {
  createGroup: PropTypes.func.isRequired,
  formRef: PropTypes.shape().isRequired,
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired
};

export default withRedirector(GroupCreation);
