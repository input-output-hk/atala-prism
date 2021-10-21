import React from 'react';
import { Link } from 'gatsby';
import moment from 'moment';
import Searchbar from '../searchbar/searchbar';

import './_style.scss';

const Sidebar = ({ selectedYear, recentPosts, postsPerYear, postsPerMonth }) => (
  <div className="sideBarContainer">
    <Searchbar />
    <div className="group">
      <h3>Recent Posts</h3>
      {recentPosts.map(({ frontmatter: { title, date, author }, fields: { slug } }) => [
        <Link to={slug} itemProp="url">
          <h4>{title}</h4>
          <p>by</p>
          <h4>{author}</h4>
        </Link>,
        <p className="date">{date}</p>
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
          <li className="red">
            <Link to={`/blog/${year}`}>{`${year} (${postsPerYearCount})`}</Link>
            {selectedYear === Number(year) && (
              <ul className="dashed">
                {postsPerMonth.map(({ totalCount: postsPerMonthCount, fieldValue: month }) => {
                  const monthLabel = moment(`${year}-${month}`).format('MMM, YYYY');
                  return (
                    <li className="red">
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

export default Sidebar;
