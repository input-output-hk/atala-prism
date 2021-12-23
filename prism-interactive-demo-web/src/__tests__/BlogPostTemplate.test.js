import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import firebase from 'gatsby-plugin-firebase';
import Helmet from 'react-helmet';
import BlogPostTemplate from '../templates/blog-post/blog-post';

const location = {
  href: '/test',
  state: {
    fromResources: false
  }
};

const pageContext = {
  id: 'testId'
};

const baseFrontmatter = {
  title: 'Test blog post title',
  description: 'Test blog post',
  date: 'June 18, 2021',
  author: 'Author',
  readingTime: 10
};

const frontmatterWithImage = {
  ...baseFrontmatter,
  image: {
    publicURL: '/images/icon-help.svg'
  }
};

const post = {
  excerpt: 'This is a test post description',
  html: 'This is a test HTML'
};

const title = 'Test page';
const customTitle = `${baseFrontmatter.title} | ${title}`;

const description = 'Testing SEO component';

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  const useStaticQuery = () => ({ site: { siteMetadata: { title, description } } });
  return { Link, useStaticQuery, graphql: () => {} };
});

const mockLogEvent = jest.fn();
jest.mock('gatsby-plugin-firebase', () => {
  const analytics = () => ({
    logEvent: mockLogEvent
  });

  return { analytics };
});

const mockHeader = jest.fn();
jest.mock('../components/headerBlog/headerBlog', () => props => {
  mockHeader(props);
  return <mock-Header />;
});

const mockDisqus = jest.fn();
jest.mock('gatsby-plugin-disqus', () => ({
  Disqus: props => {
    mockDisqus(props);
    return <mock-Disqus />;
  }
}));

describe('<BlogPostTemplate />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(
      <BlogPostTemplate
        data={{ post: { ...post, frontmatter: baseFrontmatter } }}
        location={location}
        pageContext={pageContext}
      />
    );

    const { title: seoTitle, metaTags } = Helmet.peek();
    expect(seoTitle).toBe(customTitle);
    expect(metaTags[0].content).toBe(post.excerpt);

    expect(mockHeader).toHaveBeenCalledWith({ backTo: '/blog' });

    expect(screen.getByText(baseFrontmatter.title)).toBeTruthy();
    expect(screen.getByText(baseFrontmatter.description)).toBeTruthy();
    expect(screen.getByText(baseFrontmatter.date)).toBeTruthy();
    expect(screen.getByText(baseFrontmatter.author)).toBeTruthy();
    expect(screen.getByText(`${baseFrontmatter.readingTime} mins read`)).toBeTruthy();
    expect(screen.queryByAltText('thumbnail')).toBeNull();
    expect(screen.getByText(post.html)).toBeTruthy();

    expect(mockDisqus).toHaveBeenCalledWith({
      config: { url: location.href, identifier: pageContext.id, title: baseFrontmatter.title }
    });
  });

  it('renders with back button pointing to resources', () => {
    render(
      <BlogPostTemplate
        data={{ post: { ...post, frontmatter: baseFrontmatter } }}
        location={{ ...location, state: { fromResources: true } }}
        pageContext={pageContext}
      />
    );

    expect(mockHeader).toHaveBeenCalledWith({ backTo: '/resources' });
  });

  it('renders with image', () => {
    render(
      <BlogPostTemplate
        data={{ post: { ...post, frontmatter: frontmatterWithImage } }}
        location={{ ...location, state: { fromResources: true } }}
        pageContext={pageContext}
      />
    );

    expect(screen.getByAltText('thumbnail')).toBeTruthy();
  });

  it('triggers firebase event', () => {
    render(
      <BlogPostTemplate
        data={{ post: { ...post, frontmatter: baseFrontmatter } }}
        location={location}
        pageContext={pageContext}
      />
    );

    expect(firebase.analytics().logEvent).toHaveBeenCalledWith('blog_post_test_blog_post_title');
  });
});
