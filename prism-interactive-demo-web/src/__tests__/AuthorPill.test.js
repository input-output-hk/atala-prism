import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import AuthorPill from '../components/author/authorpill';

const baseAuthor = {
  name: 'Name',
  title: 'Title',
  position: 'Position'
};

const authorWithCompany = {
  ...baseAuthor,
  company: 'Company'
};

const authorWithSocials = {
  ...baseAuthor,
  email: 'Email',
  youtube: 'Youtube',
  twitter: 'Twitter',
  linkedIn: 'LinkedIn',
  github: 'Github'
};

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  return { Link };
});

describe('<AuthorPill />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders base author', () => {
    render(<AuthorPill {...baseAuthor} />);

    expect(screen.getByAltText('Author')).toBeTruthy();
    expect(screen.getByText(baseAuthor.name)).toBeTruthy();
    expect(screen.getByText(baseAuthor.title)).toBeTruthy();
    expect(screen.getByText(baseAuthor.position)).toBeTruthy();
  });

  it('renders author with company', () => {
    render(<AuthorPill {...authorWithCompany} />);

    expect(screen.getByText(authorWithCompany.company)).toBeTruthy();
  });

  it('renders author with socials', () => {
    render(<AuthorPill {...authorWithSocials} />);

    expect(screen.getByAltText(authorWithSocials.email)).toBeTruthy();
    expect(screen.getByAltText(authorWithSocials.youtube)).toBeTruthy();
    expect(screen.getByAltText(authorWithSocials.twitter)).toBeTruthy();
    expect(screen.getByAltText(authorWithSocials.linkedIn)).toBeTruthy();
    expect(screen.getByAltText(authorWithSocials.github)).toBeTruthy();
  });
});
