import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import ContactAdditionalFilters from '../Filters/ContactAdditionalFilters';
import CreateContactButton from '../../Atoms/ActionButtons/CreateContactButton';

const ContactActionsHeader = ({ filterSortingProps }) => {
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
};

ContactActionsHeader.propTypes = {
  filterSortingProps: PropTypes.shape({
    hasAdditionalFiltersApplied: PropTypes.bool,
    setFilterValue: PropTypes.func.isRequired
  }).isRequired
};

export default ContactActionsHeader;
