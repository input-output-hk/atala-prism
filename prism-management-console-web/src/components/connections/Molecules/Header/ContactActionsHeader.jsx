import React from 'react';
import { observer } from 'mobx-react-lite';
import { useTranslation } from 'react-i18next';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import ContactAdditionalFilters from '../filter/ContactAdditionalFilters';
import CreateContactButton from '../../Atoms/ActionButtons/CreateContactButton';

const ContactActionsHeader = observer(({ filterSortingProps }) => {
  const { t } = useTranslation();

  const { setFilterValue } = filterSortingProps;

  return (
    <div className="FilterControls">
      <div className="ContactFilters">
        <SearchBar
          setSearchText={textValue => setFilterValue('textFilter', textValue)}
          placeholder={t('contacts.filters.search')}
        />
        <CreateContactButton />
        <ContactAdditionalFilters filterSortingProps={filterSortingProps} />
      </div>
    </div>
  );
});

ContactActionsHeader.propTypes = {};

export default ContactActionsHeader;
