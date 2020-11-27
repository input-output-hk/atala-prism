import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Input, Checkbox, Row, Col, Icon, message } from 'antd';
import { withApi } from '../providers/withApi';
import { useContactsWithFilteredList } from '../../hooks/useContacts';
import { useDebounce } from '../../hooks/useDebounce';
import { noEmptyInput } from '../../helpers/formRules';
import CustomForm from '../common/Organisms/Forms/CustomForm';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { MAX_CONTACTS } from '../../helpers/constants';
import { colors } from '../../helpers/colors';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import Logger from '../../helpers/Logger';
import { exactValueExists } from '../../helpers/filterHelpers';
import './_style.scss';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import ConnectionsFilter from '../connections/Molecules/filter/ConnectionsFilter';

const { Search } = Input;

const i18nPrefix = 'groupCreation.form.';

const getInput = (key, initialValue, t, onChange) => ({
  fieldDecoratorData: {
    rules: [noEmptyInput(t('errors.form.emptyField'))],
    initialValue
  },
  key,
  className: '',
  input: <Input onChange={onChange} placeholder={t(`${i18nPrefix}${key}`)} allowClear />
});

const GroupForm = React.forwardRef(({ updateForm, groupName }, ref) => {
  const { t } = useTranslation();

  const items = [getInput('groupName', groupName, t, ({ target: { value } }) => updateForm(value))];

  return <CustomForm items={items} ref={ref} />;
});

const NAME_STATES = {
  initial: null,
  loading: 'loading',
  possible: 'possible',
  failed: 'failed'
};

const GroupCreation = ({
  api,
  createGroup,
  formRef,
  updateForm,
  formValues,
  updateMembers,
  isIssuer,
  isSaving
}) => {
  const [nameState, setNameState] = useState(NAME_STATES.initial);
  const [selectedContacts, setSelectedContacts] = useState([]);
  const [loadingContacts, setLoadingContacts] = useState(false);
  const [searching, setSearching] = useState(true);
  const {
    contacts,
    filteredContacts,
    filterProps,
    getContacts,
    handleContactsRequest,
    hasMore
  } = useContactsWithFilteredList(api.contactsManager, setLoadingContacts, setSearching);

  const { groupName } = formValues;
  const { t } = useTranslation();

  useEffect(() => {
    setLoadingContacts(true);
    if (!contacts.length) handleContactsRequest();
  }, []);

  useEffect(() => {
    updateMembers(selectedContacts);
  }, [selectedContacts]);

  const handleUpdateForm = async value => {
    updateForm(value);
    if (value) {
      setNameState(NAME_STATES.loading);
      checkIfGroupExists(value);
    } else {
      setNameState(NAME_STATES.initial);
    }
  };

  const groupExists = async value => {
    try {
      const groups = await api.groupsManager.getGroups();
      if (exactValueExists(groups, value, 'name')) {
        setNameState(NAME_STATES.failed);
      } else {
        setNameState(NAME_STATES.possible);
      }
    } catch (error) {
      setNameState(NAME_STATES.failed);
      message.error(t('groupCreation.errors.gettingGroups'));
      Logger.error('groupCreation.errors.gettingGroups', error);
    }
  };

  const checkIfGroupExists = useDebounce(groupExists);

  const handleSelectAll = async e => {
    const { checked } = e.target;
    if (checked) {
      const allContacts = await api.contactsManager.getContacts(null, MAX_CONTACTS);
      setSelectedContacts(allContacts.map(contact => contact.contactid));
    } else {
      setSelectedContacts([]);
    }
  };

  const renderNameState = () => {
    switch (nameState) {
      case NAME_STATES.loading:
        return <Icon type="loading" />;
      case NAME_STATES.failed:
        return <Icon type="close-circle" theme="filled" style={{ color: colors.error }} />;
      case NAME_STATES.possible:
        return <Icon type="check-circle" theme="filled" style={{ color: colors.success }} />;
      default:
        return null;
    }
  };

  return (
    <div className="Wrapper">
      <div className="Header">
        <h1>{t('groupCreation.title')}</h1>
        <h3 className="groupsSubtitle">{t('groupCreation.subtitle')}</h3>
      </div>

      <div className="GroupCreationContent">
        <div className="box">
          <h3>{t('groupCreation.groupName')}</h3>
          <Row type="flex" gutter={12} className="ai-center mb-3">
            <Col sm={20} md={8}>
              <GroupForm ref={formRef} updateForm={handleUpdateForm} formValues={formValues} />
            </Col>
            <Col sm={2} md={2}>
              {renderNameState()}
            </Col>
          </Row>
          <h3>{t('groupCreation.addContacts')}</h3>
          <Row className="UtilsContainer mb-0">
            <Col span={3}>
              <Checkbox onChange={handleSelectAll}>{t('groupCreation.selectAll')}</Checkbox>
              {selectedContacts.length ? `(${selectedContacts.length})` : null}
            </Col>
            <Col span={19}>
              <ConnectionsFilter {...filterProps} withStatus={false} />
            </Col>
          </Row>
          <Row gutter={10} align="bottom" type="flex">
            <Col sm={24} md={20}>
              <div className="addContactsContainer">
                {(searching && !filteredContacts.length) || loadingContacts ? (
                  <SimpleLoading />
                ) : (
                  <ConnectionsTable
                    isIssuer={isIssuer}
                    contacts={filteredContacts}
                    selectedContacts={selectedContacts}
                    setSelectedContacts={setSelectedContacts}
                    handleContactsRequest={handleContactsRequest}
                    searching={searching}
                    hasMore={hasMore}
                    size="md"
                  />
                )}
              </div>
            </Col>
            <Col sm={24} md={4}>
              <div className="groupsButtonContainer">
                <CustomButton
                  buttonProps={{
                    className: 'theme-primary',
                    disabled: nameState === NAME_STATES.failed,
                    onClick: () => createGroup(groupName)
                  }}
                  buttonText={t('groupCreation.form.buttonText')}
                  loading={isSaving}
                />
              </div>
            </Col>
          </Row>
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

GroupCreation.defaultProps = {
  isSaving: false
};

GroupCreation.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired,
      getContacts: PropTypes.func.isRequired
    }).isRequired,
    groupsManager: PropTypes.shape({
      getGroups: PropTypes.func.isRequired
    }).isRequired
  }).isRequired,
  isIssuer: PropTypes.func.isRequired,
  createGroup: PropTypes.func.isRequired,
  formRef: PropTypes.shape().isRequired,
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired,
  isSaving: PropTypes.bool,
  updateMembers: PropTypes.func.isRequired
};

export default withApi(withRedirector(GroupCreation));
