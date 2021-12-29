import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import Sidebar from '../components/sidebar/sidebar';

const recentPosts = [
  {
    fields: {
      slug: '/'
    },
    frontmatter: {
      date: 'June 18, 2021',
      title: 'Blog post 3',
      author: 'Author'
    }
  },
  {
    fields: {
      slug: '/'
    },
    frontmatter: {
      date: 'May 18, 2021',
      title: 'Blog post 2',
      author: 'Author'
    }
  },
  {
    fields: {
      slug: '/'
    },
    frontmatter: {
      date: 'Dec 18, 2020',
      title: 'Blog post 1',
      author: 'Author'
    }
  }
];

const postsPerYear = [{ totalCount: 2, fieldValue: '2021' }, { totalCount: 1, fieldValue: '2020' }];
const postsPerMonth = [{ totalCount: 1, fieldValue: '5' }, { totalCount: 1, fieldValue: '6' }];

const posts = [
  {
    frontmatter: { title: 'Test post', description: 'This is a testing blog post' },
    html: 'Blog post'
  }
];

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  const useStaticQuery = () => ({ posts: { nodes: posts } });
  return { useStaticQuery, graphql: () => {}, navigate: jest.fn(), Link };
});

describe('<Sidebar />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<Sidebar recentPosts={recentPosts} postsPerYear={postsPerYear} />);

    expect(screen.getByText('Recent Posts')).toBeTruthy();
    recentPosts.forEach(({ frontmatter: { title } }) => {
      expect(screen.getByText(title)).toBeTruthy();
    });
    expect(screen.getByText('Browse Posts')).toBeTruthy();
    postsPerYear.forEach(({ totalCount, fieldValue }) => {
      expect(screen.getByText(`${fieldValue} (${totalCount})`)).toBeTruthy();
    });
  });

  it('renders with selected year', () => {
    render(
      <Sidebar
        recentPosts={recentPosts}
        postsPerYear={postsPerYear}
        postsPerMonth={postsPerMonth}
        selectedYear={2021}
      />
    );

    expect(screen.getByText('May, 2021 (1)')).toBeTruthy();
    expect(screen.getByText('Jun, 2021 (1)')).toBeTruthy();
  });
});
