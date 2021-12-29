import React from 'react';
import PropTypes from 'prop-types';
import { Link } from 'gatsby';
import moment from 'moment';
import Searchbar from '../searchbar/searchbar';
import { recentPostsShape } from '../../helpers/propTypes';

import './_style.scss';

const Sidebar = ({ selectedYear, recentPosts, postsPerYear, postsPerMonth }) => (
  <div className="sideBarContainer">
    <Searchbar />
    <div className="group">
      <h3>Recent Posts</h3>
      {recentPosts.map(({ frontmatter: { title, date, author }, fields: { slug } }) => [
        <Link to={slug} itemProp="url" key={slug}>
          <h4>{title}</h4>
          <p>by</p>
          <h4>{author}</h4>
        </Link>,
        <p className="date" key={`${slug}-date`}>
          {date}
        </p>
      ])}
    </div>
    <div className="group">
      <h3>Browse Posts</h3>
      <ul>
        <li className="red">
          <Link to="/blog">All Posts</Link>
        </li>
        <li className="red">
          <Link to="/authors">Authors</Link>
        </li>
      </ul>
      <ul className="dashed">
        {postsPerYear.map(({ totalCount: postsPerYearCount, fieldValue: year }) => (
          <li className="red" key={year}>
            <Link to={`/blog/${year}`}>{`${year} (${postsPerYearCount})`}</Link>
            {selectedYear === Number(year) && (
              <ul className="dashed">
                {postsPerMonth.map(({ totalCount: postsPerMonthCount, fieldValue: month }) => {
                  const monthLabel = `${moment.monthsShort()[Number(month) - 1]}, ${year}`;
                  return (
                    <li className="red" key={month}>
                      <Link to={`/blog/${year}/${month}`}>
                        {`${monthLabel} (${postsPerMonthCount})`}
                      </Link>
                    </li>
                  );
                })}
              </ul>
            )}
          </li>
        ))}
      </ul>
    </div>
  </div>
);

Sidebar.defaultProps = {
  selectedYear: null,
  postsPerMonth: []
};

Sidebar.propTypes = {
  selectedYear: PropTypes.number,
  recentPosts: recentPostsShape.isRequired,
  postsPerYear: PropTypes.arrayOf(
    PropTypes.shape({ totalCount: PropTypes.number, fieldValue: PropTypes.string })
  ).isRequired,
  postsPerMonth: PropTypes.arrayOf(
    PropTypes.shape({ totalCount: PropTypes.number, fieldValue: PropTypes.string })
  )
};

export default Sidebar;
