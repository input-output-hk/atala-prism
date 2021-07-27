import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { SearchOutlined } from '@ant-design/icons';
import { Input } from 'antd';
import { credentialTypeShape } from '../../../../helpers/propShapes';

const SearchBar = ({ filterProps }) => {
  const { t } = useTranslation();

  return (
    <div className="searchBox">
      <Input
        id="nameFilter"
        placeholder={t('credentials.filters.search')}
        prefix={<SearchOutlined />}
        onChange={({ target: { value } }) => filterProps.setName(value)}
        allowClear
        value={filterProps.name}
      />
    </div>
  );
};
SearchBar.propTypes = {
  filterProps: PropTypes.shape({
    name: PropTypes.string,
    setName: PropTypes.func,
    credentialTypes: PropTypes.arrayOf(PropTypes.shape(credentialTypeShape)),
    credentialType: PropTypes.string,
    setCredentialType: PropTypes.func,
    credentialStatus: PropTypes.number,
    setCredentialStatus: PropTypes.func,
    contactStatus: PropTypes.string,
    setContactStatus: PropTypes.func,
    date: PropTypes.string,
    setDate: PropTypes.func
  }).isRequired
};

export default SearchBar;
