import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import SearchBar from '../../../common/Atoms/SearchBar/SearchBar';
import './_style.scss';

const SimpleGroupsFilter = observer(({ filterSortingProps }) => {
  const { t } = useTranslation();

  const { setFilterValue } = filterSortingProps;

  return (
    <div className="FilterControls">
      <div className="GroupsFilter">
        <SearchBar
          setSearchText={textValue => setFilterValue('textFilter', textValue)}
          placeholder={t('groups.filters.search')}
        />
      </div>
    </div>
  );
});

SimpleGroupsFilter.propTypes = {
  filterSortingProps: PropTypes.shape({
    setFilterValue: PropTypes.func.isRequired
  })
};

export default SimpleGroupsFilter;
