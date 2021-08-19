import React, { useState, useEffect } from 'react';
import { Input, AutoComplete } from 'antd';
import { graphql, navigate, useStaticQuery } from 'gatsby';
import keywordExtractor from 'keyword-extractor';
import _ from 'lodash';

import './style.scss';

const { Search } = Input;

const searchBlogs = value => navigate(`/search?query=${value}`);

const Searchbar = () => {
  const {
    posts: { nodes: posts }
  } = useStaticQuery(
    graphql`
      query {
        posts: allMarkdownRemark {
          nodes {
            html
            frontmatter {
              title
              description
            }
          }
        }
      }
    `
  );

  const [wordList, setWordList] = useState([]);
  const [searchValue, setSearchValue] = useState('');

  useEffect(() => {
    const postsContent = posts.reduce(
      (accum, { frontmatter: { title, description }, html }) =>
        accum.concat(title, ' ', description, ' ', html.replace(/(<([^>]+)>)/gi, ''), ' '),
      ''
    );

    const newWordList = keywordExtractor.extract(postsContent, {
      language: 'english',
      remove_digits: true,
      return_changed_case: true,
      remove_duplicates: true
    });

    const sortedWordList = _.sortBy(newWordList, ['length']);

    setWordList(sortedWordList);
  }, [posts]);

  return (
    <div className="group">
      <h3>Search Blogs</h3>
      <AutoComplete
        className="searchbar"
        value={searchValue}
        onSelect={searchBlogs}
        onChange={setSearchValue}
        dataSource={
          searchValue ? wordList.filter(word => _.startsWith(word, searchValue.toLowerCase())) : []
        }
      >
        <Search style={{ width: 250 }} onSearch={searchBlogs} />
      </AutoComplete>
    </div>
  );
};

export default Searchbar;
