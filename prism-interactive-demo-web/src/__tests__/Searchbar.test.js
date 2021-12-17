import React from 'react';
import { navigate } from 'gatsby';
import { cleanup, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Searchbar from '../components/searchbar/searchbar';

const posts = [
  {
    frontmatter: { title: 'Test post', description: 'This is a testing blog post' },
    html: 'Blog post'
  }
];

jest.mock('gatsby', () => {
  const useStaticQuery = () => ({ posts: { nodes: posts } });
  return { useStaticQuery, graphql: () => {}, navigate: jest.fn() };
});

describe('<Searchbar />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<Searchbar />);

    expect(screen.getByText('Search Blogs')).toBeTruthy();
  });

  it('filters blog posts', () => {
    render(<Searchbar />);

    const input = screen.getByTestId('searchbox');
    userEvent.type(input, 'mocked search value{enter}');

    expect(navigate.mock.calls.length).toBe(1);
  });
});
