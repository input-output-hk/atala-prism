import React from 'react';
import { cleanup, render } from '@testing-library/react';
import Helmet from 'react-helmet';
import SEO from '../components/seo/seo';

const title = 'Test page';
const prefix = 'Fake';
const customTitle = `${prefix} | ${title}`;

const description = 'Testing SEO component';
const customDescription = 'Some description';

jest.mock('gatsby', () => {
  const useStaticQuery = () => ({ site: { siteMetadata: { title, description } } });
  return { useStaticQuery, graphql: () => {} };
});

describe('<SEO />', () => {
  afterEach(() => {
    cleanup();
  });

  it('sets default title', () => {
    render(<SEO />);
    const { title: seoTitle, metaTags } = Helmet.peek();

    expect(seoTitle).toBe(title);
    expect(metaTags.length).toBe(15);
  });

  it('sets custom title', () => {
    render(<SEO title={prefix} />);
    const { title: seoTitle } = Helmet.peek();

    expect(seoTitle).toBe(customTitle);
  });

  it('sets custom description', () => {
    render(<SEO description={customDescription} />);
    const { metaTags } = Helmet.peek();

    expect(metaTags[0].content).toBe(customDescription);
  });
});
