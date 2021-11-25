import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import CustomButton from '../common/Atoms/CustomButton/CustomButton';
import { CONTACT_ID_KEY, GROUP_NAME_STATES } from '../../helpers/constants';
import ConnectionsTable from '../connections/Organisms/table/ConnectionsTable';
import ConnectionsFilter from '../connections/Molecules/filter/ConnectionsFilter';
import GroupName from '../common/Molecules/GroupForm/GroupFormContainer';
import SelectAllButton from '../newCredential/Molecules/RecipientsTable/SelectAllButton';
import { useSelectAll } from '../../hooks/useSelectAll';
import { useContactStore } from '../../hooks/useContactStore';
import { refPropShape } from '../../helpers/propShapes';

import './_style.scss';

const GroupCreation = observer(
  ({ createGroup, formRef, updateForm, formValues, updateMembers, isSaving }) => {
    const [selectedContacts, setSelectedContacts] = useState([]);
    const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);

    const { groupName } = formValues;
    const { t } = useTranslation();

    useEffect(() => {
      updateMembers(selectedContacts);
    }, [selectedContacts, updateMembers]);

    const {
      contacts,
      contactUiState,
      initContactStore,
      getContactsToSelect,
      isLoadingFirstPage,
      fetchMoreData,
      isFetching,
      hasMore
    } = useContactStore();
    const { hasFiltersApplied, isSearching, isSorting } = contactUiState;

    useEffect(() => {
      initContactStore();
    }, [initContactStore]);

    const { loadingSelection, checkboxProps } = useSelectAll({
      displayedEntities: contacts,
      entitiesFetcher: getContactsToSelect,
      entityKey: CONTACT_ID_KEY,
      selectedEntities: selectedContacts,
      setSelectedEntities: setSelectedContacts,
      isFetching
    });

    return (
      <div className="WrapperGroupCreation">
        <div className="Header">
          <div className="Title">
            <h1>{t('groupCreation.title')}</h1>
            <h3 className="groupsSubtitle">{t('groupCreation.subtitle')}</h3>
          </div>

          <div className="flex">
            <div className="SearchBar">
              <ConnectionsFilter filterSortingProps={contactUiState} showFullFilter={false} />
            </div>

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
          </div>
        </div>
        <div className="GroupCreationContent InfiniteScrollTableContainer">
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
            <div className="UtilsContainer">
              <SelectAllButton
                loadingSelection={loadingSelection}
                selectedEntities={selectedContacts}
                checkboxProps={checkboxProps}
              />
            </div>
            <div className="ConnectionsTable InfiniteScrollTableContainer">
              <ConnectionsTable
                contacts={contacts}
                fetchMoreData={fetchMoreData}
                hasMore={hasMore}
                hasFiltersApplied={hasFiltersApplied}
                isLoading={isLoadingFirstPage || isLoadingFirstPage || isSorting}
                isFetchingMore={isFetching || isSearching}
                selectedContacts={selectedContacts}
                setSelectedContacts={setSelectedContacts}
                size="md"
              />
            </div>
          </div>
        </div>
      </div>
    );
  }
);

GroupCreation.defaultProps = {
  isSaving: false
};

GroupCreation.propTypes = {
  createGroup: PropTypes.func.isRequired,
  formRef: refPropShape.isRequired,
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired,
  isSaving: PropTypes.bool,
  updateMembers: PropTypes.func.isRequired
};

export default GroupCreation;
