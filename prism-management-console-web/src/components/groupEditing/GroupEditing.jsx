import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { PlusOutlined } from '@ant-design/icons';
import { Checkbox, Col, Row, Button } from 'antd';
import ConnectionsFilter from '../connections/Molecules/filter/ConnectionsFilter';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';
import { withApi } from '../providers/withApi';
import { GROUP_NAME_STATES, MAX_CONTACTS } from '../../helpers/constants';
import GroupContacts from './GroupContacts';
import AddContactsModal from './AddContactsModal/AddContactsModal';
import ConfirmDeletionModal from './ConfirmDeletionModal/ConfirmDeletionModal';
import { contactShape, groupShape } from '../../helpers/propShapes';

import './_style.scss';

const GroupEditing = ({
  api,
  group,
  filterProps,
  handleContactsRequest,
  contacts,
  onRemoveContacts,
  onAddContacts,
  hasMore,
  isSaving,
  loading,
  loadingContacts
}) => {
  const { t } = useTranslation();
  const formRef = React.createRef();
  const [groupName, setGroupName] = useState('');
  const [contactsToRemove, setContactsToRemove] = useState([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [modalDeletionVisible, setModalDeletionVisible] = useState(false);
  const [selectedGroupContacts, setSelectedGroupContacts] = useState([]);
  const [editing, setEditing] = useState(false);
  const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);
  const formValues = { groupName };

  useEffect(() => {
    if (group.name) {
      setGroupName(group.name);
    }
  }, [group.name]);

  const contactidMapper = contact => contact.contactid;

  const handleSelectAll = async e => {
    const { checked } = e.target;
    if (checked) {
      const allContacts = await api.contactsManager.getContacts(null, MAX_CONTACTS, groupName);
      setSelectedGroupContacts(allContacts.map(contactidMapper));
    } else {
      setSelectedGroupContacts([]);
    }
  };

  const handleCancelClick = () => {
    setGroupName(group.name);
    setEditing(false);
  };

  const handleRemoveClick = async () =>
    onRemoveContacts(contactsToRemove).finally(() => {
      setModalDeletionVisible(false);
      setSelectedGroupContacts([]);
    });

  const handleAddContactConfirm = async aContactsList =>
    onAddContacts(aContactsList).finally(() => setModalVisible(false));

  const handleRemoveContactRequest = aContactsList => {
    setContactsToRemove(aContactsList);
    setModalDeletionVisible(true);
  };

  const editButtonProps = {
    type: 'link',
    // TODO: remove disabled when name edition is enabled from backend
    disabled: true,
    style: { color: '#FF2D3B' },
    onClick: () => setEditing(!editing)
  };

  const firstColProps = {
    lg: 20,
    md: 19,
    sm: 18
  };

  const secondColProps = {
    lg: 4,
    md: 5,
    sm: 6
  };

  const renderNameSection = () => {
    if (loading) return <SimpleLoading size="sm" />;
    if (editing)
      return (
        <GroupName
          ref={formRef}
          updateForm={setGroupName}
          groupName={groupName}
          formValues={formValues}
          setNameState={setNameState}
          nameState={nameState}
          className="GroupNameForm"
        />
      );
    return groupName;
  };

  return (
    <div className="Wrapper">
      <Row className="Header">
        <Col {...firstColProps}>
          <h1>{t('groupEditing.title')}</h1>
          <h3 className="groupsSubtitle">{t('groupEditing.subtitle')}</h3>
        </Col>
        <Col {...secondColProps}>
          <CustomButton
            className="custom"
            buttonProps={{
              className: 'theme-secondary',
              onClick: () => setModalVisible(true)
            }}
            buttonText={t('groupEditing.buttons.addContacts')}
            icon={<PlusOutlined />}
          />
        </Col>
      </Row>

      <AddContactsModal
        visible={modalVisible}
        groupName={groupName}
        onCancel={() => setModalVisible(false)}
        onConfirm={handleAddContactConfirm}
      />

      <ConfirmDeletionModal
        visible={modalDeletionVisible}
        length={contactsToRemove.length}
        onCancel={() => setModalDeletionVisible(false)}
        onConfirm={handleRemoveClick}
      />

      <div className="GroupEditingContent">
        <Row className="GroupNameContainer">
          <Col {...firstColProps} className="GroupName">
            {renderNameSection()}
            {editing ? (
              <Button {...editButtonProps} onClick={handleCancelClick}>
                {t('groupEditing.buttons.cancel')}
              </Button>
            ) : (
              <Button {...editButtonProps}>{t('groupEditing.buttons.edit')}</Button>
            )}
          </Col>
          <Col {...secondColProps}>
            <CustomButton
              className="custom"
              buttonProps={{
                className: 'theme-outline',
                disabled: !selectedGroupContacts.length,
                onClick: () => handleRemoveContactRequest(selectedGroupContacts)
              }}
              buttonText={t('groupEditing.buttons.remove')}
              loading={isSaving}
            />
          </Col>
        </Row>
        <h3>{t('groupEditing.contacts')}</h3>
        <Row type="flex" align="middle" className="mb-0">
          <Col span={3}>
            {loading ? (
              <SimpleLoading size="xs" />
            ) : (
              <>
                <Checkbox onChange={handleSelectAll}>{t('groupEditing.selectAll')}</Checkbox>
                {selectedGroupContacts.length ? `(${selectedGroupContacts.length})` : null}
              </>
            )}
          </Col>
          <Col span={19}>
            <ConnectionsFilter {...filterProps} withStatus={false} />
          </Col>
        </Row>
        <Row gutter={10} align="bottom" type="flex" className="ContactsContainer">
          <GroupContacts
            contacts={contacts}
            groupName={groupName}
            selectedContacts={selectedGroupContacts}
            setSelectedContacts={setSelectedGroupContacts}
            onDeleteContact={handleRemoveContactRequest}
            handleContactsRequest={handleContactsRequest}
            loading={loadingContacts || isSaving}
            hasMore={hasMore}
          />
        </Row>
      </div>
    </div>
  );
};

GroupEditing.defaultProps = {
  group: {
    name: ''
  },
  filterProps: {},
  loading: true,
  loadingContacts: false,
  isSaving: false,
  contacts: []
};

GroupEditing.propTypes = {
  api: PropTypes.shape({
    contactsManager: PropTypes.shape({
      getContacts: PropTypes.func
    })
  }).isRequired,
  handleContactsRequest: PropTypes.func.isRequired,
  onRemoveContacts: PropTypes.func.isRequired,
  onAddContacts: PropTypes.func.isRequired,
  hasMore: PropTypes.bool.isRequired,
  group: PropTypes.shape(groupShape),
  filterProps: PropTypes.shape(),
  loading: PropTypes.bool,
  loadingContacts: PropTypes.bool,
  isSaving: PropTypes.bool,
  contacts: PropTypes.arrayOf(PropTypes.shape(contactShape))
};

export default withApi(GroupEditing);
