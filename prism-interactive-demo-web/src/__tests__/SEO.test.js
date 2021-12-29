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
    expect(metaTags[1].content).toBe(title);
    expect(metaTags[10].content).toBe(title);
    expect(metaTags[0].content).toBe(description);
    expect(metaTags[2].content).toBe(description);
    expect(metaTags[11].content).toBe(description);
  });

  it('sets custom title', () => {
    render(<SEO title={prefix} />);
    const { title: seoTitle, metaTags } = Helmet.peek();

    expect(seoTitle).toBe(customTitle);
    expect(metaTags[1].content).toBe(customTitle);
    expect(metaTags[10].content).toBe(customTitle);
  });

  it('sets custom description', () => {
    render(<SEO description={customDescription} />);
    const { metaTags } = Helmet.peek();

    expect(metaTags[0].content).toBe(customDescription);
    expect(metaTags[2].content).toBe(customDescription);
    expect(metaTags[11].content).toBe(customDescription);
  });
});
