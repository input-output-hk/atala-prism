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
import { refPropShape } from '../../helpers/propShapes';
import { useCreateGroupStore } from '../../hooks/useGroupStore';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';

import './_style.scss';

const GroupCreation = observer(
  ({ createGroup, formRef, updateForm, formValues, updateMembers }) => {
    const [selectedContacts, setSelectedContacts] = useState([]);
    const [nameState, setNameState] = useState(GROUP_NAME_STATES.initial);

    const { groupName } = formValues;
    const { t } = useTranslation();

    useEffect(() => {
      updateMembers(selectedContacts);
    }, [selectedContacts, updateMembers]);

    const {
      init,
      contacts,
      isLoadingContacts,
      getContactsToSelect,
      isSaving,
      filterSortingProps,
      hasFiltersApplied,
      hasMore,
      isSearching,
      isFetchingMore,
      fetchMoreGroupContacts
    } = useCreateGroupStore();

    useEffect(() => {
      init();
    }, [init]);

    const { loadingSelection, checkboxProps } = useSelectAll({
      displayedEntities: contacts,
      entitiesFetcher: getContactsToSelect,
      entityKey: CONTACT_ID_KEY,
      selectedEntities: selectedContacts,
      setSelectedEntities: setSelectedContacts,
      isFetching: isLoadingContacts
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
              <ConnectionsFilter filterSortingProps={filterSortingProps} showFullFilter={false} />
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
              {isLoadingContacts ? (
                <SimpleLoading />
              ) : (
                <ConnectionsTable
                  contacts={contacts}
                  fetchMoreData={fetchMoreGroupContacts}
                  hasMore={hasMore}
                  hasFiltersApplied={hasFiltersApplied}
                  isLoading={isSearching}
                  isFetchingMore={isFetchingMore}
                  selectedContacts={selectedContacts}
                  setSelectedContacts={setSelectedContacts}
                  size="md"
                />
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }
);

GroupCreation.propTypes = {
  createGroup: PropTypes.func.isRequired,
  formRef: refPropShape.isRequired,
  updateForm: PropTypes.func.isRequired,
  formValues: PropTypes.shape().isRequired,
  updateMembers: PropTypes.func.isRequired
};

export default GroupCreation;
