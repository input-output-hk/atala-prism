import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { PulseLoader } from 'react-spinners';
import { Checkbox, Row, Col } from 'antd';
import { withApi } from '../providers/withApi';
import { useContactsWithFilteredList } from '../../hooks/useContacts';
import { withRedirector } from '../providers/withRedirector';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { CONTACT_ID_KEY, GROUP_NAME_STATES } from '../../helpers/constants';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import ConnectionsFilter from '../connections/Molecules/filter/ConnectionsFilter';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';
import { getCheckedAndIndeterminateProps, handleSelectAll } from '../../helpers/selectionHelpers';

import './_style.scss';

const GroupCreation = ({
  api,
  createGroup,
  formRef,
  updateForm,
  formValues,
  updateMembers,
  isSaving
}) => {
  const [selectedContacts, setSelectedContacts] = useState([]);
  const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);

  const [loadingSelection, setLoadingSelection] = useState(false);

  const {
    filteredContacts,
    filterProps,
    handleContactsRequest,
    hasMore,
    isLoading,
    isSearching,
    fetchAll
  } = useContactsWithFilteredList(api.contactsManager);

  const { groupName } = formValues;
  const { t } = useTranslation();

  useEffect(() => {
    updateMembers(selectedContacts);
  }, [selectedContacts, updateMembers]);

  const handleSelectAllContacts = ev =>
    handleSelectAll({
      ev,
      setSelected: setSelectedContacts,
      entities: filteredContacts,
      hasMore,
      idKey: CONTACT_ID_KEY,
      fetchAll,
      setLoading: setLoadingSelection
    });

  const selectAllProps = {
    ...getCheckedAndIndeterminateProps(filteredContacts, selectedContacts),
    disabled: loadingSelection,
    onChange: handleSelectAllContacts
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
          <GroupName
            updateForm={updateForm}
            formValues={formValues}
            ref={formRef}
            setNameState={setNameState}
            nameState={nameState}
          />
          <h3>{t('groupCreation.addContacts')}</h3>
          <Row className="UtilsContainer mb-0">
            <Col span={3}>
              <Checkbox className="groupsCheckbox" {...selectAllProps}>
                {loadingSelection ? (
                  <PulseLoader size={3} color="#FFAEB3" />
                ) : (
                  <span>
                    {t('groupCreation.selectAll')}
                    {selectedContacts.length ? `  (${selectedContacts.length})  ` : null}
                  </span>
                )}
              </Checkbox>
            </Col>
            <Col span={19}>
              <ConnectionsFilter {...filterProps} withStatus={false} />
            </Col>
          </Row>
          <Row gutter={10} align="bottom" type="flex">
            <Col sm={24} md={20}>
              <div className="addContactsContainer">
                {(isSearching && !filteredContacts.length) || isLoading ? (
                  <SimpleLoading />
                ) : (
                  <ConnectionsTable
                    contacts={filteredContacts}
                    selectedContacts={selectedContacts}
                    setSelectedContacts={setSelectedContacts}
                    handleContactsRequest={handleContactsRequest}
                    searching={isSearching}
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
                    disabled: nameState === GROUP_NAME_STATES.failed,
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
  createGroup: PropTypes.func.isRequired,
  formRef: PropTypes.shape().isRequired,
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired,
  isSaving: PropTypes.bool,
  updateMembers: PropTypes.func.isRequired
};

export default withApi(withRedirector(GroupCreation));
