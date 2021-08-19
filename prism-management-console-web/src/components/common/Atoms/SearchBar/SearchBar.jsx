import React from 'react';
import PropTypes from 'prop-types';
import { SearchOutlined } from '@ant-design/icons';
import { Input } from 'antd';

const SearchBar = ({ searchText, setSearchText, placeholder }) => (
  <div className="searchBox">
    <Input
      id="searchBarFilter"
      placeholder={placeholder}
      prefix={<SearchOutlined />}
      onChange={({ target: { value } }) => setSearchText(value)}
      allowClear
      value={searchText}
    />
  </div>
);

SearchBar.defaultProps = {
  placeholder: ''
};

SearchBar.propTypes = {
  searchText: PropTypes.string.isRequired,
  setSearchText: PropTypes.func.isRequired,
  placeholder: PropTypes.string
};

export default SearchBar;
