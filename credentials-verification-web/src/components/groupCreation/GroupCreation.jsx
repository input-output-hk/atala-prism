import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Input, Checkbox } from 'antd';
import { noEmptyInput } from '../../helpers/formRules';
import CustomForm from '../common/Organisms/Forms/CustomForm';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

import ContactDataContainer from './contactDataContainer';

const { Search } = Input;

const onSearch = value => console.log(value);
function Change(e) {
  console.log(`checked = ${e.target.checked}`);
}

const i18nPrefix = 'groupCreation.form.';

const getInput = (key, initialValue, t, onChange) => ({
  fieldDecoratorData: {
    rules: [noEmptyInput(t('errors.form.emptyField'))],
    initialValue
  },
  label: t(`${i18nPrefix}${key}`),
  key,
  className: '',
  input: <Input onChange={Change} />
});

const GroupForm = React.forwardRef(({ updateForm, groupName }, ref) => {
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
        <h3 className="groupsSubtitle">{t('groupCreation.subtitle')}</h3>
      </div>
      <div className="GroupCreationContent">
        <div className="box">
          <h3>Group name</h3>
          <GroupForm ref={formRef} updateForm={updateForm} formValues={formValues} />
          <h3>Add contacts</h3>
          <div className="groupsCheckboxContainer">
            <div className="groupsCheckbox">
              <Checkbox onChange={Change}>Select All</Checkbox>
            </div>
            <Search
              className="searchInput"
              placeholder="Search"
              onSearch={onSearch}
              style={{ width: 200 }}
            />
          </div>
          <div className="addContactsContainer">
            <ContactDataContainer name="Anna Rodriguez" date="2020/05/15" id="00000000" />
            <ContactDataContainer name="Anna Rodriguez" date="2020/05/15" id="00000000" />
            <ContactDataContainer name="Anna Rodriguez" date="2020/05/15" id="00000000" />
            <ContactDataContainer name="Anna Rodriguez" date="2020/05/15" id="00000000" />
          </div>
          <div className="groupsButtonContainer">
            <CustomButton
              buttonProps={{ className: 'theme-primary', onClick: () => createGroup(groupName) }}
              buttonText={t('groupCreation.form.buttonText')}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

GroupForm.defaultProps = {
  groupName: ''
};

GroupForm.propTypes = {
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
