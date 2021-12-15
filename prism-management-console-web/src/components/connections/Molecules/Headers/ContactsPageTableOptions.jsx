import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import {
  CONTACT_SORTING_KEYS,
  CONTACT_SORTING_KEYS_TRANSLATION
} from '../../../../helpers/constants';
import SortControls from '../../../common/Molecules/Sorting/SortControls';

import './_style.scss';

const ContactsPageTableOptions = ({ filterSortingProps }) => {
  const { t } = useTranslation();

  const sortingOptions = Object.values(CONTACT_SORTING_KEYS).map(column => ({
    key: column,
    label: t(`contacts.filters.${CONTACT_SORTING_KEYS_TRANSLATION[column]}`)
  }));

  return (
    <div className="ContactsPageTableOptions">
      <div className="LeftOptions">
        <SortControls options={sortingOptions} {...filterSortingProps} />
      </div>
    </div>
  );
};

ContactsPageTableOptions.propTypes = {
  filterSortingProps: PropTypes.shape({
    sortDirection: PropTypes.string.isRequired,
    toggleSortDirection: PropTypes.func.isRequired,
    sortingBy: PropTypes.string.isRequired,
    setSortingBy: PropTypes.func.isRequired
  }).isRequired
};

export default ContactsPageTableOptions;
