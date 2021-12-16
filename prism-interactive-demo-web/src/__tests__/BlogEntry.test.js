import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import BlogEntry from '../components/blogEntry/BlogEntry';

const baseFrontmatter = {
  date: 'June 18, 2021',
  description: 'Test blog post',
  author: 'Author',
  readingTime: 10
};

const frontmatterWithTitle = {
  ...baseFrontmatter,
  title: 'Test blog post title'
};

const frontmatterWithImage = {
  ...baseFrontmatter,
  image: {
    publicURL: '/images/icon-help.svg'
  }
};

const post = {
  html: 'This is a test HTML',
  fields: {
    slug: 'test-post'
  }
};

const query = 'test';

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  return { Link };
});

describe('<BlogEntry />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<BlogEntry post={{ ...post, frontmatter: baseFrontmatter }} />);

    expect(screen.getByText(post.fields.slug)).toBeTruthy();
    expect(screen.getByText(post.html)).toBeTruthy();
    expect(screen.getByText(baseFrontmatter.description)).toBeTruthy();
    expect(screen.getByText(baseFrontmatter.date)).toBeTruthy();
    expect(screen.getByText(baseFrontmatter.author)).toBeTruthy();
    expect(screen.getByText(`${baseFrontmatter.readingTime} mins read`)).toBeTruthy();
  });

  it('renders with title', () => {
    render(<BlogEntry post={{ ...post, frontmatter: frontmatterWithTitle }} />);

    expect(screen.getByText(frontmatterWithTitle.title)).toBeTruthy();
  });

  it('renders with image', () => {
    render(<BlogEntry post={{ ...post, frontmatter: frontmatterWithImage }} />);

    expect(screen.getByAltText('thumbnail')).toBeTruthy();
  });

  it('renders with highlights', () => {
    render(<BlogEntry post={{ ...post, frontmatter: frontmatterWithTitle }} query={query} />);

    expect(screen.getAllByTestId('highlighted-title')).toBeTruthy();
    expect(screen.getAllByTestId('highlighted-description')).toBeTruthy();
    expect(screen.getAllByTestId('highlighted-body')).toBeTruthy();
  });
});
