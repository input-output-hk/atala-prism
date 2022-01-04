import React from 'react';
import { cleanup, render, screen } from '@testing-library/react';
import GetStarted from '../components/landing/Molecules/GetStarted/GetStarted';

jest.mock('react-i18next', () => ({
  useTranslation: () => ({ t: key => key })
}));

jest.mock('gatsby', () => {
  // eslint-disable-next-line react/prop-types
  const Link = ({ children }) => <div>{children}</div>;
  return { Link };
});

describe('<GetStarted />', () => {
  afterEach(() => {
    cleanup();
  });

  it('renders', () => {
    render(<GetStarted />);

    expect(screen.getByText('landing.getStarted.title')).toBeTruthy();
    expect(screen.getByText('landing.getStarted.part1')).toBeTruthy();
    expect(screen.getByText('landing.getStarted.part2')).toBeTruthy();
    expect(screen.getByText('landing.getStarted.part3')).toBeTruthy();
    expect(screen.getByText('Contact Us')).toBeTruthy();
  });
});
