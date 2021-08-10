import React from 'react';
import { Input } from 'antd';
import { navigate } from 'gatsby';

const { Search } = Input;

const Searchbar = () => (
  <div className="group">
    <h3>Search Blogs</h3>
    <Search onSearch={value => navigate(`/search?query=${value}`)} style={{ width: 250 }} />
  </div>
);

export default Searchbar;
